package it.usna.shellyscan.model.device.g1;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.TimeAndLocationManager;

public class TimeAndLocationManagerG1 implements TimeAndLocationManager {
	private final AbstractG1Device d;
	
	public TimeAndLocationManagerG1(AbstractG1Device d) {
		this.d = d;
	}
	
	@Override
	public String getSNTPServer() throws IOException {
		JsonNode settings = d.getJSON("/settings");
		return settings.path("sntp").path("server").textValue();
	}

	@Override
	public String setSNTPServer(String server) throws IOException {
		return d.sendCommand("/settings?sntp.server=" + server);
	}
}

// https://api.shelly.cloud/timezone/tzlist