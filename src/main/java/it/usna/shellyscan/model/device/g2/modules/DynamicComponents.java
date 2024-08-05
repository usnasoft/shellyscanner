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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

/**
 * Reference: https://shelly-api-docs.shelly.cloud/gen2/DynamicComponents/
 * <br>
 * IDs for these components start from 200 and are limited to 299.
 */
//TODO restore BTHOME_TYPES configuration when same device is installed
public class DynamicComponents {
	private final static Logger LOG = LoggerFactory.getLogger(DynamicComponents.class);
	
	private final static String[] VIRTUAL_TYPES = {"Boolean", "Number", "Text", "Enum", "Group", "Button"};
//	private final static String[] BTHOME_TYPES = {"BTHomeDevice", "BTHomeSensor"};
	
//	/**
//	 * @param components - result of <IP>/rpc/Shelly.GetComponents?dynamic_only=true
//	 */
//	public DynamicComponents(JsonNode components) {
//		components.path("components");
//	}
//	
//	public DynamicComponents(AbstractG2Device parent) throws IOException {
//		JsonNode components = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
////		for(int i= 0; i < components.size(); i++) {
////			
////		}
//	}

	/**
	 * Remove all virtual components.<br>
	 * Note: if a component is removed and it is grouped it is also removed from its group  
	 * @return the List<String> of not removed dynamic components (usually bthome components).
	 */
	private static List<String> deleteAllVirtual(AbstractG2Device parent) throws IOException, InterruptedException {
		List<String> dynamicKeys = new ArrayList<>();
		JsonNode currenteComponents = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
		Iterator<JsonNode> compIt = currenteComponents.iterator();
		while (compIt.hasNext()) {
			JsonNode comp = compIt.next();
			String key = comp.get("key").asText();
			if(Arrays.stream(VIRTUAL_TYPES).anyMatch(type -> key.toLowerCase().startsWith(type.toLowerCase() + ":"))) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				parent.postCommand("Virtual.Delete", "{\"key\":\"" + key + "\"}");
			} else {
				dynamicKeys.add(key);
			}
		}
		return dynamicKeys;
	}
	
	public static void restoreCheck(AbstractG2Device d, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
//		JsonNode storedComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
//		Iterator<JsonNode> compIt = storedComponents.iterator();
//		while (compIt.hasNext()) {
//			JsonNode comp = compIt.next();
//			String key = comp.get("key").asText();
//			if(Arrays.stream(BTHOME_TYPES).anyMatch(type -> key.toLowerCase().startsWith(type.toLowerCase() + ":"))) {
//				res.put(RestoreMsg.WARN_RESTORE_BTHOME, null);
//				break;
//			}
//		}

		try {
			JsonNode currenteComponents = d.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
			JsonNode storedComponents = backupJsons.get("Shelly.GetComponents.json").path("components");

			// BTHomeDevice -> stored ones are already installed on the device?
			Iterator<JsonNode> storedIt = storedComponents.iterator();
			while (storedIt.hasNext()) {
				JsonNode storedComp = storedIt.next();
				String storedKey = storedComp.get("key").asText().toLowerCase();
				if(storedKey.startsWith("bthomedevice:")) {
					boolean exists = false;
					Iterator<JsonNode> it = currenteComponents.iterator();
					while (it.hasNext()) {
						JsonNode currentComp = storedIt.next();
						if(storedKey.equals(currentComp.get("key").asText().toLowerCase()) && currentComp.at("/config/addr").equals(storedComp.at("/config/addr"))) {
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
			
			// BTHomeSensor -> stored ones are already installed on the device?
			/*Iterator<JsonNode>*/ storedIt = storedComponents.iterator();
			while (storedIt.hasNext()) {
				JsonNode storedComp = storedIt.next();
				String storedKey = storedComp.get("key").asText().toLowerCase();
				if(storedKey.startsWith("bthomesensor:")) {
					boolean exists = false;
					Iterator<JsonNode> it = currenteComponents.iterator();
					while (it.hasNext()) {
						JsonNode currentComp = storedIt.next();
						if(storedKey.equals(currentComp.get("key").asText().toLowerCase()) && currentComp.at("/config/addr").equals(storedComp.at("/config/addr"))
								/*&& id*/) {
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
		} catch (IOException e) {
			LOG.error("DynamicComponents.restoreCheck", e);
		}
	}
	
//	private boolean exist(ArrayNode array, String val) {
//		Iterator<JsonNode> it = array.iterator();
//		while (it.hasNext()) {
//			String node = it.next().asText();
//			if(it.next().asText().equals(val)) {
//				return true;
//			}
//		}
//		return false;
//	}

	public static void restore(AbstractG2Device d, Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		try {
			List<String> existingKeys = deleteAllVirtual(d);
			
			JsonNode components = backupJsons.get("Shelly.GetComponents.json");
			if(components != null) {
				List<GroupValue> groupsValues = new ArrayList<>();
				Iterator<JsonNode> compIt = components.path("components").iterator();
				while (compIt.hasNext()) {
					JsonNode comp = compIt.next();
					String key = comp.get("key").asText();
					String typeIdx[] = key.split(":");
					if(typeIdx.length == 2 && Arrays.stream(VIRTUAL_TYPES).anyMatch(typeIdx[0]::equalsIgnoreCase)) {
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						ObjectNode out = JsonNodeFactory.instance.objectNode();
						out.put("type", typeIdx[0]);
						out.put("id", Integer.parseInt(typeIdx[1]));
						ObjectNode config = (ObjectNode)comp.path("config").deepCopy();
						config.remove("id");
						out.set("config", config);
						errors.add(d.postCommand("Virtual.Add", out));
						existingKeys.add(key);

						JsonNode value;
						if(typeIdx[0].equalsIgnoreCase("Group") && (value = comp.path("status").get("value")) != null && value.size() > 0) {
							groupsValues.add(new GroupValue(Integer.parseInt(typeIdx[1]), (ArrayNode)value));
						}
					}
				}
				// group values after all components have been added
				for(GroupValue val: groupsValues) {
					ObjectNode grValue = JsonNodeFactory.instance.objectNode();
					groupRestoreValues(val.value, existingKeys);
					grValue.put("id", val.groupId);
					grValue.set("value", val.value); // todo only values included in existingKeys
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(d.postCommand("Group.Set", grValue));
				}
			}
		} catch (IOException e) {
			LOG.error("DynamicComponents.restore", e);
		}
	}
	
	// remove non existing components from orig
	private static void groupRestoreValues(ArrayNode orig, List<String> existing) {
		Iterator<JsonNode> toRestore = orig.iterator();
		while(toRestore.hasNext()) {
			String val = toRestore.next().asText();
			if(existing.contains(val) == false) {
				toRestore.remove();
			}
		}
	}
	
	private record GroupValue(Integer groupId, ArrayNode value) {}
}