package it.usna.shellyscan.model.device.g3;

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
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Shelly 2L G3 model 
 * @author usna
 */
public class Shelly2LG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "S2LG3";
	public static final String MODEL = "S3SW-0A2X4EUL";
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay[] relaysArray = new Relay[] {relay0, relay1};
	private float internalTmp;

	public Shelly2LG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "Shelly 2L G3";
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
	public DeviceModule[] getModules() {
		return relaysArray /*modeRelay ? relaysArray : rollersArray*/;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		relay1.fillSettings(configuration.get("switch:1"), configuration.get("input:1"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0, status.get("input:0"));
		relay1.fillStatus(status.get("switch:1"), status.get("input:1"));

		internalTmp = switchStatus0.path("temperature").path("tC").floatValue();
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
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
	}
}