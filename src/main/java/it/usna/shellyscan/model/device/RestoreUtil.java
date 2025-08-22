package it.usna.shellyscan.model.device;

import it.usna.shellyscan.model.device.g3.Shelly1G3;
import it.usna.shellyscan.model.device.g3.Shelly1PMG3;
import it.usna.shellyscan.model.device.g3.ShellyMini1G3;
import it.usna.shellyscan.model.device.g3.ShellyMini1PMG3;
import it.usna.shellyscan.model.device.g4.Shelly1G4;
import it.usna.shellyscan.model.device.g4.Shelly1PMG4;
import it.usna.shellyscan.model.device.g4.Shelly2PMG4;
import it.usna.shellyscan.model.device.g4.ShellyMini1G4;
import it.usna.shellyscan.model.device.g4.ShellyMini1PMG4;

public class RestoreUtil {
	private static final String[][] COMPATIBILITY_TABLE = 
		{
				{Shelly1G3.ID, ShellyMini1G3.ID, Shelly1G4.ID, Shelly1G4.ID_ZB, ShellyMini1G4.ID, ShellyMini1G4.ID_ZB}, // 1
				{Shelly1PMG3.ID, ShellyMini1PMG3.ID, Shelly1PMG4.ID, Shelly1PMG4.ID_ZB, ShellyMini1PMG4.ID, ShellyMini1PMG4.ID_ZB}, // 1PM
				{Shelly2PMG4.ID, Shelly2PMG4.ID_ZB}, // 2PM
		};
	
	public static boolean compatibleModels(String backApp, String currentApp) {
		if(backApp.equals(currentApp)) {
			return true;
		}
		for(String[] idList: COMPATIBILITY_TABLE) {
			boolean firstFound = false;
			for(String id: idList) {
				if(id.equals(backApp) || id.equals(currentApp)) {
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
