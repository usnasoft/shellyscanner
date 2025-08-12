package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Shelly X MOD1 model
 * @author usna
 */
public class ShellyXMOD1 extends AbstractG3Device implements ModulesHolder {
	public static final String ID = "XMOD1";
	private int numInputs;
	private int numOutputs;
	private int numModules;
	private DeviceModule[] inputOutput;
	private Meters[] meters;
	private SensorAddOn addOn;

	public ShellyXMOD1(InetAddress address, int port, String hostname) {
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
		final JsonNode xmod = getJSON("/rpc/XMOD.GetInfo").get("jwt").get("xmod1");
		numInputs = xmod.get("ni").intValue();
		numOutputs = xmod.get("no").intValue();
		numModules = Math.max(numOutputs, numInputs);

		inputOutput = new DeviceModule[numModules];
		int i = 0;
		for(; i < numOutputs; i++) {
			inputOutput[i] = new Relay(this, i);
		}
		for(; i < numInputs; i++) {
			inputOutput[i] = new Input(this, i);
		}

		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
			addOn = new SensorAddOn(this);
			meters = (addOn.getTypes().length > 0) ? new Meters[] {addOn} : null;
		} else {
			addOn = null;
			meters = null;
		}
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly X MOD1";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public DeviceModule[] getModules() {
		return inputOutput;
	}

	@Override
	public int getModulesCount() {
		return numModules;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		int i = 0;
		for(; i < numOutputs; i++) {
			if(i < numInputs) {
				((Relay)inputOutput[i]).fillSettings(configuration.get("switch:" + i), configuration.get("input:" + i));
			} else {
				((Relay)inputOutput[i]).fillSettings(configuration.get("switch:" + i));
			}
		}
		for(; i < numInputs; i++) {
			((Input)inputOutput[i]).fillSettings(configuration.get("input:" + i));
		}
		if(addOn != null) {
			addOn.fillSettings(configuration);
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		int i = 0;
		for(; i < numOutputs; i++) {
			if(i < numInputs) {
				((Relay)inputOutput[i]).fillStatus(status.get("switch:" + i), status.get("input:" + i));
			} else {
				((Relay)inputOutput[i]).fillStatus(status.get("switch:" + i));
			}
		}
		for(; i < numInputs; i++) {
			((Input)inputOutput[i]).fillStatus(status.get("input:" + i));
		}
		if(addOn != null) {
			addOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		final String[] cmd = new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
				/*"/rpc/BTHome.GetConfig", "/rpc/BTHome.GetStatus",*/ "/rpc/XMOD.GetProductJWS", "/rpc/XMOD.GetInfo"};
		return (addOn != null) ? SensorAddOn.getInfoRequests(cmd) : cmd;
	}

	@Override
	protected void backup(FileSystem out) throws IOException, InterruptedException {
		sectionToStream("/rpc/XMOD.GetInfo", "XMOD.GetInfo.json", out);
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		configure(); // reload IO & addon configuration -  useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		JsonNode xmodStored = backupJsons.get("XMOD.GetInfo.json");
		int numStoredInputs = xmodStored.at("/jwt/xmod1/ni").intValue();
		int numStoredOutputs = xmodStored.at("/jwt/xmod1/no").intValue();
		if(numStoredInputs != numInputs || numStoredOutputs != numOutputs) {
			res.put(RestoreMsg.WARN_RESTORE_XMOD_IO, null);
		}
		SensorAddOn.restoreCheck(this, addOn, backupJsons, res);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		JsonNode xmodStored = backupJsons.get("XMOD.GetInfo.json");
		int numStoredInputs = xmodStored.at("/jwt/xmod1/ni").intValue();
		int numStoredOutputs = xmodStored.at("/jwt/xmod1/no").intValue();
		
		for(int i = 0; i < Math.min(numStoredInputs, numInputs); i++) {
			errors.add(Input.restore(this, configuration, i));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		}
		for(int i = 0; i < Math.min(numStoredOutputs, numOutputs); i++) {
			errors.add(((Relay)inputOutput[i]).restore(configuration));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		}
		
		SensorAddOn.restore(this, addOn, backupJsons, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Inputs: " + numInputs + "; Outputs: " + numOutputs;
	}
}