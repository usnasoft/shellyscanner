package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LoRaAddOn;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOnPro;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;

/**
 * Shelly Pro 2 model
 * @author usna
 */
public class ShellyPro2 extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "Pro2";
	public static final String ID_ADDON = "Pro2ProAddon";
	public static final String MODEL_1 = "SPSW-202XE15UL";
	public static final String MODEL_2 = "SPSW-202XE16EU";
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay[] relays; // = new Relay[] {relay0, relay1};
	private float internalTmp;
	private Meters[] meters;
	private SensorAddOnPro sensorAddOn;
	private boolean hasLoraAddOn;

	public ShellyPro2(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asString("");
		this.mac = devInfo.get("mac").asString("");
		
		final JsonNode config = configure();
			
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	private JsonNode configure() throws IOException {
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		final String addOnType = config.get("sys").get("device").path("addon_type").asString(null);
		if(SensorAddOnPro.ADDON_TYPE.equals(addOnType)) {
			sensorAddOn = new SensorAddOnPro(this);
			meters = sensorAddOn.addMetersArray();
			if(sensorAddOn.getDigitalOut() == null) {
				relays = new Relay[] {relay0, relay1};
			} else {
				relays = new Relay[] {relay0, relay1, sensorAddOn.getDigitalOut()};
			}
		} else {
			sensorAddOn = null;
			meters = null;
			relays = new Relay[] {relay0, relay1};
			hasLoraAddOn = LoRaAddOn.ADDON_TYPE.equals(addOnType);
		}
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Pro 2";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getModulesCount() {
		return 2;
	}

	@Override
	public Relay[] getModules() {
		return relays;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		
		relay0.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		relay1.fillSettings(configuration.get("switch:1"), configuration.get("input:1"));
		if(sensorAddOn != null) {
			sensorAddOn.fillSettings(configuration);
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);

		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0, status.get("input:0"));

		JsonNode switchStatus1 = status.get("switch:1");
		relay1.fillStatus(switchStatus1, status.get("input:1"));

		internalTmp = switchStatus0.path("temperature").path("tC").floatValue();
		if(sensorAddOn != null) {
			sensorAddOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {	
		final String[] cmd = super.getInfoRequests();
		if(sensorAddOn != null) {
			return SensorAddOnPro.getInfoRequests(cmd);
		} else if(hasLoraAddOn) {
			return LoRaAddOn.getInfoRequests(cmd);
		} else {
			return cmd;
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this,configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay0.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay1.restore(configuration));
		
		SensorAddOnPro.restore(this, sensorAddOn, backupJsons, errors);
		LoRaAddOn.restore(this, hasLoraAddOn, configuration, errors);
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
	}
}

/*
{
  "name" : null,
  "id" : "shellypro2-xxx",
  "mac" : "xxx",
  "model" : "SPSW-202XE16EU",
  "gen" : 2,
  "fw_id" : "20221206-143405/0.12.0-gafc2404",
  "ver" : "0.12.0",
  "app" : "Pro2",
  "auth_en" : false,
  "auth_domain" : null
}
*/