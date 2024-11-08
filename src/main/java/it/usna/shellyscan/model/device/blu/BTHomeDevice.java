package it.usna.shellyscan.model.device.blu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.blu.modules.Sensor;
import it.usna.shellyscan.model.device.blu.modules.SensorsCollection;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Generic BTHome device with measures and/or buttons
 * https://shelly-api-docs.shelly.cloud/gen2/DynamicComponents/BTHome/
 */
public class BTHomeDevice extends AbstractBluDevice implements ModulesHolder {
	private final static Logger LOG = LoggerFactory.getLogger(BTHomeDevice.class);
	private final static Map<String, String> DEV_DICTIONARY = Map.of(
			"SBBT-002C", "Blu Button",
			"SBMO-003Z", "BLU Motion",
			"SBDW-002C", "Blu Door Window",
			"SBHT-003C", "Blu H&T",
			"SBBT-004CEU", "Blu Wall Switch 4",
			"SBBT-004CUS", "Blu RC Button 4"
			);
	private final static Map<String, String> DEV_DICTIONARY_NEW = Map.of(
			"SBBT-2C", "Blu Button",
			"SBMO-3Z", "BLU Motion",
			"SBDW-2C", "Blu Door Window",
			"SBHT-3C", "Blu H&T",
			"SBBT-EU", "Blu Wall Switch 4",
			"SBBT-US", "Blu RC Button 4"
			);
	private String typeName;
	private String localName;
	private SensorsCollection sensors;
	private Meters[] meters;
	private Webhooks webhooks = new Webhooks(parent);
	private Input[] inputs;

	public BTHomeDevice(ShellyAbstractDevice parent, JsonNode compInfo, String localName, String index) {
		super((AbstractG2Device)parent, compInfo, index);
//		this.typeName = Optional.ofNullable(DEV_DICTIONARY.get(localName)).orElse("Generic BTHome");

		this.typeName = DEV_DICTIONARY.get(localName); // old fw
		int len;
		if(this.typeName == null && (len = localName.length()) > 4) {
			String tmpLocalName = localName.substring(0, len -4);
			this.typeName = DEV_DICTIONARY_NEW.get(tmpLocalName); // new fw
			if(this.typeName != null) {
				localName = tmpLocalName;
			}
		}
		if(this.typeName == null) {
			this.typeName = "Generic BTHome";
		}
		
		this.hostname = localName + "-" + mac;
		this.localName = localName;
		this.webhooks = new Webhooks(this.parent);
	}

	@Override
	public void init(HttpClient httpClient) throws IOException {
		this.httpClient = httpClient;
		initSensors();
		if(localName.isEmpty()) {
			localName = sensors.toString();
			hostname = "B" + localName + "-" + mac;
		}
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		refreshStatus();
		refreshSettings();
	}
	
	private void initSensors() throws IOException {
		sensors = new SensorsCollection(this);
		meters = sensors.getTypes().length > 0 ? new Meters[] {sensors} : null;
		
		int numInputs = sensors.getInputSensors().length;
		this.inputs = new Input[numInputs];
		for(int i = 0; i < numInputs; i++) {
			inputs[i] = new Input(/*this.parent, in[i].getId()*/);
			inputs[i].setEnabled(true);
		}
	}
	
	public void setTypeName(String name) {
		typeName = name;
	}
	
	@Override
	public String getTypeID() {
		return localName;
	}
	
	@Override
	public String getTypeName() {
		return typeName;
	}
	
	@Override
	public int getModulesCount() {
		return inputs.length;
	}

	@Override
	public DeviceModule getModule(int index) {
		return inputs[index];
	}

	@Override
	public DeviceModule[] getModules() {
		return inputs;
	}
	
	@Override
	public void refreshStatus() throws IOException {
		JsonNode components = getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
		String k;
		boolean devExists = false;
		for(JsonNode comp: components) {
			if(comp.path("key").textValue().equals(DEVICE_KEY_PREFIX + componentIndex)) {
				fillSettings(comp.path("config"));
				fillStatus(comp.path("status"));
				devExists = true;
			} else if((k = comp.path("key").textValue()).startsWith(SENSOR_KEY_PREFIX)) {
				int id = Integer.parseInt(k.substring(13));
				Sensor s = sensors.getSensor(id);
				if(s != null) {
					s.fillSConfig(comp.path("config"));
					s.fillStatus(comp.path("status"));
				}
				devExists = true;
			}
		}
		if(devExists == false) {
			this.rssi = 0;
		}
	}
	
	@Override
	public void refreshSettings() throws IOException {
		webhooks.fillBTHomesensorSettings();
		
		Sensor in[] = sensors.getInputSensors();
		for(int i = 0; i < in.length; i++) {
			inputs[i].setLabel(in[i].getName());
			inputs[i].associateWH(webhooks.getHooks(in[i].getId()));
		}
	}
	
	private void fillSettings(JsonNode config) {
		this.name = config.path("name").asText("");
	}

	private void fillStatus(JsonNode status) {
		this.rssi = status.path("rssi").intValue();
		this.lastConnection = status.path("last_updated_ts").intValue() * 1000L;
		//	this.battery = status.path("battery").intValue();
	}

	@Override
	public String[] getInfoRequests() {
		ArrayList<String> l = new ArrayList<String>(Arrays.asList(
				"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex));
		for(Sensor s: sensors.getSensors()) {
			l.add("()/rpc/BTHomeSensor.GetConfig?id=" + s.getId());
			l.add("()/rpc/BTHomeSensor.GetStatus?id=" + s.getId());
		}
		return l.toArray(String[]::new);
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public boolean backup(File file) throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		ObjectMapper mapper = new ObjectMapper(jsonFactory);
		
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			// ShellyScanner.json
			ZipEntry entry = new ZipEntry("ShellyScannerBLU.json");
			out.putNextEntry(entry);
			ObjectNode usnaData = JsonNodeFactory.instance.objectNode();
			usnaData.put("index", componentIndex);
			usnaData.put("type", localName);
			mapper.writeValue(out, usnaData);
			out.closeEntry();
			
			sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}
	
	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
		JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
		String fileLocalName;
		if(usnaInfo == null || (fileLocalName = usnaInfo.path("type").asText("")).equals(localName) == false) {
			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			return res;
		}
		final String fileComponentIndex = usnaInfo.get("index").asText();
		JsonNode fileComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
		for(JsonNode fileComp: fileComponents) {
			if(fileComp.path("key").textValue().equals(DEVICE_KEY_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
				String fileMac = fileComp.path("config").path("addr").textValue();
				if(fileMac.equals(mac) == false) {
					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, fileLocalName + "-" + fileMac);
				}
				break;
			}
		}
		return res;
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) {
		final ArrayList<String> errors = new ArrayList<>();
		try {
			initSensors(); // in case they have been altered (from the web GUI)
			
			// Store groups components into HashMap<String, ArrayNode> groups (they will be removed deleting a sensor)
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			JsonNode currentComponents = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true");
			HashMap<String, ArrayNode> groups = new HashMap<>();
			for (JsonNode storedComp: currentComponents.path("components")) {
				String key = storedComp.get("key").asText();
				if(key.startsWith(GROUP_KEY_PREFIX)) {
					groups.put(key, (ArrayNode)storedComp.path("status").get("value"));
				}
			}

			JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
			String fileComponentIndex = usnaInfo.get("index").textValue();
			JsonNode fileComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
			String fileAddr = null;
			// BLU configuration: Device
			for(JsonNode fileComp: fileComponents) {
				if(fileComp.path("key").textValue().equals(DEVICE_KEY_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					out.put("id", Integer.parseInt(componentIndex)); // could be different
					ObjectNode config = (ObjectNode)fileComp.path("config").deepCopy();
					config.remove("id");
					config.remove("addr"); // new (registered on host) addr could not be the stored addr
					out.set("config", config);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(parent.postCommand("BTHomeDevice.SetConfig", out));
					fileAddr = fileComp.at("/config/addr").textValue();
					break;
				}
			}

			// Sensors
			HashMap<String, String> sensorsDictionary = new HashMap<>(); // old-new key ("bthomesensor:200"-"bthomesensor:201")
			JsonNode storedWebHooks = backupJsons.get("Webhook.List.json");
			errors.add(sensors.deleteAll()); // deleting a sensor all related webhooks are also deleted
			for(JsonNode fileComp: fileComponents) {
				final String fileKey = fileComp.path("key").textValue();
				if(fileKey.startsWith(SENSOR_KEY_PREFIX) && fileComp.at("/config/addr").textValue().equals(fileAddr)) {
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					ObjectNode config = (ObjectNode)fileComp.path("config");
					config.put("addr", this.mac);
					out.set("config", config);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					String newKey = parent.getJSON("BTHome.AddSensor", out).get("added").textValue(); // BTHome.AddSensor -> {"added":"bthomesensor:200"}
					Webhooks.restore(parent, fileKey, newKey, Devices.MULTI_QUERY_DELAY, storedWebHooks, errors); // Webhook.Create
					
					sensorsDictionary.put(fileKey, newKey);
				}
			}
			
			// restore groups
			for(Map.Entry<String, ArrayNode> group: groups.entrySet()) {
				ArrayNode conponents = group.getValue();
				boolean change = false;
				for(int i = 0; i < conponents.size(); i++) {
					String newKey = sensorsDictionary.get(conponents.get(i).textValue());
					if(newKey != null) {
						change = true;
						conponents.set(i, newKey);
					}
				}
				if(change) {
					ObjectNode grValue = JsonNodeFactory.instance.objectNode();
					grValue.put("id", Integer.parseInt(group.getKey().substring(6)));
					grValue.set("value", conponents);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(parent.postCommand("Group.Set", grValue));
				}
			}
			
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			initSensors();
			// RestoreAction do refreshSettings(); refreshStatus(); here we need a refreshStatus(); before refreshSettings(); for input names
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			refreshStatus();
		} catch(IOException | RuntimeException | InterruptedException e) {
			LOG.error("restore - RuntimeException", e);
			errors.add(RestoreMsg.ERR_UNKNOWN.toString());
		}
		return errors;
	}
}

//https://smarthomecircle.com/connect-xiaomi-temperature-and-humidity-bluetooth-sensor-to-home-assistant