package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class DynamicComponents {
//	private final static Logger LOG = LoggerFactory.getLogger(DynamicComponents.class);
	
	private final static String[] VIRTUAL_TYPES = {"Boolean", "Number", "Text", "Enum", "Group", "Button"};
	private final static String[] BTHOME_TYPES = {"BTHomeDevice", "BTHomeSensor"};
	
	/**
	 * @param components - result of <IP>/rpc/Shelly.GetComponents?dynamic_only=true
	 */
	public DynamicComponents(JsonNode components) {
		components.path("components");
	}
	
	public DynamicComponents(AbstractG2Device parent) throws IOException {
		JsonNode components = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
//		for(int i= 0; i < components.size(); i++) {
//			
//		}
	}
	
	// todo - si puo' rimuovere un component che si trova in un gruppo?
	public void deleteAllVirtual(AbstractG2Device parent) throws IOException {
		JsonNode components = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
		Iterator<JsonNode> compIt = components.path("components").iterator();
		while (compIt.hasNext()) {
			JsonNode comp = compIt.next();
			String key = comp.get("key").asText();
			if(Arrays.stream(VIRTUAL_TYPES).anyMatch(type -> key.toLowerCase().startsWith(type.toLowerCase() + ":"))) {
				parent.postCommand("Virtual.Delete", "key=\"" + key + "\"");
			}
		}
	}
	
	public static void restoreCheck(AbstractG2Device d, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		JsonNode virtualComponents = backupJsons.get("Shelly.GetComponents.json");
		if(virtualComponents != null && virtualComponents.path("components").size() > 0) {
			res.put(RestoreMsg.WARN_RESTORE_VIRTUAL, null);
		}
	}
	
	// todo si puo' fare Group.Set con componenti non ancora creati?
	//Group.Set last - solo componenti esistenti (BTHOME_TYPES)
	public static void restore(AbstractG2Device d, Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode components = backupJsons.get("Shelly.GetComponents.json");
		if(components != null) {
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
					
					JsonNode value;
					if(typeIdx[0].equalsIgnoreCase("Group") && (value = comp.path("status").get("value")) != null && value.size() > 0) {
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						ObjectNode grValue = JsonNodeFactory.instance.objectNode();
						grValue.put("id", Integer.parseInt(typeIdx[1]));
						grValue.set("value", value);
						errors.add(d.postCommand("Group.Set", grValue));
					}
				}
			}
		}
	}
}

// ref: https://shelly-api-docs.shelly.cloud/gen2/DynamicComponents/
// IDs for these components start from 200 and are limited to 299

/* 
restore VIRTUAL_TYPES solo se non ci sono componenti virtuali già inclusi
aggiungere Group(s) solo al termine

restore BTHOME_TYPES solo se non ci sono componenti BTHome già inclusi
nei gruppi possono anche esserci BTHome
*/