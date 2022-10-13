package it.usna.shellyscan.model.device;

public abstract class MetersPower extends Meters {
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W};
	
	@Override
	public Type[] getTypes() {
		return SUPPORTED_MEASURES;
	}
	
	@Override
	public String toString() {
		return Type.W + "=" + NF.format(getValue(null));
	}
}