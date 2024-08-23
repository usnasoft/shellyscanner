package it.usna.shellyscan.model.device.g1;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.TimeAndLocationManager;

public class TimeAndLocationManagerG1 implements TimeAndLocationManager {
	private final AbstractG1Device d;
	private String server;
	
	public TimeAndLocationManagerG1(AbstractG1Device d) throws IOException {
		this.d = d;
		init(d.getJSON("/settings"));
	}
	
	private void init(JsonNode settings) {
		server = settings.path("sntp").path("server").textValue();
	}
	
	@Override
	public String getSNTPServer() {
		return server;
	}

	@Override
	public String setSNTPServer(String server) {
		String ret = d.sendCommand("/settings?sntp_server=" + server);
		if(ret == null) {
			this.server = server;
		}
		return ret;
	}
}

// https://api.shelly.cloud/timezone/tzlist