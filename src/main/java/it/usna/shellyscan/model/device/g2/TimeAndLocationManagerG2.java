package it.usna.shellyscan.model.device.g2;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.TimeAndLocationManager;

public class TimeAndLocationManagerG2 implements TimeAndLocationManager {
	private final AbstractG2Device d;
	
	public TimeAndLocationManagerG2(AbstractG2Device d) {
		this.d = d;
	}
	
	@Override
	public String getSNTPServer() throws IOException {
		JsonNode settings = d.getJSON("/rpc/Shelly.GetConfig");
		return settings.path("sys").path("sntp").path("server").textValue();
	}

	@Override
	public String setSNTPServer(String server) {
		return d.postCommand("Sys.SetConfig", "{\"config\":{\"sntp\": {\"server\": \"" + server + "\"}}}");
	}
}

// Shelly.ListTimezones
// http://<IP>/rpc/Sys.SetConfig?config={"location":{"tz":"Europe/Sofia"}}