package it.usna.shellyscan.model.device.g4.meters;

import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.g2.modules.EM1Manager;
import it.usna.shellyscan.model.device.meters.EMHolder;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;

public class EM1MiniMeters extends Meters implements LabelHolder, EMHolder {
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.V, Meters.Type.I, Meters.Type.FREQ};
	private String label;
	private float power;
	private float voltage;
	private float current;
	private float freq;
	private EM1Manager em;
		
	public EM1MiniMeters(EM1Manager em) {
		this.em = em;
	}
	
	
	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}

	@Override
	public float getValue(Type t) {
		if(t == Type.W) {
			return power;
		} else if(t == Type.I) {
			return current;
		} else if(t == Type.FREQ) {
			return freq;
		} else {
			return voltage;
		}
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public EM1Manager getEM() {
		return em;
	}
	
	public void fillSettings(JsonNode em1Configuration) {
		label = em1Configuration.get("name").asString("");
	}
	
	public void fillStatus(JsonNode em1Status) {
		power = em1Status.get("act_power").floatValue();
		current = em1Status.get("current").floatValue();
		voltage = em1Status.get("voltage").floatValue();
		freq= em1Status.get("freq").floatValue();
	}
	
	@Override
	public String toString() {
		return label + ": " + Type.W + "=" + power + " " + Type.I + "=" + current + " " + Type.V + "=" + voltage;
	}
}
