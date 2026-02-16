package it.usna.shellyscan.model.device;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g3.Shelly1G3;
import it.usna.shellyscan.model.device.g3.Shelly1PMG3;
import it.usna.shellyscan.model.device.g3.Shelly2PMG3;
import it.usna.shellyscan.model.device.g3.ShellyDimmerG3;
import it.usna.shellyscan.model.device.g3.ShellyMini1G3;
import it.usna.shellyscan.model.device.g3.ShellyMini1PMG3;
import it.usna.shellyscan.model.device.g4.Shelly1G4;
import it.usna.shellyscan.model.device.g4.Shelly1PMG4;
import it.usna.shellyscan.model.device.g4.Shelly2PMG4;
import it.usna.shellyscan.model.device.g4.ShellyDimmerG4;
import it.usna.shellyscan.model.device.g4.ShellyMini1G4;
import it.usna.shellyscan.model.device.g4.ShellyMini1PMG4;
import tools.jackson.databind.JsonNode;

public class RestoreUtil {

	private static final String[][] COMPATIBILITY_APP_TABLE = 
		{
				{Shelly1G3.ID, ShellyMini1G3.ID, Shelly1G4.ID, /*Shelly1G4.ID_ZB,*/ ShellyMini1G4.ID/*, ShellyMini1G4.ID_ZB*/}, // 1
				{Shelly1PMG3.ID, ShellyMini1PMG3.ID, Shelly1PMG4.ID, /*Shelly1PMG4.ID_ZB,*/ ShellyMini1PMG4.ID/*, ShellyMini1PMG4.ID_ZB*/}, // 1PM
				{Shelly2PMG3.ID, Shelly2PMG4.ID/*, Shelly2PMG4.ID_ZB*/}, // 2PM
				{ShellyDimmerG3.ID, ShellyDimmerG4.ID/*, ShellyDimmerG4.ID_ZB*/}, // Dimmer
//				{ShellyEMG3.ID},
		};

	public static boolean compatibleModels(JsonNode devInfoBack, AbstractG2Device dev) {
		String backApp = devInfoBack.get("app").asString("").replaceAll("ZB$", ""); // .replaceAll("ZB$", ""); -> remove zigbee suffix
		String devApp = dev.getTypeID();
		// getModelID() == null for all gen2 devices; moreover a variant could not be included so I prefer the RestoreUtil.compatibleModels(...) method
		if(backApp.equals(devApp) || devInfoBack.get("model").asString("").equals(dev.getModelID())) {
			return true;
		}
		for(String[] idList: COMPATIBILITY_APP_TABLE) {
			boolean firstFound = false;
			for(String id: idList) {
				if(id.equals(backApp) || id.equals(devApp)) {
					if(firstFound) {
						return true;
					} else {
						firstFound = true;
					}
				}
			}
		}
		return false;
	}
}