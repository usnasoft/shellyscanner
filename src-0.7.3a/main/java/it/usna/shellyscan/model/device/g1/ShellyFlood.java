package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

public class ShellyFlood extends AbstractG1Device {
	public final static String ID = "SHWT-1";
	private boolean flood;
	private float temp;
	private int bat;

	public ShellyFlood(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		JsonNode status = getJSON("/status");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(status);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Flood";
	}
	
//	@Override
//	protected void fillSettings(JsonNode settings) throws IOException {
//		super.fillSettings(settings);
//	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		flood = status.get("flood").asBoolean();
		temp = (float)status.get("tmp").get("tC").doubleValue();
		bat = status.get("bat").get("value").asInt();
	}
	
	public boolean flood() {
		return flood;
	}
	
	public float getTemp() {
		return temp;
	}

	public int getBattery() {
		return bat;
	}
	
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		JsonNode sensors = settings.get("sensors");
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(sensors, "temperature_units", "temperature_threshold")));
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "rain_sensor", "temperature_offset")));
	}
}