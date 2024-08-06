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
public class DynamicComponents {
	private final static Logger LOG = LoggerFactory.getLogger(DynamicComponents.class);
	
	private final static String[] VIRTUAL_TYPES = {"Boolean", "Number", "Text", "Enum", "Group", "Button"};
	private final static String BTHOME_DEVICE = "BTHomeDevice";
	private final static String BTHOME_SENSOR = "BTHomeSensor";
	
//	/**
//	 * @param components - result of <IP>/rpc/Shelly.GetComponents?dynamic_only=true
//	 */
//	public DynamicComponents(JsonNode components) {
//	}
//	
//	public DynamicComponents(AbstractG2Device parent) throws IOException {
//	}

	/**
	 * Remove all dynamic components except BTHomeDevice(s).<br>
	 * Note: if a component is removed and it is grouped it is also removed from its group  
	 * @return the List<String> of not removed dynamic components (usually bthome components).
	 */
	private static List<String> deleteAll(AbstractG2Device parent) throws IOException, InterruptedException {
		final List<String> devicesAddress = new ArrayList<>();
		final JsonNode currenteComponents = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
		final Iterator<JsonNode> compIt = currenteComponents.iterator();
		while (compIt.hasNext()) {
			JsonNode comp = compIt.next();
			String key = comp.get("key").asText();
			if(Arrays.stream(VIRTUAL_TYPES).anyMatch(type -> key.toLowerCase().startsWith(type.toLowerCase() + ":"))) { // VIRTUAL_TYPES
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				parent.postCommand("Virtual.Delete", "{\"key\":\"" + key + "\"}");
			} else if(key.toLowerCase().startsWith("bthomesensor" + ":")) { // BTHomeSensor
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				String typeIdx[] = key.split(":");
				parent.postCommand("BTHome.DeleteSensor", "{\"id\":" + typeIdx[1] + "}");
			} else { // BTHomeDevice
				devicesAddress.add(comp.at("/config/addr").asText());
			}
		}
		return devicesAddress;
	}
	
	public static void restoreCheck(AbstractG2Device parent, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		try {
			JsonNode storedComponentsFile = backupJsons.get("Shelly.GetComponents.json");
			JsonNode storedComponents;
			if(storedComponentsFile != null && (storedComponents = storedComponentsFile.get("components")).size() > 0) {
				JsonNode currenteComponents = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");

				// BTHomeDevice -> stored ones are already installed on the device?
				Iterator<JsonNode> storedIt = storedComponents.iterator();
				while (storedIt.hasNext()) {
					JsonNode storedComp = storedIt.next();
					String storedKey = storedComp.get("key").asText().toLowerCase();
					if(storedKey.startsWith("bthomedevice:")) {
						boolean exists = false;
						Iterator<JsonNode> it = currenteComponents.iterator();
						while (it.hasNext()) {
							JsonNode currentComp = it.next();
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
			}
		} catch (/*IO*/Exception e) { // beta version -> possible errors on firmware updates
			LOG.error("DynamicComponents.restoreCheck", e);
		}
	}

	public static void restore(AbstractG2Device parent, Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		try {
			final JsonNode storedComponents = backupJsons.get("Shelly.GetComponents.json");
			if(storedComponents != null) {
				final List<String> existingKeys = new ArrayList<>();
				final List<String> existingDevices = deleteAll(parent);
				final List<GroupValue> groupsValues = new ArrayList<>();
				final Iterator<JsonNode> storedIt = storedComponents.path("components").iterator();
				while (storedIt.hasNext()) {
					JsonNode storedComp = storedIt.next();
					String key = storedComp.get("key").asText();
					String typeIdx[] = key.split(":");
					if(typeIdx.length == 2 && Arrays.stream(VIRTUAL_TYPES).anyMatch(typeIdx[0]::equalsIgnoreCase)) { // add virtual component
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						ObjectNode out = JsonNodeFactory.instance.objectNode();
						out.put("type", typeIdx[0]);
						out.put("id", Integer.parseInt(typeIdx[1]));
						ObjectNode config = (ObjectNode)storedComp.path("config").deepCopy();
						config.remove("id");
						out.set("config", config);
						errors.add(parent.postCommand("Virtual.Add", out));
						existingKeys.add(key);

						JsonNode value; // groups values are restored later
						if(typeIdx[0].equalsIgnoreCase("Group") && (value = storedComp.path("status").get("value")) != null && value.size() > 0) {
							groupsValues.add(new GroupValue(Integer.parseInt(typeIdx[1]), (ArrayNode)value));
						}
					} else if(typeIdx.length == 2 && typeIdx[0].equalsIgnoreCase(BTHOME_SENSOR) && existingDevices.contains(storedComp.at("/config/addr").asText())) { // add BTHome sensor
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						ObjectNode out = JsonNodeFactory.instance.objectNode();
						out.set("config", storedComp.path("config"));
						errors.add(parent.postCommand("BTHome.AddSensor", out));
						existingKeys.add(key);
					} else if(typeIdx.length == 2 && typeIdx[0].equalsIgnoreCase(BTHOME_DEVICE) && existingDevices.contains(storedComp.at("/config/addr").asText())) { // add BTHome device
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						ObjectNode out = JsonNodeFactory.instance.objectNode();
						out.put("id", Integer.parseInt(typeIdx[1]));
						ObjectNode config = (ObjectNode)storedComp.path("config").deepCopy();
						config.remove("id");
						config.remove("addr");
						out.set("config", config);
						errors.add(parent.postCommand("BTHomeDevice.SetConfig", out));
						existingKeys.add(key);
					}
				}
				// group values after all components have been added
				for(GroupValue val: groupsValues) {
					ObjectNode grValue = JsonNodeFactory.instance.objectNode();
					groupRestoreValues(val.value, existingKeys);
					grValue.put("id", val.groupId);
					grValue.set("value", val.value); // todo only values included in existingKeys
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(parent.postCommand("Group.Set", grValue));
				}
			}
		} catch (/*IO*/Exception e) { // beta version -> possible errors on firmware updates
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