package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.meters.EM1Meters;
import it.usna.shellyscan.model.device.g2.modules.EM1Manager;
import it.usna.shellyscan.model.device.g2.modules.LoRaAddOn;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class ShellyEMG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	private static final Logger LOG = LoggerFactory.getLogger(ShellyEMG3.class);
	public static final String ID = "EMG3";
	public static final String MODEL = "S3EM-002CXCEU";
	private Relay relay = new Relay(this, 0);
	private Relay[] relays = new Relay[] {relay};
	private float internalTmp;
	private EM1Meters meters0, meters1;
	private Meters meters[];
	private SensorAddOn sensorAddOn;
	private boolean loraAddOn;

	public ShellyEMG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		meters0 = new EM1Meters(new EM1Manager(this, 0));
		meters1 = new EM1Meters(new EM1Manager(this, 1));
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
		final String addOn = config.get("sys").get("device").path("addon_type").asString();
		if(SensorAddOn.ADDON_TYPE.equals(addOn)) {
			sensorAddOn = new SensorAddOn(this);
			meters = (sensorAddOn.getTypes().length > 0) ? new Meters[] {meters0, meters1, sensorAddOn} : new Meters[] {meters0, meters1};
		} else {
			sensorAddOn = null;
			meters = new Meters[] {meters0, meters1};
		}
		loraAddOn = LoRaAddOn.ADDON_TYPE.equals(addOn);
		return config;
	}

	@Override
	public String getTypeName() {
		return "Shelly EM G3";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getModelID() {
		return MODEL;
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
		relay.fillSettings(configuration.get("switch:0"));
		
		meters0.fillSettings(configuration.get("em1:0"));
		meters1.fillSettings(configuration.get("em1:1"));
		
		if(sensorAddOn != null) {
			sensorAddOn.fillSettings(configuration);
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus);
		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
		
		meters0.fillStatus(status.get("em1:0"));
		meters1.fillStatus(status.get("em1:1"));
		
		if(sensorAddOn != null) {
			sensorAddOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		final String[] cmd = EM1Manager.getInfoRequests(super.getInfoRequests(), 0, 1);
		if(sensorAddOn != null) {
			return SensorAddOn.getInfoRequests(cmd);
		} else if(loraAddOn) {
			return LoRaAddOn.getInfoRequests(cmd);
		} else {
			return cmd;
		}
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
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
		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
		errors.add(relay.restore(config));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

		ObjectNode conf = createIndexedRestoreNode(config, "em1", 0);
		((ObjectNode)conf.get("config")).remove("ct_type");
		errors.add(postCommand("EM1.SetConfig", conf));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		conf = createIndexedRestoreNode(config, "em1", 1);
		((ObjectNode)conf.get("config")).remove("ct_type");
		errors.add(postCommand("EM1.SetConfig", conf));
		
		SensorAddOn.restore(this, sensorAddOn, backupJsons, errors);
		LoRaAddOn.restore(this, loraAddOn, config, errors);
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}