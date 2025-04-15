package it.usna.shellyscan.model.device.g3.modules;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g3.AbstractG3Device;

public class LoRaAddOn {
//	private final static Logger LOG = LoggerFactory.getLogger(LoRaAddOn.class);
	public final static String BACKUP_SECTION = "LoRa.GetConfig.json";
	public final static String ADDON_TYPE = "LoRa";
	public final static String ID = "100";
	
	public static String enable(AbstractG3Device d, boolean enable) {
		return d.postCommand("Sys.SetConfig", "{\"config\":{\"device\":{\"addon_type\":" + (enable ? "\"" + ADDON_TYPE + "\"" : "null") + "}}}");
	}

	public static String[] getInfoRequests(String [] cmd) {
		int lgt = cmd.length;
		String[] newArray = Arrays.copyOf(cmd, lgt + 2);
		newArray[lgt] = "/rpc/LoRa.GetConfig?id=" + ID;
		newArray[lgt + 1] = "/rpc/LoRa.GetStatus?id=" + ID;
		return newArray;
	}
	
//	public static void restoreCheck(AbstractG3Device d, boolean addOn, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
//		JsonNode backupAddOn = backupJsons.get(BACKUP_SECTION);
//		if(backupAddOn != null && addOn) {
//			
//		}
//	}
	
	public static void restore(AbstractG3Device d, boolean addOn, Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode backupAddOn = backupJsons.get(BACKUP_SECTION);
		if(backupAddOn == null && addOn) { // there is addon on the device but not on backup -> disable (must reboot)
			errors.add(enable(d, false));
		} else if(backupAddOn != null /*&& addOn == false*/) { // NO addon on the device but addon on backup -> enable (must reboot)
			String err = null;
			if(addOn == false) {
				err = enable(d, true);
			}
			if(err == null) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				// todo restore
			} else {
				errors.add(err);
			}
		} /*else if(backupAddOn != null && addOn) {
			// todo restore
		}*/
	}
	
//	public static ObjectNode createIndexedRestoreNode(JsonNode backConfig, String type, int index) { // todo addon, input, switch
//		ObjectNode out = JsonNodeFactory.instance.objectNode();
//		out.put("id", index);
//		ObjectNode data = backConfig.get(type + ":" + index).deepCopy();
//		data.remove("id");
//		out.set("config", data);
//		return out;
//	}
}
