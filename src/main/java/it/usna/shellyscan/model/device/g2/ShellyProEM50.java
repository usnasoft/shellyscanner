package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.meters.EM1Meters;
import it.usna.shellyscan.model.device.g2.modules.EM1Manager;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class ShellyProEM50 extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "ProEM";
	public static final String MODEL = "SPEM-002CEBEU50";
	private Relay relay = new Relay(this, 0);
	private Relay[] relays = new Relay[] {relay};
	private float internalTmp;
	private EM1Meters meters0, meters1;
	private Meters meters[];

	public ShellyProEM50(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters0 = new EM1Meters(new EM1Manager(this, 0));
		meters1 = new EM1Meters(new EM1Manager(this, 1));
		meters = new Meters[] {meters0, meters1};
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro EM-50";
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
		relay.fillSettings(configuration.get("switch:0"));
		
		meters0.fillSettings(configuration.get("em1:0"));
		meters1.fillSettings(configuration.get("em1:1"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus);
		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
		
		meters0.fillStatus(status.get("em1:0"));
		meters1.fillStatus(status.get("em1:1"));
	}
	
	@Override
	public String[] getInfoRequests() {
		return EM1Manager.getInfoRequests(super.getInfoRequests(), 0, 1);
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
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}