package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;

public class Thermostat implements ThermostatInterface {
	private final AbstractG1Device parent;
	private final static String INDEX = "0";
	private float target; // Target temperature 4..31
	private int position;
	
	public Thermostat(AbstractG1Device parent) {
		this.parent = parent;
	}
	
	public void fillStatus(JsonNode thermostat) throws IOException {
		target = (float)thermostat.get("target_t").get("value").asDouble();
		position = thermostat.path("pos").intValue();
	}
	
	@Override
	public float getTargetTemp() {
		return target;
	}
	
	public float getPosition() {
		return position;
	}

	@Override
	public void setTargetTemp(float temp) throws IOException {
		/*final JsonNode t =*/ parent.getJSON("/settings/thermostats/" + INDEX + "?target_t=" + temp);
	}
	
//	public String restore(JsonNode data) throws IOException {
//		return null;
//	}
}