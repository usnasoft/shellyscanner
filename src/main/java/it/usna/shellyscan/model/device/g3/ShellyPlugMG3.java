package it.usna.shellyscan.model.device.g3;

import java.net.InetAddress;

/**
 * Plug M Gen3 model
 */
public class ShellyPlugMG3 extends ShellyPlugSG3 {
	public static final String ID = "PlugMG3";
	public static final String MODEL = "S3PL-30110EU";

	public ShellyPlugMG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Plug M G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getModelID() {
		return MODEL;
	}
}