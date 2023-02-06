package it.usna.shellyscan.view.util;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class UtilCollecion {
	private UtilCollecion() {}
	
	public static String getDescName(ShellyAbstractDevice d) {
		final String dName = d.getName();
//		return (dName.length() > 0 ? dName : d.getTypeName());
		return (dName != null && dName.length() > 0 ? dName : d.getHostname());
	}
	
	public static String getDescName(ShellyAbstractDevice d, int channel) {
		if(d instanceof RelayCommander) {
			RelayInterface[] ri = ((RelayCommander)d).getRelays();
			String name;
			if(ri != null && ri.length > channel && (name = ri[channel].getName()) != null && name.length() > 0) {
//				return getDescName(d) + "-" + name;
				final String dName = d.getName();
				return (dName != null && dName.length() > 0) ? dName + "-" + name : name;
			}
		}
		return channel == 0 ? getDescName(d) : getDescName(d) +  "-" + (channel + 1);
	}
	
	public static String getFullName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		if(dName.length() > 0) {
			return dName + "-" + d.getHostname() + "-" + d.getTypeName();
		} else {
			return d.getHostname() + "-" + d.getTypeName();
		}
	}
	
	public static String getExtendedHostName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		return d.getHostname() + " - " + (dName != null && dName.length() > 0 ? dName : d.getTypeName());
	}
}