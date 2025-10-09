package it.usna.shellyscan.model.device.g2.meters;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.modules.EMManager;
import it.usna.shellyscan.model.device.meters.EMHolder;
import it.usna.shellyscan.model.device.meters.Meters;

/**
 * EM 3phase model (total)
 */
public class EMTotalMeters extends Meters implements EMHolder {
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.VA, Meters.Type.I};
	private float power;
	private float apparent;
	private float current;
	private EMManager em;
	
	public EMTotalMeters(EMManager em) {
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
		} else if(t == Type.VA) {
			return apparent;
		} else { //if(t == Type.I)
			return current;
		}
	}
	
	@Override
	public EMManager getEM() {
		return em;
	}
	
	public void fillStatus(JsonNode emStatus) {
		power = emStatus.path("total_act_power").floatValue();
		apparent = emStatus.path("total_aprt_power").floatValue();
		current = emStatus.path("total_current").floatValue();
	}
	
	@Override
	public String toString() {
		return Type.W + "=" + power + " " + Type.VA + "=" + apparent + " " + Type.I + "=" + current;
	}
}