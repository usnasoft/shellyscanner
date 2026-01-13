package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import tools.jackson.databind.JsonNode;

public class TimeAndLocationManagerG1 implements TimeAndLocationManager {
	private final AbstractG1Device d;
	private String server;
	
	public TimeAndLocationManagerG1(AbstractG1Device d) throws IOException {
		this.d = d;
		init(d.getJSON("/settings"));
	}
	
	public TimeAndLocationManagerG1(AbstractG1Device d, JsonNode settings) {
		this.d = d;
		init(settings);
	}
	
	private void init(JsonNode settings) {
		server = settings.path("sntp").path("server").asString("");
	}
	
	@Override
	public String getSNTPServer() {
		return server;
	}

	@Override
	public String setSNTPServer(String server) {
		try {
			String ret = d.sendCommand("/settings?sntp_server=" + URLEncoder.encode(server, StandardCharsets.UTF_8.name()));
			if(ret == null) {
				this.server = server;
			}
			return ret;
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}
}

// https://api.shelly.cloud/timezone/tzlist
// https://api.shelly.cloud/timezone/tzoffsetlist
// https://api.shelly.cloud/timezone/autodetect
// http://<ip>/settings?tzautodetect=false&timezone=Europe/Rome&tz_utc_offset=3600&lat=38.130199&lng=13.329&tz_dst_auto=1