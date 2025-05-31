package it.usna.shellyscan.model.device.blu;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.blu.modules.InputOnDevice;
import it.usna.shellyscan.model.device.blu.modules.Sensor;
import it.usna.shellyscan.model.device.blu.modules.SensorsCollection;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.g2.modules.InputActionInterface;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.FirmwareManager;

/**
 * Generic BTHome device with measures and/or buttons
 * https://shelly-api-docs.shelly.cloud/gen2/DynamicComponents/BTHome/
 */
public class BTHomeDevice extends AbstractBluDevice implements ModulesHolder {
	public final static String GENERATION = "bth";
	private final static Logger LOG = LoggerFactory.getLogger(BTHomeDevice.class);
//	private final static Map<String, String> DEV_DICTIONARY = Map.of(
//			"SBBT-002C", "Blu Button", "SBMO-003Z", "BLU Motion",
//			"SBDW-002C", "Blu Door Window", "SBHT-003C", "Blu H&T",
//			"SBBT-004CEU", "Blu Wall Switch 4", "SBBT-004CUS", "Blu RC Button 4");
	private final static Map<Integer, String> MODELS_DICTIONARY = Map.of(
			1, "Blu Button",
			2, "Blu Door Window",
			3, "Blu H&T",
			5, "Blu Motion",
			6, "Blu Wall Switch 4", // Square
			7, "Blu RC Button 4", // line
			8, "Blu TRV",
//			9. "??",
			10, "Blu Distance"
			);
	private String typeName;
	private String typeID;
	private SensorsCollection sensors;
	private Meters[] meters;
	private Webhooks webhooks;
	private InputActionInterface[] inputs;
	private DeviceModule[] modules;

	public BTHomeDevice(AbstractG2Device parent, JsonNode compInfo, int modelId, String index) {
		super(parent, compInfo, index);
		typeID = "BLU" + modelId;

		String modelDesc = MODELS_DICTIONARY.get(modelId);
		this.typeName = (modelDesc == null) ? "Generic BTHome" : modelDesc;

		this.webhooks = new Webhooks(parent);
		this.uptime = -1;
	}

	@Override
	public void init(HttpClient httpClient) throws IOException {
		this.httpClient = httpClient;
		initSensors();
		hostname = "B" + sensors.toString() + "-" + mac;
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		refreshStatus();
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		refreshSettings();
	}
	
	private void initSensors() throws IOException {
		this.sensors = new SensorsCollection(this);
		this.meters = sensors.getTypes().length > 0 ? new Meters[] {sensors} : null;
		
		ArrayList<DeviceModule> tmpModules = sensors.getModuleSensors();
		List<InputActionInterface> tmpInputs = tmpModules.stream().filter(m -> m instanceof InputActionInterface).map(InputActionInterface.class::cast).collect(Collectors.toList());
		
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		
		// device inputs
		webhooks.fillBTHomesensorSettings();
		List<Webhook> devActions = webhooks.getHooksList(DynamicComponents.BTHOME_DEVICE + componentIndex);
		if(devActions != null) {
			List<InputOnDevice> devIn = deviceInputs(devActions);
			tmpInputs.addAll(devIn);
			tmpModules.addAll(devIn);
		}
		this.inputs = tmpInputs.toArray(InputActionInterface[]::new);
		this.modules = tmpModules.toArray(DeviceModule[]::new);
	}
	
	private List<InputOnDevice> deviceInputs(List<Webhook> devActions) {
		HashSet<String> set = new HashSet<>();
		for(Webhook hook: devActions) {
			String condition = hook.getCondition();
			if(condition == null || condition.isEmpty()) {
				set.add(""); // need a string to sort 
			} else if(condition.startsWith("ev.idx == ")) { //  "condition" : "ev.idx == 0",
				try {
					set.add(condition);
				} catch(RuntimeException e) {
					LOG.error("Unexpected contition {}", condition);
				}
			}
		}
		return set.stream().sorted().map(cond -> new InputOnDevice(cond, componentIndex)).toList();
	}
	
	public void setTypeName(String name) {
		typeName = name;
	}
	
	@Override
	public String getTypeID() {
		return typeID;
	}
	
	@Override
	public String getTypeName() {
		return typeName;
	}
	
	@Override
	public int getModulesCount() {
		return modules.length;
	}

	@Override
	public DeviceModule[] getModules() {
		return /*inputs*/modules;
	}
	
	@Override
	public void refreshStatus() throws IOException {
		JsonNode components = getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
		String k;
		boolean devExists = false;
		for(JsonNode comp: components) {
			if(devExists == false && comp.path("key").textValue().equals(DEVICE_KEY_PREFIX + componentIndex)) { // devExists == false for efficiency
				fillSettings(comp.path("config"));
				fillStatus(comp.path("status"));
				devExists = true;
			} else if((k = comp.path("key").textValue()).startsWith(SENSOR_KEY_PREFIX)) {
				int id = Integer.parseInt(k.substring(13));
				Sensor sensor = sensors.getSensor(id);
				if(sensor != null) {
					sensor.fill(comp);
				}
			}
		}
		if(devExists == false) {
			this.rssi = 0;
		}
	}
	
	@Override
	public void refreshSettings() throws IOException {
		if(inputs.length > 0) {
			webhooks.fillBTHomesensorSettings();
			for(int i = 0; i < inputs.length; i++) {
				inputs[i].associateWH(webhooks);
			}
		}
	}
	
	private void fillSettings(JsonNode config) {
		this.name = config.path("name").asText("");
	}

	private void fillStatus(JsonNode status) {
		this.rssi = status.path("rssi").intValue();
		this.lastConnection = status.path("last_updated_ts").intValue() * 1000L;
		//	this.battery = status.path("battery").intValue(); // there is a specific sensor for this
	}

	@Override
	public String[] getInfoRequests() {
		ArrayList<String> l = new ArrayList<String>(Arrays.asList(
				"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex));
		for(Sensor s: sensors.getSensors()) {
			l.add("(BTHomeSensor.GetConfig [" + s.getId() + "])/rpc/BTHomeSensor.GetConfig?id=" + s.getId());
			l.add("(BTHomeSensor.GetStatus [" + s.getId() + "])/rpc/BTHomeSensor.GetStatus?id=" + s.getId());
		}
		return l.toArray(String[]::new);
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public FirmwareManager getFWManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean backup(Path file) throws IOException {
		ObjectNode usnaData = JsonNodeFactory.instance.objectNode();
		usnaData.put("index", componentIndex);
		usnaData.put("type", typeID);
		usnaData.put("mac", mac);
		Files.deleteIfExists(file);
		try(FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + file.toUri()), Map.of("create", "true"));
			BufferedWriter writer = Files.newBufferedWriter(fs.getPath("ShellyScannerBLU.json"))) {
			jsonMapper.writer().writeValue(writer, usnaData);

			sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", fs); // "status" is used for groups
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", fs);
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}
	
	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) {
		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
		JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
		if(usnaInfo == null || usnaInfo.path("type").asText("?").equals(typeID) == false) {
			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			return res;
		}
		// questo ...
		String fileMac;
		if((fileMac = usnaInfo.path("mac").asText("?")).equals(mac) == false) {
			res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, "mac: " + fileMac);
		}
//		// invece di questo
//		final String fileComponentIndex = usnaInfo.get("index").asText();
//		JsonNode fileComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
//		for(JsonNode fileComp: fileComponents) {
//			if(fileComp.path("key").textValue().equals(DEVICE_KEY_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
//				/*String*/ fileMac = fileComp.path("config").path("addr").textValue();
//				if(fileMac.equals(mac) == false) {
//					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, "mac: " + fileMac);
//				}
//				break;
//			}
//		}
		return res;
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) {
		final ArrayList<String> errors = new ArrayList<>();
		try {
			initSensors(); // in case they have been altered (from the web GUI)
			
			// Store groups components into HashMap<String, ArrayNode> groups (they will be removed deleting a sensor)
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			JsonNode currentComponents = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true&include=[%22status%22]");
			HashMap<String, ArrayNode> existingGroups = new HashMap<>();
			for (JsonNode comp: currentComponents.path("components")) {
				String key = comp.get("key").asText();
				if(key.startsWith(GROUP_KEY_PREFIX)) {
					existingGroups.put(key, (ArrayNode)comp.path("status").get("value"));
				}
			}

			JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
			String fileComponentIndex = usnaInfo.get("index").textValue();
			JsonNode fileComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
			JsonNode storedWebHooks = backupJsons.get("Webhook.List.json");
			String fileAddr = null;
			// BLU configuration: Device
			for(JsonNode fileComp: fileComponents) {
				if(fileComp.path("key").textValue().equals(DEVICE_KEY_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					final int currentComponentIndex = Integer.parseInt(componentIndex);
					out.put("id", currentComponentIndex); // could be different
					ObjectNode config = (ObjectNode)fileComp.path("config").deepCopy();
					config.remove("id");
					config.remove("addr"); // new (registered on host) addr could not be the stored addr
					out.set("config", config);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(parent.postCommand("BTHomeDevice.SetConfig", out));
					fileAddr = fileComp.at("/config/addr").textValue();
					
					// todo /attrs/flags ? Valuable values here?

					Webhooks.delete(parent, DynamicComponents.BTHOME_DEVICE, currentComponentIndex, Devices.MULTI_QUERY_DELAY);//					Webhooks.restore(parent, DEVICE_KEY_PREFIX + fileComponentIndex, DEVICE_KEY_PREFIX + componentIndex, Devices.MULTI_QUERY_DELAY, storedWebHooks, errors);
					Webhooks.restore(parent, DynamicComponents.BTHOME_DEVICE, Integer.parseInt(fileComponentIndex), currentComponentIndex, storedWebHooks, Devices.MULTI_QUERY_DELAY, errors);
					
					break;
				}
			}

			// Sensors (look for MAC)
			HashMap<String, String> sensorsDictionary = new HashMap<>(); // old-new key ("bthomesensor:200"-"bthomesensor:201")
			errors.add(sensors.deleteAll()); // deleting a sensor all related webhooks are removed
			for(JsonNode fileComp: fileComponents) {
				final String fileKey = fileComp.path("key").textValue();
				if(fileKey.startsWith(SENSOR_KEY_PREFIX) && fileComp.at("/config/addr").textValue().equals(fileAddr)) {
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					ObjectNode config = (ObjectNode)fileComp.path("config");
					config.put("addr", this.mac);
					out.set("config", config);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					String newKey = parent.getJSON("BTHome.AddSensor", out).get("added").textValue(); // BTHome.AddSensor -> {"added":"bthomesensor:200"}
					Webhooks.restore(parent, fileKey, newKey, storedWebHooks, Devices.MULTI_QUERY_DELAY, errors); // Webhook.Create - deleting a sensor all related webhooks are removed
					
					sensorsDictionary.put(fileKey, newKey);
				}
			}
			
			// restore groups
			for(Map.Entry<String, ArrayNode> group: existingGroups.entrySet()) {
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

/*
"BTHomeSensor.SetConfig",
"BTHomeSensor.GetConfig",
"BTHomeSensor.GetStatus",
"BTHomeDevice.UpdateFirmware",
"BTHomeDevice.GetKnownObjects",
"BTHomeDevice.SetConfig",
"BTHomeDevice.GetConfig",
"BTHomeDevice.GetStatus",
"BTHome.GetObjectInfos",
"BTHome.DeleteSensor",
"BTHome.AddSensor",
"BTHome.DeleteDevice",
"BTHome.AddDevice",
"BTHome.StartDeviceDiscovery",
"BTHome.SetConfig",
"BTHome.GetConfig",
"BTHome.GetStatus"
*/