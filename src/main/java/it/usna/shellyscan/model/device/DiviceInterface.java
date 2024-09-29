package it.usna.shellyscan.model.device;

import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public interface DiviceInterface {

	String getTypeID();

	String getTypeName();
	
	Status getStatus();
	
	String getHostname();

	int getRssi();

	long getLastTime();

	String getName();
	
	InetAddressAndPort getAddressAndPort();

	String getMacAddress();
	
	default Meters[] getMeters() {
		return null;
	}
}
