package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.MQTTManager;

//https://shelly-api-docs.shelly.cloud/gen2/Components/SystemComponents/Mqtt
public class MQTTManagerG2 implements MQTTManager {
	private final AbstractG2Device d;
	private boolean enabled;
	private String server;
	private String user;
	private String prefix;
	
	// G2 specific
	private String sslCA;
	private boolean control;
	private boolean rpc;
	private boolean rpcNtf;
	private boolean statusNtf;

	public MQTTManagerG2(AbstractG2Device d) throws IOException {
		this.d = d;
		init();
	}

	private void init() throws IOException {
		JsonNode settings = d.getJSON("/rpc/MQTT.GetConfig");
		this.enabled = settings.get("enable").asBoolean();
		this.server = settings.path("server").asText("");
		this.user = settings.path("user").asText("");
		this.prefix = settings.path("topic_prefix").asText("");
		this.sslCA = settings.path("ssl_ca").asText("");
		this.control = settings.path("enable_control").asBoolean();
		this.rpc = settings.path("enable_rpc").asBoolean();
		this.rpcNtf = settings.path("rpc_ntf").asBoolean();
		this.statusNtf = settings.path("status_ntf").asBoolean();
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

	public String getSslCA() {
		return sslCA;
	}

	public boolean isRpcNtf() {
		return rpcNtf;
	}

	public boolean isStatusNtf() {
		return statusNtf;
	}
	
	public boolean isControlEnabled() {
		return control;
	}
	
	public boolean isRpcEnabled() {
		return rpc;
	}

	@Override
	public String disable() {
		return d.postCommand("MQTT.SetConfig", "{\"config\": {\"enable\": false}}");
	}

	@Override
	public String set(String server, String user, String pwd, String prefix) {
		ObjectNode pars = JsonNodeFactory.instance.objectNode();
		pars.put("enable", true);
		pars.put("server", server);
		pars.put("user", user);
		pars.put("pass", pwd);
		if(prefix == null) {
			pars.putNull("topic_prefix"); // device id is used as topic prefix (default)
		} else if(prefix.isEmpty() == false) {
			pars.put("topic_prefix", prefix); // do not alter if field is left blank
		}
		ObjectNode config = JsonNodeFactory.instance.objectNode();
		config.set("config", pars);
		return d.postCommand("MQTT.SetConfig", config);
	}
	
	public String set(Boolean control, Boolean rpc, Boolean rpcNtf, Boolean statusNtf, String server, String user, String pwd, String prefix) {
		ObjectNode pars = JsonNodeFactory.instance.objectNode();
		pars.put("enable", true);
		if(control != null) {
			pars.put("enable_control", control);
		}
		if(rpc != null) {
			pars.put("enable_rpc", rpc);
		}
		if(rpcNtf != null) {
			pars.put("rpc_ntf", rpcNtf);
		}
		if(statusNtf != null) {
			pars.put("status_ntf", statusNtf);
		}
		pars.put("server", server);
		pars.put("user", user);
		pars.put("pass", pwd);
		if(prefix == null) {
			pars.putNull("topic_prefix"); // device id is used as topic prefix (default)
		} else if(prefix.isEmpty() == false) {
			pars.put("topic_prefix", prefix); // do not alter if field is left blank
		}
		ObjectNode config = JsonNodeFactory.instance.objectNode();
		config.set("config", pars);
		return d.postCommand("MQTT.SetConfig", config);
	}
	
	public static String restore(AbstractG2Device parent, final JsonNode mqtt, String pwd) {
		ObjectNode outConfig = JsonNodeFactory.instance.objectNode();
		ObjectNode mqttCopy = mqtt.deepCopy();
		if(pwd != null && pwd.length() > 0) {
			mqttCopy.put("pass", pwd);
		}
		outConfig.set("config", mqttCopy);
		return parent.postCommand("MQTT.SetConfig", outConfig);
	}
}