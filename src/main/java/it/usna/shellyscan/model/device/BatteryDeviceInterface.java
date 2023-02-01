package it.usna.shellyscan.model.device;

import com.fasterxml.jackson.databind.JsonNode;

public interface BatteryDeviceInterface {
	int getBattery();
	public JsonNode getStoredJSON(final String command);
	public void setStoredJSON(final String command, JsonNode val);
}
