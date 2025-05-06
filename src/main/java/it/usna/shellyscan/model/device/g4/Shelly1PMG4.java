package it.usna.shellyscan.model.device.g4;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.meters.MetersWVI;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g3.modules.LoRaAddOn;

/**
 * Shelly 1PM G4 model
 * @author usna
 */
public class Shelly1PMG4 extends AbstractG4Device implements ModulesHolder, InternalTmpHolder {
	private final static Logger LOG = LoggerFactory.getLogger(Shelly1PMG4.class);
	public final static String ID = "S1PMG4";
	public final static String MODEL = "S4SW-001P16EU";
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private Relay[] relays = new Relay[] {relay};
	private MetersWVI baseMeasures = new MetersWVI();
	private Meters[] meters;
	private SensorAddOn sensorAddOn;
	private boolean loraAddOn;

	public Shelly1PMG4(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		
		final JsonNode config = configure();
		
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	private JsonNode configure() throws IOException {
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		final String addOn = config.get("sys").get("device").path("addon_type").asText();
		if(SensorAddOn.ADDON_TYPE.equals(addOn)) {
			sensorAddOn = new SensorAddOn(this);
			meters = (sensorAddOn.getTypes().length > 0) ? new Meters[] {baseMeasures, sensorAddOn} : new Meters[] {baseMeasures};
		} else {
			sensorAddOn = null;
			meters = new Meters[] {baseMeasures};
		}
		loraAddOn = LoRaAddOn.ADDON_TYPE.equals(addOn);
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly 1PM G4";
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
		relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		if(sensorAddOn != null) {
			sensorAddOn.fillSettings(configuration);
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get("input:0"));
		internalTmp = switchStatus.path("temperature").path("tC").floatValue();
		baseMeasures.fill(switchStatus);
		if(sensorAddOn != null) {
			sensorAddOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		final String[] cmd = super.getInfoRequests();
		if(sensorAddOn != null) {
			return SensorAddOn.getInfoRequests(cmd);
		} else if(loraAddOn) {
			return LoRaAddOn.getInfoRequests(cmd);
		} else {
			return cmd;
		}
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		try {
			configure(); // maybe useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		} catch (IOException e) {
			LOG.error("restoreCheck", e);
		}
		SensorAddOn.restoreCheck(this, sensorAddOn, backupJsons, res);
		LoRaAddOn.restoreCheck(this, loraAddOn, backupJsons, res);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));

		SensorAddOn.restore(this, sensorAddOn, backupJsons, errors);
		LoRaAddOn.restore(this, loraAddOn, configuration, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}