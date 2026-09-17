package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LoRaAddOn;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOnPro;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;

/**
 * Shelly Pro 1 model
 * @author usna
 */
public class ShellyPro1 extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "Pro1";
	public static final String ID_ADDON = "Pro1ProAddon";
	public static final String MODEL_1 = "SPSW-201XE15UL";
	public static final String MODEL_2 = "SPSW-201XE16EU";
	private Relay relay = new Relay(this, 0);
	private String inputKey;
	private float internalTmp;
	private Meters[] meters;
	private Relay[] relays;
	private SensorAddOnPro sensorAddOn;
	private boolean hasLoraAddOn;

	public ShellyPro1(InetAddress address, int port, String hostname) {
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
				relays = new Relay[] {relay};
			} else {
				relays = new Relay[] {relay, sensorAddOn.getDigitalOut()};
			}
		} else {
			sensorAddOn = null;
			meters = null;
			relays = new Relay[] {relay};
			hasLoraAddOn = LoRaAddOn.ADDON_TYPE.equals(addOnType);
		}
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Pro 1";
	}
	
	@Override
	public String getTypeID() {
		return ID;
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
		
		JsonNode switchConf0 = configuration.get("switch:0");
		inputKey = switchConf0.path("input_id").intValue(0) == 0 ? "input:0" : "input:1";;
		relay.fillSettings(switchConf0, configuration.get(inputKey));
		if(sensorAddOn != null) {
			sensorAddOn.fillSettings(configuration);
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get(inputKey));
		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
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
	protected void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> resp) {
		SensorAddOnPro.restoreCheck(this, sensorAddOn, backupJsons, resp);
		LoRaAddOn.restoreCheck(this, hasLoraAddOn, backupJsons, resp);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
		
		SensorAddOnPro.restore(this, sensorAddOn, backupJsons, errors);
		LoRaAddOn.restore(this, hasLoraAddOn, configuration, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}