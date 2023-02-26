package it.usna.shellyscan.view.util;

import javax.swing.ImageIcon;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.shellyscan.view.DevicesTable;

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
			RelayInterface rel;
			if(ri != null && ri.length > channel && (rel = ri[channel]) != null && (name = rel.getName()) != null && name.length() > 0) {
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
	
	public static ImageIcon getStatusIcon(ShellyAbstractDevice d) {
		if(d.getStatus() == Status.ON_LINE) {
			return DevicesTable.ONLINE_BULLET;
		} else if(d.getStatus() == Status.OFF_LINE) {
			return DevicesTable.OFFLINE_BULLET;
		} else if(d.getStatus() == Status.READING) {
			return DevicesTable.UPDATING_BULLET;
		} else if(d.getStatus() == Status.ERROR) {
			return DevicesTable.ERROR_BULLET;
		} else { // Status.NOT_LOOGGED
			return DevicesTable.LOGIN_BULLET;
		}
	}
}