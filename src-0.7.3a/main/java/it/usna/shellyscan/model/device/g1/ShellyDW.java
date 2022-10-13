package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

public class ShellyDW extends AbstractG1Device {
	public final static String ID = "SHDW-1";
	private boolean open;

	public ShellyDW(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		JsonNode status = getJSON("/status");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(status);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly DW";
	}
	
	public boolean isOpen() {
		return open;
	}
	
//	@Override
//	protected void fillSettings(JsonNode settings) throws IOException {
//		super.fillSettings(settings);
//	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		open = status.get("sensor").get("state").asText("").equals("open");
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "dark_threshold", "twilight_threshold",
				"led_status_disable", "lux_wakeup_enable", "tilt_enabled", "vibration_enabled", "vibration_sensitivity", "reverse_open_close")));
	}
}