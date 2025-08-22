package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;

/* TRV only, I will not continue develop this class */
public class ThermostatG1 implements ThermostatInterface {
	private final AbstractG1Device parent;
	private static final String INDEX = "0";
	private boolean autoTemp;
	private float targetTemp; // Target temperature 4..31
	private float position;
	private boolean schedule;
	private int scheduleProfile; //  1..5
	private String[] profileNames = new String[5];
	public static final float TARGET_MIN = 4;
	public static final float TARGET_MAX = 31;
	
	public ThermostatG1(AbstractG1Device parent) {
		this.parent = parent;
	}
	
	@Override
	public float getMaxTargetTemp() {
		return TARGET_MAX;
	}
	
	@Override
	public float getMinTargetTemp() {
		return TARGET_MIN;
	}
	
	public void fillSettings(JsonNode thermostat) {
		JsonNode profiles = thermostat.get("schedule_profile_names");
		for(int i = 0; i < profiles.size(); i++) {
			profileNames[i] = profiles.get(i).asText();
		}
		autoTemp = thermostat.get("t_auto").get("enabled").asBoolean();
	}
	
	public void fillStatus(JsonNode thermostat) throws IOException {
		position = (float)thermostat.get("pos").asDouble();
		fillThermostat(thermostat);
	}
	
	@Override
	public boolean isEnabled() {
		return autoTemp;
	}
	
	@Override
	public void setEnabled(boolean enable) {
		// todo not user - TRV only, I will not continue develop this class
	}
	
	@Override
	public boolean isRunning() {
		return position > 0;
	}
	
	@Override
	public float getTargetTemp() {
		return targetTemp;
	}
	
	public float getPosition() {
		return position;
	}
	
	public boolean isScheduleActive() {
		return schedule;
	}
	
	public String getCurrentProfile() {
		try {
			return profileNames[scheduleProfile - 1];
		} catch(RuntimeException e) {
			return "";
		}
	}

	@Override
	public String getLabel() {
		return getCurrentProfile();
	}

	public int getCurrentProfileIndex() {
		return scheduleProfile;
	}

	@Override
	public void setTargetTemp(float temp) throws IOException {
		fillThermostat(parent.getJSON("/settings/thermostats/" + INDEX + "?target_t=" + temp));
	}
	
	public void targetTempUp(float delta) throws IOException {
		setTargetTemp(Math.min(TARGET_MAX, targetTemp + delta));
	}
	
	public void targetTempDown(float delta) throws IOException {
		setTargetTemp(Math.max(TARGET_MIN, targetTemp - delta));
	}
	
	private void fillThermostat(JsonNode thermostat) {
		targetTemp = (float)thermostat.get("target_t").get("value").asDouble();
//		position = thermostat.get("pos").intValue();
		schedule = thermostat.get("schedule").asBoolean();
		scheduleProfile = thermostat.get("schedule_profile").asInt();
	}
	
	public String restore(JsonNode data) throws IOException {
		return parent.sendCommand("/settings/thermostats/" + INDEX + "?" +
				AbstractG1Device.jsonNodeToURLPar(data, "temperature_offset") +
				"&ext_t_enabled=" + data.get("ext_t").get("enabled").asBoolean());
	}

	@Override
	public String toString() {
		return "Prof:" + getCurrentProfile() + "; Temp:" + targetTemp;
	}
}

/*
.../settings/thermostats/0?target_t=20
{
"target_t":{"enabled":true,"value":20.0,"value_op":8.0,"units":"C","accelerated_heating":true},
"schedule":true,
"schedule_profile":1,
"schedule_profile_names"🙁"Livingroom","Livingroom 1","Bedroom","Bedroom 1","Holiday"],"schedule_rules"🙁"0700-0123456-19","0910-0123456-16","1105-0123456-25","1230-0123456-19","2030-0123456-16"],
"temperature_offset":-1.0,"ext_t":{"enabled":false, "floor_heating": false},"t_auto":{"enabled":true},
"boost_minutes":30,"valve_min_percent":0.00,"force_close":false,"calibration_correction":true,"extra_pressure":false,"open_window_report":false}
*/