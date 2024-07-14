package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOnHolder;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.MixedModuleHolder;

/**
 * Shelly X MOD1 model
 * @author usna
 */
//TODO gestire eventuali input in eccesso (o separare tutto) creando un array di LabelHolder (?) e appositi renderers/editors
public class ShellyXMOD1 extends AbstractG3Device implements MixedModuleHolder, SensorAddOnHolder {
	public final static String ID = "XMOD1";
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
			if(addOn.getTypes().length > 0) {
				meters = new Meters[] {addOn};
			}
		}

		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly X MOD1";
	}
	
//	@Override
//	public Relay getRelay(int index) {
//		return /*(Relay)*/inputOutput[index];
//	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
//	@Override
//	public Relay[] getRelays() {
//		return /*(Relay[])*/inputOutput;
//	}
	
//	@Override
//	public int getRelayCount() {
//		return numOutputs;
//	}

	@Override
	public DeviceModule getModule(int index) {
		return inputOutput[index];
	}

	@Override
	public DeviceModule[] getModules() {
		return inputOutput;
	}

	@Override
	public int getModuleCount() {
		return numModules;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public SensorAddOn getSensorAddOn() {
		return addOn;
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
		if(addOn != null) {
			return new String[] {
					"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
					"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
					/*"/rpc/BTHome.GetConfig", "/rpc/BTHome.GetStatus",*/ "/rpc/SensorAddon.GetPeripherals", "/rpc/XMOD.GetProductJWS", "/rpc/XMOD.GetInfo"};
		} else {
			return new String[] {
					"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
					"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
					/*"/rpc/BTHome.GetConfig", "/rpc/BTHome.GetStatus",*/ "/rpc/XMOD.GetProductJWS", "/rpc/XMOD.GetInfo"};
		}
	}
	
	//TODO warning on numStoredInputs != numInputs || numStoredOutputs != numOutputs
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<Restore, Object
			> res) {
		if(SensorAddOn.restoreCheck(this, backupJsons, res) == false) {
			res.put(Restore.WARN_RESTORE_ADDON, null);
		}
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
		
		SensorAddOn.restore(this, backupJsons, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Inputs: " + numInputs + "; Outputs: " + numOutputs;
	}
}