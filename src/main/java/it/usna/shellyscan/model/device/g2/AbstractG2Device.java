package it.usna.shellyscan.model.device.g2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.Authentication;
import org.eclipse.jetty.client.AuthenticationStore;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.DigestAuthentication;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.DeviceAPIException;
import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;
import it.usna.shellyscan.model.device.WIFIManager.Network;
import it.usna.shellyscan.model.device.g2.modules.KVS;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;

/**
 * Base abstract class for any gen2(+) Shelly device
 * @author usna
 */
public abstract class AbstractG2Device extends ShellyAbstractDevice {
	public final static int LOG_VERBOSE = 4;
//	public final static int LOG_WARN = 1;

	private final static Logger LOG = LoggerFactory.getLogger(AbstractG2Device.class);
	protected WebSocketClient wsClient;
	private boolean rangeExtender;

	protected AbstractG2Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	public void init(HttpClient httpClient, WebSocketClient wsClient, JsonNode devInfo) throws IOException {
		this.httpClient = httpClient;
		this.wsClient = wsClient;
		init(devInfo);
	}

	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText().toUpperCase();

		fillSettings(getJSON("/rpc/Shelly.GetConfig"));
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	public void setAuthentication(Authentication auth) {
		AuthenticationStore store = httpClient.getAuthenticationStore();
		Authentication ar = store.findAuthentication("Digest", URI.create(uriPrefix), DigestAuthentication.ANY_REALM);
		if(ar != null) {
			store.removeAuthentication(ar);
		}
		if(auth != null) {
			store.addAuthentication(auth);
		}
	}

	protected void fillSettings(JsonNode config) throws IOException {
		JsonNode sysNode = config.get("sys");
		this.name = sysNode.path("device").path("name").asText("");

		JsonNode udp;
		JsonNode debugNode = sysNode.path("debug");
		if(debugNode.path("websocket").path("enable").booleanValue()) {
			this.debugEnabled = LogMode.SOCKET;
		} else if(debugNode.path("mqtt").path("enable").booleanValue()) {
			this.debugEnabled = LogMode.MQTT;
		} else if((udp = debugNode.get("udp")) != null && udp.get("addr").isNull() == false) {  // no "udp" on wall display ???
			this.debugEnabled = LogMode.UDP;
		} else {
			this.debugEnabled = LogMode.NO;
		}

		this.cloudEnabled = config.path("cloud").path("enable").booleanValue();
		this.mqttEnabled = config.path("mqtt").path("enable").booleanValue();

		this.rangeExtender = config.get("wifi").path("ap").path("range_extender").path("enable").booleanValue(); // no "ap" on wall display ???
	}

	protected void fillStatus(JsonNode status) throws IOException {
		this.cloudConnected = status.path("cloud").path("connected").booleanValue();
		JsonNode wifiNode = status.get("wifi");
		this.rssi = wifiNode.path("rssi").intValue();
		this.ssid = wifiNode.path("ssid").asText();
		JsonNode sysNode = status.get("sys");
		this.uptime = sysNode.get("uptime").intValue();
		this.rebootRequired = sysNode.path("restart_required").booleanValue();
		this.mqttConnected = status.path("mqtt").path("connected").booleanValue();

		lastConnection = System.currentTimeMillis();
	}

	@Override
	public void refreshSettings() throws IOException {
		fillSettings(getJSON("/rpc/Shelly.GetConfig"));
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	@Override
	public void refreshStatus() throws IOException {
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	@Override
	public String[] getInfoRequests() {
		return new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents"};
	}

	@Override
	public void reboot() throws IOException {
		getJSON("/rpc/Shelly.Reboot");
	}

	@Override
	public boolean setEcoMode(boolean eco) {
		return postCommand("Sys.SetConfig", "{\"config\":{\"device\":{\"eco_mode\":" + eco + "}}}") == null;
	}

	public boolean setDebugMode(LogMode mode, boolean enable) {
		if(mode == LogMode.SOCKET) {
			return postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": " + (enable ? "true" : "false") + "}}}}") == null;
		} else if(mode == LogMode.MQTT) {
			return postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"mqtt\":{\"enable\": " + (enable ? "true" : "false") + "}}}}") == null;
		} else if(mode == LogMode.NO) {
			return postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": false}, \"mqtt\":{\"enable\": false}, \"udp\":{\"addr\": null}} } }") == null;
		} else {
			return false;
		}
	}

	@Override
	public String setCloudEnabled(boolean enable) {
		return postCommand("Cloud.SetConfig", "{\"config\":{\"enable\":" + enable + "}}");
	}

	public boolean setBLEMode(boolean ble) {
		return postCommand("BLE.SetConfig", "{\"config\":{\"enable\":" + ble + "}}") == null;
	}

	public boolean rebootRequired() {
		return rebootRequired; //return getJSON("/rpc/Sys.GetStatus").path("restart_required").asBoolean(false);
	}

	public boolean isExtender() {
		return rangeExtender;
	}

	@Override
	public FirmwareManager getFWManager() {
		return new FirmwareManagerG2(this);
	}

	@Override
	public WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException {
		return new WIFIManagerG2(this, net);
	}

	public RangeExtenderManager getRangeExtenderManager() throws IOException {
		return new RangeExtenderManager(this);
	}

	@Override
	public LoginManager getLoginManager() throws IOException {
		return new LoginManagerG2(this);
	}

	@Override
	public MQTTManagerG2 getMQTTManager() throws IOException {
		return new MQTTManagerG2(this);
	}
	
	@Override
	public TimeAndLocationManagerG2 getTimeAndLocationManager() {
		return new TimeAndLocationManagerG2(this);
	}

	public String postCommand(final String method, JsonNode payload) {
		try {
			return postCommand(method, jsonMapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			return e.toString();
		}
	}

	/**
	 * return null if ok or error description in case of error
	 */
	public String postCommand(final String method, String payload) {
		try {
			final JsonNode resp = executeRPC(method, payload);
			JsonNode error;
			if((error = resp.get("error")) == null) { // {"id":1,"src":"shellyplusi4-xxx","result":{"restart_required":true}}
//				System.out.println(resp.toPrettyString());
				if(resp.path("result").path("restart_required").asBoolean(false)) {
					rebootRequired = true;
				}
				if(status == Status.NOT_LOOGGED) {
					return "Status-PROTECTED";
				} else if(status == Status.ERROR) {
					return "Status-ERROR";
				} else {
					return null;
				}
			} else {
				return error.path("message").asText("Generic error");
			}
		} catch(IOException e) {
			return "Status-OFFLINE";
		} catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	
	@Override
	public JsonNode getJSON(final String command) throws IOException {
		JsonNode resp = super.getJSON(command);
		if(resp.has("code") && resp.has("message")) { // e.g.: {"code":-114,"message":"Method KVS.GetMany failed: No such component"}
			throw new DeviceAPIException(resp.get("code").intValue(), resp.get("message").asText("Generic error"));
		}
		return resp;
	}

	public JsonNode getJSON(final String method, String payload) throws IOException {
		final JsonNode resp = executeRPC(method, payload);
		JsonNode result;
		if((result = resp.get("result")) != null) {
			return result;
		} else {
			JsonNode error = resp.get("error");
			throw new DeviceAPIException(error.get("code").intValue(), error.get("message").asText("Generic error"));
		}	
	}

	private JsonNode executeRPC(final String method, String payload) throws IOException, StreamReadException { // StreamReadException extends ... IOException
		try {
			ContentResponse response = httpClient.POST(uriPrefix + "/rpc")
					.body(new StringRequestContent("application/json", "{\"id\":1, \"method\":\"" + method + "\", \"params\":" + payload + "}", StandardCharsets.UTF_8))
					.send();
			int statusCode = response.getStatus(); //response.getContentAsString()
			if(statusCode == HttpStatus.OK_200) {
				status = Status.ON_LINE;
			} else if(statusCode == HttpStatus.UNAUTHORIZED_401) {
				status = Status.NOT_LOOGGED;
			} else /*if(statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR || statusCode == HttpURLConnection.HTTP_BAD_REQUEST)*/ {
				status = Status.ERROR;
				LOG.debug("executeRPC - reponse code: {}", statusCode);
			}
			return jsonMapper.readTree(response.getContent());
		} catch(InterruptedException | ExecutionException | TimeoutException | SocketTimeoutException e) {
			status = Status.OFF_LINE;
			throw new DeviceOfflineException(e);
		}
	}

	public CompletableFuture<Session> connectWebSocketClient(WebSocketDeviceListener listener/*, boolean activate*/) throws IOException, InterruptedException, ExecutionException {
		try {
			CompletableFuture<Session> s = wsClient.connect(listener, URI.create("ws://" + address.getHostAddress() + ":" + port + "/rpc")); // this also do upgrade
			s.get().sendText("{\"id\":2, \"src\":\"S_Scanner\", \"method\":\"Shelly.GetDeviceInfo\"}", Callback.NOOP);
			return s;
		} catch(RuntimeException e) {
			LOG.warn("connectWebSocketClient", e);
			throw e;
		}
	}
	
	/** this doesn't work on protected devices
	 * Кристиан Тодоров:
	When sending the challange request for the debug endpoint, you have to provide the same auth params, but as get paramethers in format
	auth.[paramName]=paramValue. For example about the username it will be auth.username=admin&auth.cnonce=…&auth.respose=...
	 */
	public Future<Session> connectWebSocketLogs(WebSocketDeviceListener listener) throws IOException, InterruptedException, ExecutionException {
		return wsClient.connect(listener, URI.create("ws://" + address.getHostAddress() + ":" + port + "/debug/log"));
	}

	@Override
	public boolean backup(final File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // unmanaged battery device
				sectionToStream("/rpc/Schedule.List", "Schedule.List.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try {
				sectionToStream("/rpc/KVS.GetMany", "KVS.GetMany.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			byte[] scripts = null;
			try {
				scripts = sectionToStream("/rpc/Script.List", "Script.List.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // Virtual components (PRO & gen3)
				sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // On devices with active sensor add-on
				sectionToStream("/rpc/SensorAddon.GetPeripherals", SensorAddOn.BACKUP_SECTION, out);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			// Scripts
			if(scripts != null) {
				JsonNode scrList = jsonMapper.readTree(scripts).get("scripts");
				for(JsonNode scr: scrList) {
					try {
						Script script = new Script(this, scr);
						byte[] code =  script.getCode().getBytes();
						ZipEntry entry = new ZipEntry(scr.get("name").asText() + ".mjs");
						out.putNextEntry(entry);
						out.write(code, 0, code.length);
					} catch(IOException e) {}
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			}
			try { // Device specific
				backup(out);
			} catch(Exception e) {
				LOG.error("backup specific", e);
			}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}
	
	/** implement for devices that need additional information */
	protected void backup(ZipOutputStream out) throws IOException, InterruptedException {	}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
		try {
			JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
			final String fileHostname = devInfo.get("id").asText("");
			final String fileType = devInfo.get("app").asText();
			if(this.getTypeID().equals(fileType) == false) {
				res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			} else {
				boolean sameHost = fileHostname.equals(this.hostname);
				if(sameHost == false) {
					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, fileHostname);
				}
				JsonNode virtualComponents = backupJsons.get("Shelly.GetComponents.json");
				if(virtualComponents != null && virtualComponents.path("components").size() > 0) {
					res.put(RestoreMsg.WARN_RESTORE_VIRTUAL, null);
				}
				if(devInfo.path("auth_en").asBoolean()) {
					res.put(RestoreMsg.RESTORE_LOGIN, LoginManagerG2.LOGIN_USER);
				}
				Network currentConnection = WIFIManagerG2.currentConnection(this);
				if(currentConnection != Network.UNKNOWN) {
					JsonNode wifi = config.at("/wifi/sta");
					if(wifi.path("enable").asBoolean() && (sameHost || wifi.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.PRIMARY) {
						if(wifi.path("is_open").asBoolean() == false) {
							res.put(RestoreMsg.RESTORE_WI_FI1, wifi.path("ssid").asText());
						}
					}
					JsonNode wifi2 = config.at("/wifi/sta1");
					if(wifi2.path("enable").asBoolean() && (sameHost || wifi2.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.SECONDARY) {
						if(wifi2.path("is_open").asBoolean() == false) {
							res.put(RestoreMsg.RESTORE_WI_FI2, wifi2.path("ssid").asText());
						}
					}
					JsonNode wifiAP = config.at("/wifi/ap");
					if(wifiAP.path("enable").asBoolean() && currentConnection != Network.AP) {
						if(wifiAP.path("is_open").asBoolean() == false) {
							res.put(RestoreMsg.RESTORE_WI_FI_AP, wifiAP.path("ssid").asText());
						}
					}
				}
				if(config.at("/mqtt/enable").asBoolean() && config.at("/mqtt/user").asText("").length() > 0) {
					res.put(RestoreMsg.RESTORE_MQTT, config.at("/mqtt/user").asText());
				}
				JsonNode scripts = backupJsons.get("Script.List.json");
				if(scripts != null && scripts.path("scripts").size() > 0) {
					List<String> scriptsEnabledByDefault = new ArrayList<>();
					List<String> scriptsWithSameName = new ArrayList<>();
					JsonNode existingScripts = Script.list(this);
					List<String> existingScriptsNames = new ArrayList<>();
					for(JsonNode existingScript: existingScripts) {
						existingScriptsNames.add(existingScript.get("name").asText());
					}
					for(JsonNode jsonScript: scripts.get("scripts")) {
						if(existingScriptsNames.contains(jsonScript.get("name").asText()))
							scriptsWithSameName.add(jsonScript.get("name").asText());
						if(jsonScript.get("enable").asBoolean())
							scriptsEnabledByDefault.add(jsonScript.get("name").asText());
					}
					if(scriptsWithSameName.isEmpty() == false) {
						res.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE, String.join(", ", scriptsWithSameName));
					}
					if(scriptsEnabledByDefault.isEmpty() == false) {
						res.put(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP, String.join(", ", scriptsEnabledByDefault));
					}
				}
				// device specific
				restoreCheck(backupJsons, res);
			}
		} catch(RuntimeException e) {
			LOG.error("restoreCheck", e);
			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
		}
		return res;
	}

	/** device specific */
	protected void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> resp) throws IOException {}

	@Override
	public final List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> userPref) throws IOException {
		final ArrayList<String> errors = new ArrayList<>();
		try {
			final long delay = this instanceof BatteryDeviceInterface ? Devices.MULTI_QUERY_DELAY / 2: Devices.MULTI_QUERY_DELAY;

			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
			errors.add("->r_step:specific");
			restore(backupJsons, errors);
			if(status == Status.OFF_LINE) {
				return errors.size() > 0 ? errors : List.of(RestoreMsg.ERR_UNKNOWN.toString());
			}
			
			errors.add("->r_step:restoreCommonConfig");
			restoreCommonConfig(config, delay, userPref, errors);

			errors.add("->r_step:restoreSchedule");
			JsonNode schedule = backupJsons.get("Schedule.List.json");
			if(schedule != null) { // some devices do not have Schedule.List +H&T
				TimeUnit.MILLISECONDS.sleep(delay);
				restoreSchedule(schedule, errors);
			}

			errors.add("->r_step:Script");
			Script.restoreAll(this, backupJsons, delay, userPref.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE), userPref.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP), errors);

			errors.add("->r_step:KVS");
			JsonNode kvs = backupJsons.get("KVS.GetMany.json");
			if(kvs != null) {
				try {
					TimeUnit.MILLISECONDS.sleep(delay);
					KVS kvStore = new KVS(this);
					kvStore.restoreKVS(kvs, errors);
				} catch(Exception e) {}
			}
			
			errors.add("->r_step:Webhooks");
			TimeUnit.MILLISECONDS.sleep(delay);
			Webhooks.restore(this, backupJsons.get("Webhook.List.json"), errors);

			errors.add("->r_step:WIFIManagerG2");
			TimeUnit.MILLISECONDS.sleep(delay);
			Network currentConnection = WIFIManagerG2.currentConnection(this);
			if(currentConnection != Network.UNKNOWN) {
				JsonNode sta1Node = config.at("/wifi/sta1");
				if(sta1Node.isMissingNode() == false && (userPref.containsKey(RestoreMsg.RESTORE_WI_FI2) || sta1Node.path("is_open").asBoolean() || sta1Node.path("enable").asBoolean() == false) && currentConnection != Network.SECONDARY) {
					TimeUnit.MILLISECONDS.sleep(delay);
					WIFIManagerG2 wm = new WIFIManagerG2(this, Network.SECONDARY, true);
					errors.add(wm.restore(sta1Node, userPref.get(RestoreMsg.RESTORE_WI_FI2)));
				}
				if((userPref.containsKey(RestoreMsg.RESTORE_WI_FI1) || config.at("/wifi/sta/is_open").asBoolean() || config.at("/wifi/sta/enable").asBoolean() == false) && currentConnection != Network.PRIMARY) {
					TimeUnit.MILLISECONDS.sleep(delay);
					WIFIManagerG2 wm = new WIFIManagerG2(this, Network.PRIMARY, true);
					errors.add(wm.restore(config.at("/wifi/sta"), userPref.get(RestoreMsg.RESTORE_WI_FI1)));
				}
				
				JsonNode apNode = config.at("/wifi/ap"); // wall display -> isMissingNode() (?)
				if(apNode.isMissingNode() == false && ((userPref.containsKey(RestoreMsg.RESTORE_WI_FI_AP) || apNode.path("is_open").asBoolean() || apNode.path("enable").asBoolean() == false) && currentConnection != Network.AP)) {
					TimeUnit.MILLISECONDS.sleep(delay);
					errors.add(WIFIManagerG2.restoreAP_roam(this, config.get("wifi"), userPref.get(RestoreMsg.RESTORE_WI_FI_AP)));
				}
			}
			
			errors.add("->r_step:LoginManagerG2");
			TimeUnit.MILLISECONDS.sleep(delay);
			LoginManagerG2 lm = new LoginManagerG2(this, true);
			if(userPref.containsKey(RestoreMsg.RESTORE_LOGIN)) {
				errors.add(lm.set(null, userPref.get(RestoreMsg.RESTORE_LOGIN).toCharArray()));
			} else if(backupJsons.get("Shelly.GetDeviceInfo.json").path("auth_en").asBoolean() == false) {
				errors.add(lm.disable());
			}
		} catch(RuntimeException | InterruptedException e) {
			LOG.error("restore - RuntimeException", e);
			errors.add(RestoreMsg.ERR_UNKNOWN.toString());
		}
		return errors;
	}

	// Shelly.GetConfig.json
	void restoreCommonConfig(JsonNode config, final long delay, Map<RestoreMsg, String> userPref, List<String> errors) throws InterruptedException, IOException {
		ObjectNode outConfig = JsonNodeFactory.instance.objectNode();

		// BLE.SetConfig
		outConfig.set("config", config.get("ble").deepCopy());
		TimeUnit.MILLISECONDS.sleep(delay);
		errors.add(postCommand("BLE.SetConfig", outConfig));

		// Cloud.SetConfig
		ObjectNode outCloud = JsonNodeFactory.instance.objectNode(); // Cloud
		outCloud.put("enable", config.at("/cloud/enable").asBoolean());
		outConfig.set("config", outCloud);
		TimeUnit.MILLISECONDS.sleep(delay);
		errors.add(postCommand("Cloud.SetConfig", outConfig));

		// Sys.SetConfig
		JsonNode sys = config.get("sys");

//		ObjectNode outDevice = JsonNodeFactory.instance.objectNode();
//		outDevice.put("name", sys.at("/device/name").asText("")); // does not appreciate null
//		JsonNode ecoMode = sys.at("/device/eco_mode");
//		if(ecoMode.isNull() == false) {
//			outDevice.put("eco_mode", ecoMode.asBoolean());
//		}
		ObjectNode outSys = JsonNodeFactory.instance.objectNode();
		
		ObjectNode outDevice = sys.get("device").deepCopy(); // todo test (anche caso name = null)
		outDevice.remove("mac");
		outDevice.remove("fw_id");
		outDevice.remove("addon_type");
		if(outDevice.get("name") == null) { // does not appreciate null
			outDevice.remove("name");
		}
		outSys.set("device", outDevice);

		outSys.set("sntp", sys.get("sntp").deepCopy());

		outConfig.set("config", outSys);
		TimeUnit.MILLISECONDS.sleep(delay);
		errors.add(postCommand("Sys.SetConfig", outConfig));
		
		final JsonNode mqtt = config.path("mqtt");
		if(userPref.containsKey(RestoreMsg.RESTORE_MQTT) || mqtt.path("enable").asBoolean() == false || mqtt.path("user").asText("").length() == 0) {
			TimeUnit.MILLISECONDS.sleep(delay);
			MQTTManagerG2 mqttM = new MQTTManagerG2(this, true);
			errors.add(mqttM.restore(mqtt, userPref.get(RestoreMsg.RESTORE_MQTT)));
		}
	}

	private void restoreSchedule(JsonNode schedule, ArrayList<String> errors) throws InterruptedException {
		errors.add(postCommand("Schedule.DeleteAll", "{}"));
		for(JsonNode sc: schedule.get("jobs")) {
			ObjectNode thisSc = (ObjectNode)sc.deepCopy();
			thisSc.remove("id");
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(postCommand("Schedule.Create", thisSc));
		}
	}
	
	public static ObjectNode createIndexedRestoreNode(JsonNode backConfig, String type, int index) { // todo addon, input, switch
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", index);
		ObjectNode data = (ObjectNode)backConfig.get(type + ":" + index).deepCopy();
		data.remove("id");
		out.set("config", data);
		return out;
	}

	/** device specific */
	protected abstract void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws IOException, InterruptedException;
	
	/* experimental */
//	public Future<Session> connectWebSocketLogs2(WebSocketDeviceListener listener) throws IOException, InterruptedException, ExecutionException {
//		//return wsClient.connect(listener, URI.create("ws://" + address.getHostAddress() + ":" + port + "/debug/log"));
//		//		ClientUpgradeRequest upgrade = new ClientUpgradeRequest();
//		try {
//			String nonce = (System.currentTimeMillis() / 1000) + "";
//			String cnonce = "ss" + nonce;
//			System.out.println(nonce);
//
//			String response = LoginManagerG2.getResponse(nonce, cnonce, hostname, "1234");
//
//			CompletableFuture<Session> s = wsClient.connect(listener, URI.create("ws://192.168.1.10/debug/log?" +
//					"auth.auth_type=digest&" +
//					"auth.nonce="+ nonce + "&" +
//					"auth.nc=1&" +
//					"auth.realm=shellyplus2pm-485519a2bb1c" +
//					"&auth.algorithm=SHA-256&" +
//					"auth.username=admin&" +
//					"auth.cnonce=xdaChipkEtz61jum&" +
//					"auth.response=" + response),
//
//					/*upgrade*/null, new JettyUpgradeListener() {
//				@Override
//				public void onHandshakeRequest(org.eclipse.jetty.client.Request request) {
//					System.out.println(request);
//				}
//				@Override
//				public void onHandshakeResponse(org.eclipse.jetty.client.Request request, org.eclipse.jetty.client.Response response) {
//					System.out.println(request);
//					System.out.println(response.getHeaders().getField("WWW-Authenticate").getValueList());
//				}
//
//
//			}); // this also do upgrade
//			return s;
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//	}
} // 477 - 474 - 525 - 568