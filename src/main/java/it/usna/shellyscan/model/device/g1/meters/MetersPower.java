package it.usna.shellyscan.model.device.g1.meters;

import it.usna.shellyscan.model.device.Meters;

/**
 * For many gen 1 devices
 * SUPPORTED_MEASURES = W
 */
public abstract class MetersPower extends Meters {
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Type.W};
	
	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}
	
	@Override
	public String toString() {
		return Type.W + "=" + NF2.format(getValue(Type.W));
	}
}