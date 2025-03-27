package it.usna.shellyscan.model.device.g4;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.meters.MetersWVI;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;

/**
 * Shelly mini 1PM G4 model
 * @author usna
 */
public class ShellyMini1PMG4 extends AbstractG4Device implements ModulesHolder, InternalTmpHolder {
	public final static String ID = "Mini1PMG4";
	private float internalTmp;
	private MetersWVI meters = new MetersWVI();
	private Meters[] metersArray = new Meters[] {meters};
	private Relay relay = new Relay(this, 0);
	private Relay[] relays = new Relay[] {relay};

	public ShellyMini1PMG4(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "Shelly Mini 1PM G4";
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
		return metersArray;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get("input:0"));
		meters.fill(switchStatus);

		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}