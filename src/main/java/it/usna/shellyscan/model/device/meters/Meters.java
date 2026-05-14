package it.usna.shellyscan.model.device.meters;

import java.text.NumberFormat;
import java.util.Locale;

public abstract class Meters implements Comparable<Meters> {
	public enum Type {
		W, // active power
		VA, // apparent power
		VAR, // reactive power
		PF, // power factor
		V, // voltage
		VL, // voltage - low (addon / UNI)
		VX, // voltage - custom expression (sensor addon)
		I, // current
		FREQ, // Frequency
		T(Float.MAX_VALUE), // temperature
		H, // humidity % (int)
		HD, // humidity % (one decimal)
		L, // lux
		LE(true), // lux(enum) 0 -> dark, 1 -> twilight, 2 -> bright
		LIGHT(true), // boolean: 0/false -> no light, 1/true -> light
		T1(Float.MAX_VALUE), // temperature
		T2(Float.MAX_VALUE), // temperature
		T3(Float.MAX_VALUE), // temperature
		T4(Float.MAX_VALUE), // temperature
		EX(true), // boolean: ext switch status
		PERC, // 0-100
		NUM, // integer - UNI counter
		DMM, // distance [mm]
		RAIN, // precipitation [mm]
		VIB(true), // boolean: vibration - 0=false; 1=true
		ANG(Float.MAX_VALUE), // angle - accelerometer
		ANG1(Float.MAX_VALUE), // angle - accelerometer
		ANG2(Float.MAX_VALUE), // angle - accelerometer
		CHANNEL(Float.MAX_VALUE), // channel - BLU remore channel
		BAT, // battery %
		BATE(true); // battery boolean - 0 (False = Normal), 1 (True = Low)
		
		final boolean enumType; // the value should be translated as a specific status
		final float nullValue; // the value considered as "off" (default: 0)
		
		Type() {
			enumType = false;
			nullValue = 0;
		}
		
		Type(boolean b) {
			enumType = b;
			nullValue = 0;
		}
		
		Type(float disabledVal) {
			enumType = false;
			this.nullValue = disabledVal;
		}
		
		public boolean isEnumType() {
			return enumType;
		}
		
		public float getNullValue() {
			return nullValue;
		}
	};

	protected static NumberFormat NF1 = NumberFormat.getNumberInstance(Locale.ENGLISH);
	protected static NumberFormat NF2 = NumberFormat.getNumberInstance(Locale.ENGLISH);
	static {
		NF1.setMaximumFractionDigits(1);
		NF1.setMinimumFractionDigits(1);
		NF2.setMaximumFractionDigits(2);
		NF2.setMinimumFractionDigits(2);
	}
	
	public abstract float getValue(Type t);
	
	public abstract Type[] getTypes();
	
	public boolean hasType(Type t) {
		for(Type type: getTypes()) {
			if(type == t) return true;
		}
		return false;
	}

	/**
	 * Override for named measures
	 */
	public String getName(Type t) {
		return null;
	}
	
	public boolean hasNames() {
		String name;
		for(Type type: getTypes()) {
			if((name = getName(type)) != null && name.isEmpty() == false) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int compareTo(Meters o) {
		final Type t0 = getTypes()[0];
		int v = t0.name().compareTo(o.getTypes()[0].name());
		if(v == 0) {
			return Float.compare(getValue(t0), o.getValue(t0));
		}
		return v;
	}

	@Override
	public String toString() {
		Type[] t = getTypes();
		if(t.length > 0) {
			String tName = t[0].toString();
			if(tName.length() > 2) {
				tName = tName.substring(0, 2);
			}
			StringBuilder res = new StringBuilder(tName);
			res.append('=').append(NF1.format(getValue(t[0])));
			for(int i = 1; i < t.length; i++) {
				tName = t[i].toString();
				if(tName.length() > 2) {
					tName = tName.substring(0, 2);
				}
				res.append(' ').append(tName).append('=').append(NF1.format(getValue(t[i])));
			}
			return res.toString();
		} else {
			return "";
		}
	}
}