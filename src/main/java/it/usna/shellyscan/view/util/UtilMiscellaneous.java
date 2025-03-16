package it.usna.shellyscan.view.util;

import java.text.NumberFormat;

import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class UtilMiscellaneous {
	private final static NumberFormat formatterN2 = NumberFormat.getInstance();
	static {
		formatterN2.setMaximumFractionDigits(2);
		formatterN2.setMinimumFractionDigits(2);
	}
	
	private UtilMiscellaneous() {}
	
	public static String getDescName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		return (dName == null || dName.isEmpty()) ? d.getHostname() : dName;
	}
	
	public static String getDescName(ShellyAbstractDevice d, int channel) {
		if(d instanceof ModulesHolder) {
			DeviceModule[] ri = ((ModulesHolder)d).getModules();
			if(ri != null) {
				String name;
				if(channel < ri.length && ri[channel] instanceof RelayInterface rel && (name = rel.getName()) != null && name.length() > 0) {
					final String dName = d.getName();
					return (dName != null && dName.length() > 0) ? dName + "-" + name : name;
				}
			}
		}
		return channel == 0 ? getDescName(d) : getDescName(d) + "-" + (channel + 1);
	}
	
	public static String getDescName(ShellyAbstractDevice d, String label) {
		return (label != null && label.isEmpty() == false) ? getDescName(d) : getDescName(d) + "-" + label;
	}

	public static String getFullName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		if(dName.isEmpty()) {
			return d.getHostname() + "-" + d.getTypeName();
		} else {
			return dName + "-" + d.getHostname() + "-" + d.getTypeName();
		}
	}
	
	public static String getExtendedHostName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		return d.getHostname() + " - " + (dName == null || dName.isEmpty() ? d.getTypeName() : dName);
	}
	
	public static String celsiusToFahrenheit(float celsius) {
		return formatterN2.format(celsius * 1.8f + 32f);
	}
	
	public static int clamp(int value, int min, int max) {
		if(value >= max) {
			return max;
		}
		if(value <= min) {
			return min;
		}
		return value;
	}
}