package it.usna.shellyscan.model.device.meters;

import it.usna.shellyscan.model.device.Meters;

public abstract class MetersPower extends Meters {
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Type.W};
	
	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}
	
	@Override
	public String toString() {
		return Type.W + "=" + NF.format(getValue(Type.W));
	}
}