package it.usna.shellyscan.model.device.g1;

import java.net.InetAddress;

public class ShellyPlugE extends ShellyPlug {
	public final static String ID = "SHPLG2-1";

	public ShellyPlugE(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "Plug E";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
}