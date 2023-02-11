package it.usna.shellyscan.model.device.g2;

import java.net.InetAddress;

public class ShellyPlusPlugUS extends ShellyPlusPlugIT {
	public final static String ID = "PlugUS";

	public ShellyPlusPlugUS(InetAddress address, String hostname) {
		super(address, hostname);
	}

	@Override
	public String getTypeName() {
		return "Plug +US";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
}