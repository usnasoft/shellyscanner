package it.usna.shellyscan.model.device.g3.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g3.AbstractG3Device;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;

public class XT1Thermostat implements ThermostatInterface {
	private final AbstractG3Device parent;
	private boolean enabled;
	private float targetTemp;
	private float minTarget;
	private float maxTarget;
	private final String enabledID;
	private final String targetId;
	private boolean celsius;
	
	public XT1Thermostat(AbstractG3Device parent, String enabledID, String targetId) {
		this.parent = parent;
		this.enabledID = enabledID;
		this.targetId = targetId;
	}
	
	public void configTargetTemperature(JsonNode sensor) {
		JsonNode config = sensor.path("config");
		celsius = "°C".equals(config.path("meta").path("ui").path("unit").textValue());
		if(celsius) {
			targetTemp = sensor.path("status").path("value").floatValue();
			minTarget = config.path("min").floatValue();
			maxTarget = config.path("max").floatValue();
		} else {
			targetTemp = Math.round((sensor.path("status").path("value").floatValue() - 32f) * (5f / 9f * 10f)) / 10f;//float val =Math.round(thermostat.getTargetTemp() * 10f) / 10f;
			minTarget = Math.round((config.path("min").floatValue() - 32f) * (5f / 9f * 10f)) / 10f;
			maxTarget = Math.round((config.path("max").floatValue() - 32f) * (5f / 9f * 10f)) / 10f;
		}
	}
	
	public void configEnabled(JsonNode sensor) {
		enabled = sensor.path("status").path("value").booleanValue();
	}

	@Override
	public String getLabel() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isRunning() {
		return false; // no info
	}

	@Override
	public void setEnabled(boolean enabled) throws IOException {
		String res = parent.postCommand("Boolean.Set", "{\"id\":" + enabledID + ",\"value\":" + enabled + "}");
		if(res == null) {
			this.enabled = enabled;
		} else {
			throw new IOException(res);
		}
	}

	@Override
	public float getTargetTemp() {
		return targetTemp;
	}

	@Override
	public void setTargetTemp(float temp) throws IOException {
		final String res;
		if(celsius) {
			res = parent.postCommand("Number.Set", "{\"id\":" + targetId + ",\"value\":" + temp + "}");
		} else {
			res = parent.postCommand("Number.Set", "{\"id\":" + targetId + ",\"value\":" + (Math.round(temp * 18f + 320f) / 10f) + "}");
//			res = parent.postCommand("Number.Set", "{\"id\":" + targetId + ",\"value\":" + (Math.round((temp * 1.8f + 32f) * 10f) / 10f) + "}");
		}
		if(res == null) {
			targetTemp = temp;
		} else {
			throw new IOException(res);
		}
	}

	@Override
	public float getMaxTargetTemp() {
		return maxTarget;
	}

	@Override
	public float getMinTargetTemp() {
		return minTarget;
	}
	
	@Override
	public String toString() {
		return "Target temp: " + targetTemp /*+ (running ? " (on)" : " (off)")*/;
	}
}