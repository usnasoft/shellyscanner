package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reference: https://shelly-api-docs.shelly.cloud/gen2/DynamicComponents/
 * <br>
 * IDs for these components start from 200 and are limited to 299.
 * <br>
 * List: <IP>/rpc/Shelly.GetComponents?dynamic_only=true
 */
public class DynamicComponents {
	private static final Logger LOG = LoggerFactory.getLogger(DynamicComponents.class);
	
	public static final String GROUP_TYPE = "group";
	public static final String[] VIRTUAL_TYPES = {"boolean", "number", "text", "enum", GROUP_TYPE, "button"};
	public static final String BTHOME_DEVICE = "bthomedevice";
	public static final String BTHOME_SENSOR = "bthomesensor";
	public static final String LNM_COMPONENT = "lnm";

	public static final int MIN_ID = 200;
	public static final int MAX_ID = 299;

//	private static final int LATENCY = 1250;
//	private final AbstractG2Device parent;
//	private long lastFetch = 0;
//	private ArrayList<JsonNode> bthDevices = new ArrayList<>();
//	private ArrayList<JsonNode> bthSensors = new ArrayList<>();
	
//	public DynamicComponents(AbstractG2Device device) {
//		this.parent = device;
//	}
//	
//	private void readComponents() throws IOException {
//		//		long currentTime = System.currentTimeMillis();
//		//		if(currentTime > lastFetch + LATENCY) {
//		bthDevices.clear();
//		bthSensors.clear();
//		JsonPageIterator it = new JsonPageIterator(parent, "/rpc/Shelly.GetComponents?dynamic_only=true", "components");
//		it.forEach(node -> {
//			if(node.path("key").asString().startsWith(DEVICE_KEY_PREFIX)) {
//				bthDevices.add(node);
//			} else if(node.path("key").asString().startsWith(SENSOR_KEY_PREFIX)) {
//				bthSensors.add(node);
//			}
//		});
//		//			lastFetch = currentTime;
//		//		}
//	}
//	
//	public JsonNode getComponentNode(String index) throws IOException {
//		long currentTime = System.currentTimeMillis();
//		if(currentTime > lastFetch + LATENCY) {
////			System.out.println(currentTime - lastFetch);
//			readComponents();
//			
//			lastFetch = currentTime;
//			
//		} else {
//			System.out.println("risparmio");
//		}
//		
//		for(JsonNode dev: bthDevices) {
//			if(dev.path("key").asString().equals(DEVICE_KEY_PREFIX + index)) {
//				return dev;
//			}
//		}
//		return null;
//	}
//	
//	public List<JsonNode> getSensors() {
//		return bthSensors;
//	}
	
	private DynamicComponents() {}

	/**
	 * Remove all dynamic components except BTHomeDevice(s).<br>
	 * Note: if a component is removed and it is grouped it is also removed from its group  
	 * @return the List<String> of (not removed) BTHomeDevice(s) mac addresses.
	 */
	private static List<String> deleteAll(AbstractG2Device parent) throws IOException, InterruptedException {
		final List<String> devicesAddress = new ArrayList<>();
		Iterator<JsonNode> compIt = parent.getJSONIterator("/rpc/Shelly.GetComponents?dynamic_only=true&include=[%22config%22]", "components");
		while (compIt.hasNext()) {
			JsonNode comp = compIt.next();
			String key = comp.get("key").asString("");
			if(Arrays.stream(VIRTUAL_TYPES).anyMatch(type -> key/*.toLowerCase()*/.startsWith(type/*.toLowerCase()*/ + ":"))) { // VIRTUAL_TYPES
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				parent.postCommand("Virtual.Delete", "{\"key\":\"" + key + "\"}");
			} else if(key.toLowerCase().startsWith(BTHOME_SENSOR + ":")) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				String typeIdx[] = key.split(":");
				parent.postCommand("BTHome.DeleteSensor", "{\"id\":" + typeIdx[1] + "}");
			} else if(key.toLowerCase().startsWith(LNM_COMPONENT + ":")) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				String typeIdx[] = key.split(":");
				parent.postCommand("LNM.Delete", "{\"id\":" + typeIdx[1] + "}");
			} else if(key.toLowerCase().startsWith(BTHOME_DEVICE + ":")) {
				devicesAddress.add(comp.at("/config/addr").asString(""));
			}
		}
		return devicesAddress;
	}
	
	public static void restoreCheck(AbstractG2Device parent, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		try {
			JsonNode storedComponentsFile = backupJsons.get("Shelly.GetComponents.json");
			JsonNode storedComponents;
			if(storedComponentsFile != null && (storedComponents = storedComponentsFile.get("components")).size() > 0) {
				JsonNode currenteComponents = parent.getPagedJson("/rpc/Shelly.GetComponents?dynamic_only=true&include=[%22config%22]", "components").path("components");

				// BTHomeDevice -> stored ones are already installed on the device?
				Iterator<JsonNode> storedIt = storedComponents.iterator();
				while (storedIt.hasNext()) {
					JsonNode storedComp = storedIt.next();
					String storedKey = storedComp.get("key").asString("").toLowerCase();
					if(storedKey.startsWith("bthomedevice:")) {
						boolean exists = false;
						Iterator<JsonNode> it = currenteComponents.iterator();
						while (it.hasNext()) {
							JsonNode currentComp = it.next();
							if(storedKey.equals(currentComp.get("key").asString("").toLowerCase()) && currentComp.at("/config/addr").equals(storedComp.at("/config/addr"))) {
								exists = true;
								break;
							}
						}
						if(exists == false) {
							res.put(RestoreMsg.WARN_RESTORE_BTHOME, null);
							return;
						}
					}
				}
			}
		} catch (/*IO*/Exception e) { // beta version -> possible errors on firmware updates
			LOG.error("DynamicComponents.restoreCheck", e);
		}
	}

	// All components will keep stored IDs
	public static void restore(AbstractG2Device parent, Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		try {
			final JsonNode storedComponents = backupJsons.get("Shelly.GetComponents.json");
			if(storedComponents != null) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				final List<String> existingBTHDevices = deleteAll(parent);
				final List<String> existingKeys = new ArrayList<>();
				final List<GroupValue> groupsValues = new ArrayList<>();
				final Iterator<JsonNode> storedIt = storedComponents.path("components").iterator();
				while (storedIt.hasNext()) {
					JsonNode storedComp = storedIt.next();
					String key = storedComp.get("key").asString("");
					String typeIdx[] = key.split(":");
					if(typeIdx.length == 2 && Arrays.stream(VIRTUAL_TYPES).anyMatch(typeIdx[0]::equals/*IgnoreCase*/)) { // add virtual component
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						errors.add(createVirtual(parent, typeIdx[0], Integer.parseInt(typeIdx[1]), (ObjectNode)storedComp.path("config")));
						existingKeys.add(key);
						JsonNode value; // groups values are restored later
						if(typeIdx[0].equals/*IgnoreCase*/(GROUP_TYPE) && (value = storedComp.at("/status/value")) != null && value.size() > 0) {
							groupsValues.add(new GroupValue(Integer.parseInt(typeIdx[1]), (ArrayNode)value));
						}
					} else if(typeIdx.length == 2 && typeIdx[0].equals/*IgnoreCase*/(BTHOME_SENSOR) && existingBTHDevices.contains(storedComp.at("/config/addr").asString(""))) { // add BTHome sensor
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						errors.add(addBTHomeSensor(parent, Integer.parseInt(typeIdx[1]), (ObjectNode)storedComp.path("config")));
						existingKeys.add(key);
					} else if(typeIdx.length == 2 && typeIdx[0].equals/*IgnoreCase*/(BTHOME_DEVICE) && existingBTHDevices.contains(storedComp.at("/config/addr").asString(""))) { // add BTHome device
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						errors.add(configBTHomeDevice(parent, Integer.parseInt(typeIdx[1]), (ObjectNode)storedComp.path("config")));
						existingKeys.add(key);
					} else if(typeIdx.length == 2 && typeIdx[0].equals/*IgnoreCase*/(LNM_COMPONENT)) { // add Local Network Messaging (LNM)
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						errors.add(createLNM(parent, Integer.parseInt(typeIdx[1]), (ObjectNode)storedComp.path("config")));
					}
				}
				// group values after all components have been added
				for(GroupValue val: groupsValues) {
					ObjectNode grValue = JsonNodeFactory.instance.objectNode();
					groupRestoreValues(val.value, existingKeys); // alter val.value
					grValue.put("id", val.groupId);
					grValue.set("value", val.value);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(parent.postCommand("Group.Set", grValue));
				}
			}
		} catch (/*IO*/Exception e) { // beta version -> possible errors on firmware updates
			LOG.error("DynamicComponents.restore", e);
		}
	}
	
	private static String createVirtual(AbstractG2Device parent, String type, int id, ObjectNode config) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("type", type);
		out.put("id", id); // keep old id
		config.remove("id");
		out.set("config", config);
		return parent.postCommand("Virtual.Add", out);
	}
	
	private static String addBTHomeSensor(AbstractG2Device parent, int id, ObjectNode config) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", id); // keep old id
		config.remove("id");
		out.set("config", config);
		return parent.postCommand("BTHome.AddSensor", out);
	}
	
	private static String configBTHomeDevice(AbstractG2Device parent, int id, ObjectNode config) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", id); // keep old id
		config.remove("id");
		config.remove("addr");
		out.set("config", config);
		return parent.postCommand("BTHomeDevice.SetConfig", out);
	}
	
	// create + config -> rpc/LNM.Create?config={"addr":"239.255.0.1:3333","rpc_enable":false,"tx":{"enable":true,"components":["input:0"]},"rx":{"enable":false}}
	// do not accept "components":["input:0"] (fw 2.0.0)
	private static String createLNM(AbstractG2Device parent, int id, ObjectNode config) throws InterruptedException {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", id); // keep old id
		ObjectNode createNode = config.deepCopy().retain("addr");
		out.set("config", createNode);
		String resp = parent.postCommand("LNM.Create", out);
		if(resp == null) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			config.without("id").without("addr");
			out.set("config", config);
			resp = parent.postCommand("LNM.SetConfig", out);
		}
		return resp;
	}
	
	// remove non existing components from orig
	private static void groupRestoreValues(ArrayNode orig, List<String> existing) {
		Iterator<JsonNode> origIterator = orig.iterator();
		while(origIterator.hasNext()) {
			String val = origIterator.next().asString("");
			if(existing.contains(val) == false) {
				origIterator.remove();
			}
		}
	}
	
	private record GroupValue(Integer groupId, ArrayNode value) {}
}