package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

public class ShellyDW2 extends ShellyDW {
	public final static String ID = "SHDW-2";

	public ShellyDW2(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly DW2";
	}
	
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		super.restore(settings, errors); // DW
		final JsonNode sensor = settings.get("sensor");
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(sensor, "temperature_threshold", "temperature_units")) +
				"&temperature_offset=" + settings.get("temperature_offset").asText());
	}
}