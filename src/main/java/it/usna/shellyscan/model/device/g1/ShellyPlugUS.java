package it.usna.shellyscan.model.device.g1;

import java.net.InetAddress;

public class ShellyPlugUS extends ShellyPlug {
	public final static String ID = "SHPLG-U1";

	public ShellyPlugUS(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "Plug US";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
}