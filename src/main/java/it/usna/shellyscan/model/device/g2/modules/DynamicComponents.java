package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class DynamicComponents {
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
	}
	
	public static void restoreCheck(AbstractG2Device d, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		JsonNode virtualComponents = backupJsons.get("Shelly.GetComponents.json");
		if(virtualComponents != null && virtualComponents.path("components").size() > 0) {
			res.put(RestoreMsg.WARN_RESTORE_VIRTUAL, null);
		}
	}
	
	public static void restore(AbstractG2Device d, Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		
	}
	
}

// ref: https://shelly-api-docs.shelly.cloud/gen2/DynamicComponents/
// IDs for these components start from 200 and are limited to 299

/* 
restore VIRTUAL_TYPES solo se non ci sono componenti virtuali già inclusi
aggiungere Group(s) solo al termine

restore BTHOME_TYPES solo se non ci sono componenti BTHome già inclusi
nei gruppi possono anche esserci BTHome ?
*/