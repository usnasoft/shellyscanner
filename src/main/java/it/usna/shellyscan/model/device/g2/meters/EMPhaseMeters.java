package it.usna.shellyscan.model.device.g2.meters;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.Meters;

/**
 * EM 3phase model (for a single phase)
 */
public class EMPhaseMeters extends Meters implements LabelHolder {
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.VA, Meters.Type.PF, Meters.Type.V, Meters.Type.I, Meters.Type.FREQ};
	private String label = "";
	private float power;
	private float apparent;
	private float voltage;
	private float current;
	private float pf;
	private float freq;
	private final String phaseId;
	
	public EMPhaseMeters(String phase) {
		this.phaseId = phase;
	}

	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}

	@Override
	public float getValue(Type t) {
		if(t == Type.W) {
			return power;
		} else if(t == Type.VA) {
			return apparent;
		} else if(t == Type.I) {
			return current;
		} else if(t == Type.PF) {
			return pf;
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
	
	public void fillSettings(JsonNode emConfiguration) {
		label = emConfiguration.path("name").asText("");
	}
	
	public void fillStatus(JsonNode emStatus) {
		power = emStatus.get(phaseId + "_act_power").floatValue();
		apparent = emStatus.get(phaseId + "_aprt_power").floatValue();
		current = emStatus.get(phaseId + "_current").floatValue();
		pf = emStatus.get(phaseId + "_pf").floatValue();
		voltage= emStatus.get(phaseId + "_voltage").floatValue();
		freq = emStatus.get(phaseId + "_freq").floatValue();
	}
	
	@Override
	public String toString() {
		return label + ": " + Type.W + "=" + power+ " " + Type.I + "=" + current + " " + Type.PF + "=" + pf + " " + Type.V + "=" + voltage;
	}
}