package it.usna.shellyscan.model.device.g1;

import java.net.InetAddress;

public class ShellyDimmer2 extends ShellyDimmer {
	public static final String ID = "SHDM-2";

	public ShellyDimmer2(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Dimmer 2";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
}