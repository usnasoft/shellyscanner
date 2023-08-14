package it.usna.shellyscan.model.device;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * This interface identify battery operated Shelly devices
 * @author usna
 */
public interface BatteryDeviceInterface {
	
	int getBattery();
	
	JsonNode getStoredJSON(final String command);
	
	void setStoredJSON(final String command, JsonNode val);
}
