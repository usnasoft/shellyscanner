package it.usna.shellyscan.model.device.meters;

import it.usna.shellyscan.model.device.Meters;

public abstract class MetersWVI extends Meters {
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.V, Meters.Type.I};
	
	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}
	
	@Override
	public String toString() {
		return Type.W + "=" + NF.format(getValue(Type.W)) + " " + Type.V + "=" + NF.format(getValue(Type.V)) + " " + Type.I + "=" + NF.format(getValue(Type.I));
	}
}