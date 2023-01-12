package it.usna.shellyscan.model.device;

import java.text.NumberFormat;
import java.util.Locale;

public abstract class Meters implements Comparable<Meters> {
	public enum Type {
		W, // active power
		VAR, // reactive power
		PF, // power factor
		V, // voltage
		I, // current
		BAT, // battery%
		T, // temperature (celsius)
		H, // humidity %
		L, // lux
		TX1, // temperature (celsius)
		TX2, // temperature (celsius)
		EXS // ext switch status
	};
	protected static NumberFormat NF = NumberFormat.getNumberInstance(Locale.ENGLISH);
	static {
		NF.setMaximumFractionDigits(2);
		NF.setMinimumFractionDigits(2);
	}
	
	public abstract Type[] getTypes();
	
	public abstract float getValue(Type t);
	
	public boolean hasType(Type t) {
		for(Type type: getTypes()) {
			if(type == t) return true;
		}
		return false;
	}
	
	@Override
	public int compareTo(Meters o) {
		final Type t0 = getTypes()[0];
		int v = t0.toString().compareTo(o.getTypes()[0].toString());
		if(v == 0) {
			v = Float.compare(getValue(t0), o.getValue(t0));
		}
		return v;
	}

	@Override
	public String toString() {
		Type[] t = getTypes();
		if(t.length > 0) {
			String res = t[0] + "=" + NF.format(getValue(t[0]));
			for(int i = 1; i < t.length; i++) {
				res += " " + t[i] + "=" + NF.format(getValue(t[i]));
			}
			return res;
		} else {
			return "";
		}
	}
}