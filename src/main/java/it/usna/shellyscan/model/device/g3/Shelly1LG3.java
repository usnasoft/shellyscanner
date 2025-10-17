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
 * Shelly 1L G3 model 
 * @author usna
 */
public class Shelly1LG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "S1LG3";
	public static final String MODEL = "S3SW-0A1X1EUL";
	private Relay relay0 = new Relay(this, 0);
	private Relay[] relaysArray = new Relay[] {relay0};
	private String inputKey;
	private float internalTmp;

	public Shelly1LG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "Shelly 1L G3";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public DeviceModule[] getModules() {
		return relaysArray;
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
		relay0.fillSettings(switchConf0, configuration.get(inputKey));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0, status.get(inputKey));

		internalTmp = switchStatus0.path("temperature").path("tC").floatValue();
	}

	// todo second input?
	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay0.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0;
	}
}