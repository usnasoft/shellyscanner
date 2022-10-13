package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.MQTTManager;

public class MQTTManagerG1 implements MQTTManager {
	private final AbstractG1Device d;
	private boolean enabled;
	private String server;
	private String user;
	private String prefix;
	
	private int rTimeoutMax;
	private int rTimeoutMin;
	private boolean cleanSession;
	private int keepAlive;
	private int qos;
	private boolean retain;
	private int updatePer;

	public MQTTManagerG1(AbstractG1Device d) throws IOException {
		this.d = d;
		init();
	}
	
	public MQTTManagerG1(AbstractG1Device d, boolean noInit) throws IOException {
		this.d = d;
		if(noInit == false) {
			init();
		}
	}

	private void init( ) throws IOException {
		JsonNode settings = d.getJSON("/settings").get("mqtt");
		this.enabled = settings.get("enable").asBoolean();
		this. server = settings.path("server").asText("");
		this.user = settings.path("user").asText("");
		this.prefix = settings.path("id").asText("");

		// G1 specific
		this.rTimeoutMax = settings.path("reconnect_timeout_max").asInt(0);
		this.rTimeoutMin = settings.path("reconnect_timeout_min").asInt(0);
		this.cleanSession = settings.get("clean_session").asBoolean();
		this.keepAlive = settings.path("keep_alive").asInt(0);
		this.qos = settings.path("max_qos").asInt(0);
		this.retain = settings.path("retain").asBoolean();
		this.updatePer = settings.path("update_period").asInt(0);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String getServer() {
		return server;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public String getPrefix() {
		return prefix;
	}
	
	public int getrTimeoutMax() {
		return rTimeoutMax;
	}

	public int getrTimeoutMin() {
		return rTimeoutMin;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public int getKeepAlive() {
		return keepAlive;
	}

	public int getQos() {
		return qos;
	}

	public boolean isRetain() {
		return retain;
	}

	public int getUpdatePeriod() {
		return updatePer;
	}

	@Override
	public String disable() {
		return d.sendCommand("/settings?mqtt_enable=false");
	}

	@Override
	public String set(String server, String user, String pwd, String prefix) {
		if(user == null || pwd == null) {
			user = pwd = "";
		}
		try {
			String cmd = "/settings?mqtt_enable=true" +
					"&mqtt_server=" + URLEncoder.encode(server, StandardCharsets.UTF_8.name()) + 
					"&mqtt_user=" + URLEncoder.encode(user, StandardCharsets.UTF_8.name()) +
					"&mqtt_pass=" + URLEncoder.encode(pwd, StandardCharsets.UTF_8.name());
			if(prefix != null && prefix.isEmpty() == false) {
				cmd += "&mqtt_id=" + URLEncoder.encode(prefix, StandardCharsets.UTF_8.name());
			}
			return d.sendCommand(cmd);
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}

	public String set(String server, String user, String pwd, String prefix, int recTimeoutMax, int recTimeoutMin, String cleanSession, int keepAlive, int qos, String retain, int updatePer) {
		if(user == null || pwd == null) {
			user = pwd = "";
		}
		try {
			String cmd = "/settings?mqtt_enable=true" +
					"&mqtt_server=" + URLEncoder.encode(server, StandardCharsets.UTF_8.name()) + 
					"&mqtt_user=" + URLEncoder.encode(user, StandardCharsets.UTF_8.name()) +
					"&mqtt_pass=" + URLEncoder.encode(pwd, StandardCharsets.UTF_8.name());
			if(prefix == null) {
				cmd += "&mqtt_id="; // set default value
			} else if(prefix.isEmpty() == false) { // do not alter if field is left blank
				cmd += "&mqtt_id=" + URLEncoder.encode(prefix, StandardCharsets.UTF_8.name());
			}
			if(recTimeoutMax >= 0) {
				cmd += "&mqtt_reconnect_timeout_max=" + recTimeoutMax;
			}
			if(recTimeoutMin >= 0) {
				cmd += "&mqtt_reconnect_timeout_min=" + recTimeoutMin;
			}
			if(cleanSession != null && cleanSession.isEmpty() == false) {
				cmd += "&mqtt_clean_session=" + cleanSession; // "true" or "false"
			}
			if(keepAlive >= 0) {
				cmd += "&mqtt_keep_alive=" + keepAlive;
			}
			if(qos >= 0) {
				cmd += "&mqtt_max_qos=" + qos;
			}
			if(retain != null && retain.isEmpty() == false) {
				cmd += "&mqtt_retain=" + retain; // "true" or "false"
			}
			if(updatePer >= 0) {
				cmd += "&mqtt_update_period=" + updatePer;
			}
			return d.sendCommand(cmd);
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}
	
	public String restore(final JsonNode mqtt, String pwd) {
		if(mqtt.get("enable").asBoolean()) {
			// In case authentication is required, mqtt_user and mqtt_pass
			return set(mqtt.get("server").asText(), mqtt.get("user").asText(), pwd, mqtt.get("id").asText(),
					mqtt.get("reconnect_timeout_max").asInt(), mqtt.get("reconnect_timeout_min").asInt(), mqtt.get("clean_session").asText(), mqtt.get("keep_alive").asInt(),
					mqtt.get("max_qos").asInt(), mqtt.get("retain").asText(), mqtt.get("update_period").asInt());
		} else {
			return disable();
		}
	}
}