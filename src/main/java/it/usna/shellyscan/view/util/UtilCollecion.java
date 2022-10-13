package it.usna.shellyscan.view.util;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class UtilCollecion {
	private UtilCollecion() {}
	
	public static String getDescName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		return (dName.length() > 0 ? dName : d.getTypeName());
	}
	
	public static String getFullName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		if(dName.length() > 0) {
			return dName + " - " + d.getHostname() + " - " + d.getTypeName();
		} else {
			return d.getHostname() + " - " + d.getTypeName();
		}
	}
}