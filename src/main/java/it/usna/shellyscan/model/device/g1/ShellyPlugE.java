package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

public class ShellyPlugE extends ShellyPlug {
	public final static String ID = "SHPLG2-1";

	public ShellyPlugE(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
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