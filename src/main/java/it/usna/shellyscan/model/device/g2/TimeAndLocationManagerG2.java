package it.usna.shellyscan.model.device.g2;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.TimeAndLocationManager;

public class TimeAndLocationManagerG2 implements TimeAndLocationManager {
	private final AbstractG2Device d;
	private String server;
	
	public TimeAndLocationManagerG2(AbstractG2Device d) throws IOException {
		this.d = d;
		init(d.getJSON("/rpc/Shelly.GetConfig"));
	}
	
	private void init(JsonNode settings) {
		server = settings.path("sys").path("sntp").path("server").textValue();
	}
	
	
	@Override
	public String getSNTPServer() {
		return server;
	}

	@Override
	public String setSNTPServer(String server) {
		String ret = d.postCommand("Sys.SetConfig", "{\"config\":{\"sntp\": {\"server\": \"" + server + "\"}}}");
		if(ret == null) {
			this.server = server;
		}
		return ret;
	}
}

// Shelly.ListTimezones
// http://<IP>/rpc/Sys.SetConfig?config={"location":{"tz":"Europe/Sofia"}}