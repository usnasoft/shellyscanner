package it.usna.shellyscan.model.device.g2.meters;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class MetersWVIpf extends Meters {
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Type.W, Type.PF, Type.V, Type.I};
	private float power;
	private float voltage;
	private float current;
	private float pf;
	
	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}
	
	public void fill(JsonNode status) {
		power = status.path("apower").floatValue();
		voltage = status.path("voltage").floatValue();
		current = status.path("current").floatValue();
		pf = status.get("pf").floatValue();
	}
	
	@Override
	public float getValue(Type t) {
		if(t == Type.W) {
			return power;
		} else if(t == Type.I) {
			return current;
		} else if(t == Type.PF) {
			return pf;
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
	
	public float gePF() {
		return pf;
	}
	
	@Override
	public String toString() {
		return Type.W + "=" + NF2.format(power) + " " + Type.PF + "=" + NF2.format(pf) + " " + Type.V + "=" + NF2.format(voltage) + " " + Type.I + "=" + NF2.format(current);
	}
}