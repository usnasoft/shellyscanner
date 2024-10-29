package it.usna.shellyscan.model.device;

import java.text.NumberFormat;
import java.util.Locale;

public abstract class Meters implements Comparable<Meters> {
	public enum Type {
		W, // active power
		VA, // apparent power
		VAR, // reactive power
		PF, // power factor
//		PF1, // power factor (1 decimal precision)
		V, // voltage
		I, // current
		FREQ, // Frequency
		T, // temperature (celsius)
		H, // humidity %
		L, // lux
		T1, // temperature (celsius)
		T2, // temperature (celsius)
		T3, // temperature (celsius)
		T4, // temperature (celsius)
		EX, // ext switch status
		PERC, // 0-100
		NUM, // inetger
		BAT // battery %
	};
	protected static NumberFormat NF = NumberFormat.getNumberInstance(Locale.ENGLISH);
	static {
		NF.setMaximumFractionDigits(2);
		NF.setMinimumFractionDigits(2);
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
			if((name = getName(type)) != null && name.isEmpty() == false) return true;
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
			StringBuilder res = new StringBuilder(t[0].toString());
			res.append("=").append(NF.format(getValue(t[0])));
			for(int i = 1; i < t.length; i++) {
				res.append(" ").append(t[i].toString()).append("=").append(NF.format(getValue(t[i])));
			}
			return res.toString();
		} else {
			return "";
		}
	}
}