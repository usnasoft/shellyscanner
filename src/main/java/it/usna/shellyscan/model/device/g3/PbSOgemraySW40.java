package it.usna.shellyscan.model.device.g3;

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
 * Ogemray SW40 25A rele model
 * @author usna
 */
public class PbSOgemraySW40 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	public final static String ID = "Ogemray25";
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private Relay[] relays = new Relay[] {relay};
	private MetersWVI meters = new MetersWVI();
	private Meters[] metersArray = new Meters[] {meters};

	public PbSOgemraySW40(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Ogemray SW40";
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
		
		internalTmp = switchStatus.path("temperature").path("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}