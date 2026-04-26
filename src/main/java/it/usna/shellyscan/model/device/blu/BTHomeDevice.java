package it.usna.shellyscan.model.device.blu;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
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
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Generic BTHome device with measures and/or buttons
 * https://shelly-api-docs.shelly.cloud/gen2/DynamicComponents/BTHome/
 */
public class BTHomeDevice extends AbstractBTHomeDevice implements ModulesHolder {
	public final static String GENERATION = "bth";
	public static final String DEVICE_KEY_PREFIX = DynamicComponents.BTHOME_DEVICE + ":"; // "bthomedevice:";
	public static final String SENSOR_KEY_PREFIX = DynamicComponents.BTHOME_SENSOR + ":"; // "bthomesensor:";
	private static final String GROUP_KEY_PREFIX = DynamicComponents.GROUP_TYPE + ":"; // "group:";
	private static final  Logger LOG = LoggerFactory.getLogger(BTHomeDevice.class);
//	private final static Map<String, String> DEV_DICTIONARY = Map.of(
//			"SBBT-002C", "Blu Button", "SBMO-003Z", "BLU Motion",
//			"SBDW-002C", "Blu Door Window", "SBHT-003C", "Blu H&T",
//			"SBBT-004CEU", "Blu Wall Switch 4", "SBBT-004CUS", "Blu RC Button 4");

	private static final Map<Integer, String> MODELS_DICTIONARY =  Map.ofEntries(
			Map.entry(1, "Blu Button"),
			Map.entry(2, "Blu Door Window"),
			Map.entry(3, "Blu H&T"),
			Map.entry(5, "Blu Motion"),
			Map.entry(6, "Blu Wall Switch 4"), // Square
			Map.entry(7, "Blu RC Button 4"), // line
			Map.entry(8, "Blu TRV"),
			Map.entry(9, "Blu Remote"),
			Map.entry(0x0A, "Blu Distance"), // 10
			Map.entry(0x0B, "Weather Station"), // 11
			Map.entry(0x0C, "Blu H&T Display ZB"), // 12
			Map.entry(0x11, "Blu H&T ZB"), // 17
			Map.entry(0x13, "Blu ---"), // 19
			Map.entry(0x14, "Blu Door Window ZB"), // 20
			Map.entry(0x15, "Blu Wall Switch 4 ZB"), // 21
			Map.entry(0x16, "Blu RC Button 4 ZB"), // 22 - line
			Map.entry(0x17, "Blu Button Tough 1 ZB") // 23
			);
	private String typeName;
	private String typeID;
	private SensorsCollection sensors;
	private Meters[] meters;
	private Webhooks webhooks;
	private InputActionInterface[] inputs;
	private DeviceModule[] modules;
	private String componentsKeys;

	public BTHomeDevice(AbstractG2Device parent, JsonNode compInfo, int modelId, String index) {
		super(parent, compInfo.path("config").path("addr").asString(""), index);
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
		hostname = "B" + sensors.getFullID() + "-" + mac;
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		refreshSettings();
	}
	
	private void initSensors() throws IOException {
		this.sensors = new SensorsCollection(this);
		this.meters = sensors.getTypes().length > 0 ? new Meters[] {sensors} : null;
		
		// generare key argument to retrive related components
		StringBuilder keysBuilder = new StringBuilder("[\"");
		keysBuilder.append(DEVICE_KEY_PREFIX);
		keysBuilder.append(componentIndex);
		for(Sensor s: sensors.getSensors()) {
			keysBuilder.append("\",\"");
			keysBuilder.append(SENSOR_KEY_PREFIX);
			keysBuilder.append(s.getId());
		}
		keysBuilder.append("\"]");
		componentsKeys = URLEncoder.encode(keysBuilder.toString(), StandardCharsets.UTF_8.name());

		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		refreshStatus(); // init status for this.sensors
		
		List<DeviceModule> tmpModules = sensors.getModuleSensors();
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
			set.add(condition == null ? "" : condition);
		}
		return set.stream().sorted().map(cond -> new InputOnDevice(cond, componentIndex/*, sensors*/)).toList();
	}
	
	public void setTypeName(String name) {
		typeName = name;
	}
	
	@Override
	public String getGeneration() {
		return GENERATION;
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
		return modules;
	}
	
	@Override
	public void refreshStatus() throws IOException {
		Iterator<JsonNode> componentsIt = getJSONIterator("/rpc/Shelly.GetComponents?keys=" + componentsKeys, "components");
		
		String compKey;
		boolean devExists = false;
		while(componentsIt.hasNext()) {
			JsonNode comp = componentsIt.next();
			if((compKey = comp.path("key").asString()).startsWith(SENSOR_KEY_PREFIX)) {
				int id = Integer.parseInt(compKey.substring(13));
				sensors.getSensor(id).fill(comp);
			} else { // not a sensor -> is the device
				fillSettings(comp.path("config"));
				fillStatus(comp.path("status"));
				devExists = true;
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
		this.name = config.path("name").asString("");
	}

	private void fillStatus(JsonNode status) {
		this.rssi = status.path("rssi").intValue(0);
		this.lastConnection = status.path("last_updated_ts").intValue(0) * 1000L;
		//	this.battery = status.path("battery").intValue(0); // there is a specific sensor for this
	}

	@Override
	public String[] getInfoRequests() {
		ArrayList<String> l = new ArrayList<String>(Arrays.asList(
				"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex));
		for(Sensor s: sensors.getSensors()) {
			l.add("(BTHomeSensor.GetConfig [" + s.getId() + "-" + s.getObjId() + "])/rpc/BTHomeSensor.GetConfig?id=" + s.getId());
			l.add("(BTHomeSensor.GetStatus [" + s.getId() + "-" + s.getObjId() + "])/rpc/BTHomeSensor.GetStatus?id=" + s.getId());
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
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8)) {
			ZipEntry entry = new ZipEntry("ShellyScannerBLU.json");
			out.putNextEntry(entry);
			jsonMapper.writer().writeValue(out, usnaData);

			sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "components", "Shelly.GetComponents.json", out); // "status" is used for groups
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}
	
	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) {
		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
		JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
		if(usnaInfo == null || usnaInfo.path("type").asString("?").equals(typeID) == false) {
			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			return res;
		}
		String fileMac;
		if((fileMac = usnaInfo.path("mac").asString("?")).equals(mac) == false) {
			res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, "mac: " + fileMac);
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
			HashMap<String, ArrayNode> existingGroups = new HashMap<>();
			
			Iterator<JsonNode> currentComponentsIt = getJSONIterator("/rpc/Shelly.GetComponents?dynamic_only=true&include=[%22status%22]", "components");
			currentComponentsIt.forEachRemaining(comp -> {
				String key = comp.get("key").asString("");
				if(key.startsWith(GROUP_KEY_PREFIX)) {
					existingGroups.put(key, (ArrayNode)comp.path("status").get("value"));
				}
			});

			JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
			String fileComponentIndex = usnaInfo.get("index").asString("");
			JsonNode fileComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
			JsonNode storedWebHooks = backupJsons.get("Webhook.List.json");
			String fileAddr = null;
			// BLU configuration: Device
			for(JsonNode fileComp: fileComponents) {
				if(fileComp.path("key").asString("").equals(DEVICE_KEY_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					final int currentComponentIndex = Integer.parseInt(componentIndex);
					out.put("id", currentComponentIndex); // could be different
					ObjectNode config = (ObjectNode)fileComp.path("config").deepCopy();
					config.remove("id");
					config.remove("addr"); // new (registered on host) addr could not be the stored addr
					out.set("config", config);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(parent.postCommand("BTHomeDevice.SetConfig", out));
					fileAddr = fileComp.at("/config/addr").asString("");
					
					// /attrs/flags ? Valuable values here?

					Webhooks.delete(parent, DynamicComponents.BTHOME_DEVICE, currentComponentIndex, Devices.MULTI_QUERY_DELAY); // Webhooks.restore(parent, DEVICE_KEY_PREFIX + fileComponentIndex, DEVICE_KEY_PREFIX + componentIndex, Devices.MULTI_QUERY_DELAY, storedWebHooks, errors);
					Webhooks.restore(parent, DynamicComponents.BTHOME_DEVICE, Integer.parseInt(fileComponentIndex), currentComponentIndex, storedWebHooks, Devices.MULTI_QUERY_DELAY, errors);
					
					break;
				}
			}

			// Sensors (look for MAC)
			HashMap<String, String> sensorsDictionary = new HashMap<>(); // old-new key ("bthomesensor:200"-"bthomesensor:201")
			errors.add(sensors.deleteAll()); // deleting a sensor all related webhooks are removed
			for(JsonNode fileComp: fileComponents) {
				final String fileKey = fileComp.path("key").asString("");
				if(fileKey.startsWith(SENSOR_KEY_PREFIX) && fileComp.at("/config/addr").asString("").equals(fileAddr)) {
					ObjectNode out = JsonNodeFactory.instance.objectNode();
					ObjectNode config = (ObjectNode)fileComp.path("config");
					config.put("addr", this.mac);
					out.set("config", config);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					String newKey = parent.getJSON("BTHome.AddSensor", out).get("added").asString(""); // BTHome.AddSensor -> {"added":"bthomesensor:200"}
					Webhooks.restore(parent, fileKey, newKey, storedWebHooks, Devices.MULTI_QUERY_DELAY, errors); // Webhook.Create - deleting a sensor all related webhooks are removed
					
					sensorsDictionary.put(fileKey, newKey);
				}
			}
			
			// restore groups
			for(Map.Entry<String, ArrayNode> group: existingGroups.entrySet()) {
				ArrayNode conponents = group.getValue();
				boolean change = false;
				for(int i = 0; i < conponents.size(); i++) {
					String newKey = sensorsDictionary.get(conponents.get(i).asString(""));
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
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			refreshStatus();
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