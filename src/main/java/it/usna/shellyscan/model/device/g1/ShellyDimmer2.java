package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.hc.client5.http.auth.CredentialsProvider;

public class ShellyDimmer2 extends ShellyDimmer {
	public final static String ID = "SHDM-2";

	public ShellyDimmer2(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
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