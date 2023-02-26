package it.usna.shellyscan.model.device;

import com.fasterxml.jackson.databind.JsonNode;

public interface BatteryDeviceInterface {
	
	int getBattery();
	
	JsonNode getStoredJSON(final String command);
	
	void setStoredJSON(final String command, JsonNode val);
}
