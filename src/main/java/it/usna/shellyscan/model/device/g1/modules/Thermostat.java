package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.DeviceModule;

public class Thermostat implements DeviceModule /*, ThermostatInterface*/ {
	private final AbstractG1Device parent;
	private final static String INDEX = "0";
	private float target; // Target temperature 4..31
	private int position;
	private boolean schedule;
	private int scheduleProfile; //  1..5
	private String[] profileNames = new String[5];
	public final static float TARGET_MIN = 4;
	public final static float TARGET_MAX = 31;
	
	public Thermostat(AbstractG1Device parent) {
		this.parent = parent;
	}
	
	public void fillSettings(JsonNode thermostat) {
		JsonNode profiles = thermostat.get("schedule_profile_names");
		for(int i = 0; i < profiles.size(); i++) {
			profileNames[i] = profiles.get(i).asText();
		}
	}
	
	public void fillStatus(JsonNode thermostat) throws IOException {
		target = (float)thermostat.get("target_t").get("value").asDouble();
		position = thermostat.get("pos").intValue();
		schedule = thermostat.get("schedule").asBoolean();
		scheduleProfile = thermostat.path("schedule_profile").asInt();
	}
	
//	@Override
	public float getTargetTemp() {
		return target;
	}
	
	public float getPosition() {
		return position;
	}
	
	public boolean isScheduleActive() {
		return schedule;
	}
	
	public String getCurrentProfile() {
		return profileNames[scheduleProfile - 1];
//		return "test";
	}

	@Override
	public String getLabel() {
		return getCurrentProfile();
	}

	public int getCurrentProfileIndex() {
		return scheduleProfile;
	}

//	@Override
	public void setTargetTemp(float temp) throws IOException {
		/*final JsonNode t =*/ parent.getJSON("/settings/thermostats/" + INDEX + "?target_t=" +  temp);
		target = temp; // on error target is not changed
	}
	
	public void targetTempUp(float delta) throws IOException {
		setTargetTemp(Math.min(TARGET_MAX, target + delta));
	}
	
	public void targetTempDown(float delta) throws IOException {
		setTargetTemp(Math.max(TARGET_MIN, target - delta));
	}
	
//	public String restore(JsonNode data) throws IOException {
//		return null;
//	}
	
	@Override
	public String toString() {
		return "Prof:" + getCurrentProfile() + "; Temp:" + target;
	}
}