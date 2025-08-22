package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;

/**
 * Shelly Pro 1 model
 * @author usna
 */
public class ShellyPro1 extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "Pro1";
	public static final String MODEL = "SPSW-201XE15UL";
	private Relay relay = new Relay(this, 0);
	private String inputKey;
	private float internalTmp;
	private Relay[] relays = new Relay[] {relay};

	public ShellyPro1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
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
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		
		JsonNode switchConf0 = configuration.get("switch:0");
		inputKey = switchConf0.path("input_id").intValue() == 0 ? "input:0" : "input:1";;
		relay.fillSettings(switchConf0, configuration.get(inputKey));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get(inputKey));
		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}