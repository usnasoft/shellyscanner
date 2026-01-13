package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class TimeAndLocationManagerG2 implements TimeAndLocationManager {
	private final AbstractG2Device d;
	private String server;
	private String tz;
	private double lat;
	private double lon;
	
	public TimeAndLocationManagerG2(AbstractG2Device d) throws IOException {
		this.d = d;
		init(d.getJSON("/rpc/Shelly.GetConfig"));
	}
	
	public TimeAndLocationManagerG2(AbstractG2Device d, JsonNode settings) {
		this.d = d;
		init(settings);
	}
	
	private void init(JsonNode settings) {
		var sys = settings.path("sys");
		server = sys.path("sntp").path("server").asString("");
		var jLocation = sys.path("location");
		tz = jLocation.path("tz").asString("");
		lat = jLocation.path("lat").asDouble(0);
		lon = jLocation.path("lon").asDouble(0);
//		location = new Location(jLocation.path("tz").asString(""), jLocation.path("lat").asDouble(0), jLocation.path("lon").asDouble(0));
//		setTimeZone("Europe/Rome");
//		try {
//			detectTimeZone();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	
	@Override
	public String getSNTPServer() {
		return server;
	}

	@Override
	public String setSNTPServer(String server) {
		ObjectNode outConfig = JsonNodeFactory.instance.objectNode();
		outConfig.putObject("config").putObject("sntp").put("server", server);
		String ret = d.postCommand("Sys.SetConfig", outConfig);
		if(ret == null) {
			this.server = server;
		}
		return ret;
	}
	
	public Location getLocation() {
		return new Location(tz, lat, lon);
	}
	
	public String setTimeZone(String zone) {
		ObjectNode outConfig = JsonNodeFactory.instance.objectNode();
		outConfig.putObject("config").putObject("location").put("tz", zone);
		String ret = d.postCommand("Sys.SetConfig", outConfig);
		if(ret == null) {
			this.tz = zone;
		}
		return ret;
	}
	
	public Location detectTimeZone() throws IOException {
		var jLocation = d.getJSON("/rpc/Shelly.DetectLocation");
		return new Location(jLocation.path("tz").asString(""), jLocation.path("lat").asDouble(0), jLocation.path("lon").asDouble(0)); 
	}
	
	public record Location(String tz, double lat, double lon) {};
}

// https://shelly-api-docs.shelly.cloud/gen2/ComponentsAndServices/Shelly
// /rpc/Shelly.ListTimezones
// /rpc/Shelly.DetectLocation