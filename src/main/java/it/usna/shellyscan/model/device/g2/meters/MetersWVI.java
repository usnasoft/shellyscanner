package it.usna.shellyscan.model.device.g2.meters;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class MetersWVI extends Meters {
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.V, Meters.Type.I};
	private float power;
	private float voltage;
	private float current;
	
	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}
	
	public void fill(JsonNode status) {
		power = status.path("apower").floatValue();
		voltage = status.path("voltage").floatValue();
		current = status.path("current").floatValue();
	}
	
	@Override
	public float getValue(Type t) {
		if(t == Meters.Type.W) {
			return power;
		} else if(t == Meters.Type.I) {
			return current;
		} else {
			return voltage;
		}
	}
	
	public float getPower() {
		return power;
	}
	
	public float getVoltage() {
		return voltage;
	}
	
	public float getCurrent() {
		return current;
	}
	
	@Override
	public String toString() {
		return Type.W + "=" + NF.format(power) + " " + Type.V + "=" + NF.format(voltage) + " " + Type.I + "=" + NF.format(current);
	}
}