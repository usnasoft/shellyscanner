package it.usna.shellyscan.model.device.g4;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.meters.MetersWVIpf;
import it.usna.shellyscan.model.device.g2.modules.Relay;

/**
 * Shelly Power Strip G4 model
 * @author usna
 */
public class ShellyPowerStrip4G extends AbstractG4Device implements ModulesHolder/*, InternalTmpHolder*/ {
	public final static String ID = "PowerStrip";
	public final static String MODEL = "S4PL-00416EU";
//	private float internalTmp;
	private MetersWVIpf meters0 = new MetersWVIpf();
	private MetersWVIpf meters1 = new MetersWVIpf();
	private MetersWVIpf meters2 = new MetersWVIpf();
	private MetersWVIpf meters3 = new MetersWVIpf();
	private Meters[] metersArray = new Meters[] {meters0, meters1, meters2, meters3};
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay relay2 = new Relay(this, 2);
	private Relay relay3 = new Relay(this, 3);
	private Relay[] relays = new Relay[] {relay0, relay1, relay2, relay3};

	public ShellyPowerStrip4G(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "Shelly Power Strip G4";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public Relay[] getModules() {
		return relays;
	}

//	@Override
//	public float getInternalTmp() {
//		return internalTmp;
//	}

	@Override
	public Meters[] getMeters() {
		return metersArray;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"));
		relay1.fillSettings(configuration.get("switch:1"));
		relay2.fillSettings(configuration.get("switch:2"));
		relay3.fillSettings(configuration.get("switch:3"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0);
		meters0.fill(switchStatus0);
		
		JsonNode switchStatus1 = status.get("switch:1");
		relay1.fillStatus(switchStatus1);
		meters1.fill(switchStatus1);
		
		JsonNode switchStatus2 = status.get("switch:2");
		relay2.fillStatus(switchStatus2);
		meters2.fill(switchStatus2);
		
		JsonNode switchStatus3 = status.get("switch:3");
		relay3.fillStatus(switchStatus3);
		meters3.fill(switchStatus3);

//		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException, JsonProcessingException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(relay0.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay1.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay2.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay3.restore(configuration));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		ObjectNode ui = (ObjectNode)configuration.get("powerstrip_ui").deepCopy();
		ObjectNode out = JsonNodeFactory.instance.objectNode().set("config", ui);
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("POWERSTRIP_UI.SetConfig", out));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1 + "; Relay2: " + relay2 + "; Relay3: " + relay3;
	}
}