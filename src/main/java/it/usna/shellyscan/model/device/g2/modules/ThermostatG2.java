package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.Thermostat;

public class ThermostatG2 implements Thermostat {
	private final AbstractG2Device parent;
	private String name;
	private boolean enabled;
	private float targhet;
	
	public ThermostatG2(AbstractG2Device parent) {
		this.parent = parent;
	}
	
	public void fillSettings(JsonNode thermostat) {
		name = thermostat.get("name").asText("");
		enabled = thermostat.get("enable").booleanValue();
		targhet = thermostat.get("target_C").floatValue();
	}
	
	public void fillStatus(JsonNode thermostat) throws IOException {
		
	}

	@Override
	public String getLabel() {
		return name;
	}

	@Override
	public float getTargetTemp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setTargetTemp(float temp) throws IOException {
		// TODO Auto-generated method stub
	}
}
