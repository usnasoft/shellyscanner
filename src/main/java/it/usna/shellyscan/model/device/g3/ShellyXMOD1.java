package it.usna.shellyscan.model.device.g3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOnHolder;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.ModulesHolder;

/**
 * Shelly X MOD1 model
 * @author usna
 */
public class ShellyXMOD1 extends AbstractG3Device implements ModulesHolder, SensorAddOnHolder {
	private final static Logger LOG = LoggerFactory.getLogger(ShellyXMOD1.class);
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
			if(addOn.getTypes().length > 0) {
				meters = new Meters[] {addOn};
			}
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
	public DeviceModule getModule(int index) {
		return inputOutput[index];
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
	
	@Override
	// add /rpc/XMOD.GetInfo to standard gen3 backup
	public boolean backup(final File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { //unmanaged battery device
				sectionToStream("/rpc/Schedule.List", "Schedule.List.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try {
				sectionToStream("/rpc/KVS.GetMany", "KVS.GetMany.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			byte[] scripts = null;
			try {
				scripts = sectionToStream("/rpc/Script.List", "Script.List.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // Virtual components
				sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // On devices with active sensor add-on
				sectionToStream("/rpc/SensorAddon.GetPeripherals", SensorAddOn.BACKUP_SECTION, out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // THIS IS SPECIFIC FOR XMOD1
				sectionToStream("/rpc/XMOD.GetInfo", "XMOD.GetInfo.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			// Scripts
			if(scripts != null) {
				JsonNode scrList = jsonMapper.readTree(scripts).get("scripts");
				for(JsonNode scr: scrList) {
					try {
						Script script = new Script(this, scr);
						byte[] code =  script.getCode().getBytes();
						ZipEntry entry = new ZipEntry(scr.get("name").asText() + ".mjs");
						out.putNextEntry(entry);
						out.write(code, 0, code.length);
					} catch(IOException e) {}
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			}
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<Restore, Object> res) throws IOException {
		configure(); // reload IO & addon configuration -  useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		JsonNode xmodStored = backupJsons.get("XMOD.GetInfo.json");
		int numStoredInputs = xmodStored.at("/jwt/xmod1/ni").intValue();
		int numStoredOutputs = xmodStored.at("/jwt/xmod1/no").intValue();
		if(numStoredInputs != numInputs || numStoredOutputs != numOutputs) {
			res.put(Restore.WARN_RESTORE_XMOD_IO, null);
		}
		SensorAddOn.restoreCheck(this, backupJsons, res);
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