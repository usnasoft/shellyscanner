package it.usna.shellyscan.model.device.g3.modules;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g3.AbstractG3Device;

public class LoRaAddOn {
//	private final static Logger LOG = LoggerFactory.getLogger(LoRaAddOn.class);
//	public final static String BACKUP_SECTION = "LoRa.GetConfig.json";
	public final static String ADDON_TYPE = "LoRa";
	private final static int ID = 100;
	
	public static String enable(AbstractG3Device d, boolean enable) {
		return d.postCommand("Sys.SetConfig", "{\"config\":{\"device\":{\"addon_type\":" + (enable ? "\"" + ADDON_TYPE + "\"" : "null") + "}}}");
	}

	public static String[] getInfoRequests(String [] cmd) {
		int lgt = cmd.length;
		String[] newArray = Arrays.copyOf(cmd, lgt + 2);
		newArray[lgt] = "/rpc/AddOn.GetInfo";
		return newArray;
	}
	
	public static void restoreCheck(AbstractG3Device d, boolean addOn, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		boolean backupAddOn = backupJsons.get("Shelly.GetConfig.json").hasNonNull("lora:" + ID);
		if(backupAddOn && addOn == false) {
			res.put(RestoreMsg.WARN_RESTORE_LORA_ENABLE, null);
		}
	}
	
	public static void restore(AbstractG3Device d, boolean addOn, JsonNode configuration, List<String> errors) throws InterruptedException {
		boolean backupAddOn = configuration.hasNonNull("lora:" + ID);
		if(backupAddOn) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			if(addOn == false) {
				errors.add(enable(d, true));
			} else {
				errors.add(d.postCommand("LoRa.SetConfig", AbstractG2Device.createIndexedRestoreNode(configuration, "lora", ID)));
			}
		}
	}
}