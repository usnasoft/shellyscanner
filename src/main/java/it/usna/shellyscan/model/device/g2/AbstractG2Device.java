package it.usna.shellyscan.model.device.g2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.g2.modules.FirmwareManagerG2;
import it.usna.shellyscan.model.device.g2.modules.InputResetManagerG2;
import it.usna.shellyscan.model.device.g2.modules.KVS;
import it.usna.shellyscan.model.device.g2.modules.LoginManagerG2;
import it.usna.shellyscan.model.device.g2.modules.MQTTManagerG2;
import it.usna.shellyscan.model.device.g2.modules.RangeExtenderManager;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManager;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.TimeAndLocationManagerG2;
import it.usna.shellyscan.model.device.g2.modules.WIFIManagerG2;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

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
			this.debugMode = LogMode.SOCKET;
		} else if(debugNode.path("mqtt").path("enable").booleanValue()) {
			this.debugMode = LogMode.MQTT;
		} else if((udp = debugNode.get("udp")) != null && udp.get("addr").isNull() == false) {  // no "udp" on wall display ???
			this.debugMode = LogMode.UDP;
		} else {
			this.debugMode = LogMode.NONE;
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
	}

	@Override
	public void refreshStatus() throws IOException {
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	@Override
	public String[] getInfoRequests() {
		return new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients", "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents", "/rpc/BLE.CloudRelay.ListInfos"};
	}

	@Override
	public void reboot() throws IOException {
		getJSON("/rpc/Shelly.Reboot");
	}

	@Override
	public boolean setEcoMode(boolean eco) {
		return postCommand("Sys.SetConfig", "{\"config\":{\"device\":{\"eco_mode\":" + eco + "}}}") == null;
	}

	@Override
	public boolean setDebugMode(LogMode mode, boolean enable) {
		if(mode == LogMode.SOCKET) {
			return postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": " + (enable ? "true" : "false") + "}}}}") == null;
		} else if(mode == LogMode.MQTT) {
			return postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"mqtt\":{\"enable\": " + (enable ? "true" : "false") + "}}}}") == null;
		} else if(mode == LogMode.NONE) {
			return postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": false}, \"mqtt\":{\"enable\": false}, \"udp\":{\"addr\": null}} } }") == null;
		} else {
			return false;
		}
	}

	@Override
	public String setCloudEnabled(boolean enable) {
		String ret = postCommand("Cloud.SetConfig", "{\"config\":{\"enable\":" + enable + "}}");
		if(ret == null) {
			this.cloudEnabled = enable;
		}
		return ret;
	}

	public String setBLEMode(boolean ble) {
		return postCommand("BLE.SetConfig", "{\"config\":{\"enable\":" + ble + "}}");
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
	public TimeAndLocationManagerG2 getTimeAndLocationManager() throws IOException {
		return new TimeAndLocationManagerG2(this);
	}
	
	@Override
	public InputResetManager getInputResetManager() throws IOException {
		return new InputResetManagerG2(this);
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

	public JsonNode getJSON(final String method, JsonNode payload) throws IOException {
		return getJSON(method, jsonMapper.writeValueAsString(payload));
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
	
	/**
	 * example: <code> {
	 *  "items" : [ {"key" : "key", "etag" : "xxxyyy", "value" : "{}"} ],
	 *  "offset" : 0, "total" : 1
	 * } </code>
	 * @param method - e.g. /rpc/KVS.GetMany
	 * @param arrayKey - e.g. items
	 * @return an Iterator&lt;JsonNode&gt; navigating through pages
	 * @throws IOException
	 */
	public Iterator<JsonNode> getJSONIterator(final String method, final String arrayKey) throws IOException {
		return new PageIterator(this, method, arrayKey);
	}

	private JsonNode executeRPC(final String method, String payload) throws IOException, StreamReadException { // StreamReadException extends ... IOException
		try {
			ContentResponse response = httpClient.POST(uriPrefix + "/rpc")
					.body(new StringRequestContent("application/json", "{\"id\":1,\"method\":\"" + method + "\",\"params\":" + payload + "}", StandardCharsets.UTF_8))
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
			CompletableFuture<Session> s = wsClient.connect(listener, URI.create("ws://" + addressAndPort.getRepresentation() + "/rpc")); // this also do upgrade
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
		return wsClient.connect(listener, URI.create("ws://" + addressAndPort.getRepresentation() + "/debug/log"));
	}

	@Override
	public boolean backup(final Path file) throws IOException {
		Files.deleteIfExists(file);
		try(FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + file.toUri()), Map.of("create", "true"))) {
			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", fs);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			JsonNode config = sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", fs);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // unmanaged battery device
				sectionToStream("/rpc/Schedule.List", "Schedule.List.json", fs);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", fs);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try {
				sectionToStream("/rpc/KVS.GetMany", "items", "KVS.GetMany.json", fs);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			JsonNode scripts = null;
			try {
				scripts = sectionToStream("/rpc/Script.List", "Script.List.json", fs);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // Virtual components (PRO & gen3+)
				sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "components", "Shelly.GetComponents.json", fs);
			} catch(Exception e) {}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			String addon = config.get("sys").get("device").path("addon_type").asText();
			if(SensorAddOn.ADDON_TYPE.equals(addon)) {
				sectionToStream("/rpc/SensorAddon.GetPeripherals", SensorAddOn.BACKUP_SECTION, fs);
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			// Scripts
			if(scripts != null) {
				for(Script script: Script.list(this, scripts)) {
					try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath(script.getName() + ".mjs"))) {
						writer.write(script.getCode());
					} catch(IOException e) {
						LOG.error("backup script {}", script.getName(), e);
					}
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			}
			try { // Device specific
				backup(fs);
			} catch(Exception e) {
				LOG.error("backup specific", e);
			}
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}
	
//	/** implement for devices that need additional information */
//	protected void backup(ZipOutputStream out) throws IOException, InterruptedException {}
	
	protected void backup(FileSystem fs) throws IOException, InterruptedException {}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
		try {
			JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
			if(devInfo == null || this.getTypeID().equals(devInfo.get("app").asText()) == false) {
				res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			} else {
				JsonNode config = backupJsons.get("Shelly.GetConfig.json");
				boolean sameDevice = /*devInfo.get("id").asText("").equals(this.hostname)*/devInfo.get("mac").asText("").toUpperCase().equals(this.mac);
				if(sameDevice == false) {
					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, /*fileHostname*/devInfo.get("id").asText(""));
				}
				DynamicComponents.restoreCheck(this, backupJsons, res);
				if(devInfo.path("auth_en").asBoolean()) {
					res.put(RestoreMsg.RESTORE_LOGIN, LoginManagerG2.LOGIN_USER);
				}
				Network currentConnection = WIFIManagerG2.currentConnection(this);
				if(currentConnection != Network.UNKNOWN) {
					JsonNode wifi = config.at("/wifi/sta");
					if(wifi.path("enable").asBoolean() && (sameDevice || wifi.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.PRIMARY) {
						if(wifi.path("is_open").asBoolean() == false) {
							res.put(RestoreMsg.RESTORE_WI_FI1, wifi.path("ssid").asText());
						}
					}
					JsonNode wifi2 = config.at("/wifi/sta1");
					if(wifi2.path("enable").asBoolean() && (sameDevice || wifi2.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.SECONDARY) {
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
				if(config.at("/mqtt/enable").asBoolean() && config.at("/mqtt/user").asText("").isEmpty() == false) {
					res.put(RestoreMsg.RESTORE_MQTT, config.at("/mqtt/user").asText());
				}
				JsonNode storedScripts = backupJsons.get("Script.List.json");
				if(storedScripts != null && storedScripts.path("scripts").size() > 0) {
					List<Script> existingScripts = Script.list(this);
					List<String> scriptsEnabledByDefault = new ArrayList<>();
					List<String> scriptsWithSameName = new ArrayList<>();
					List<String> existingScriptsNames = existingScripts.stream().map(s -> s.getName()).toList();
					for(JsonNode jsonScript: storedScripts.get("scripts")) {
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
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> userPref) throws IOException {
		final ArrayList<String> errors = new ArrayList<>();
		try {
			final long delay = this instanceof BatteryDeviceInterface ? (Devices.MULTI_QUERY_DELAY / 2) : Devices.MULTI_QUERY_DELAY;

			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
			errors.add("->r_step:specific");
			restore(backupJsons, errors);
			if(status == Status.OFF_LINE) {
				return errors.size() > 0 ? errors : List.of(RestoreMsg.ERR_UNKNOWN.toString());
			}

			errors.add("->r_step:DynamicComponents");
			DynamicComponents.restore(this, backupJsons, errors); // only devices with same (existing) addr are restored; if a device is no more present, related  webooks will signal error(s)
			
			errors.add("->r_step:restoreCommonConfig");
			restoreCommonConfig(config, delay, userPref, errors);

			errors.add("->r_step:restoreSchedule");
			JsonNode schedule = backupJsons.get("Schedule.List.json");
			if(schedule != null) { // some devices do not have Schedule.List +H&T
				TimeUnit.MILLISECONDS.sleep(delay);
				ScheduleManager.restore(this, schedule, delay, errors);
			}

			errors.add("->r_step:Script");
			Script.restoreAll(this, backupJsons, delay, userPref.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE), userPref.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP), errors);

			errors.add("->r_step:KVS");
			JsonNode kvs = backupJsons.get("KVS.GetMany.json");
			if(kvs != null) {
				try {
					TimeUnit.MILLISECONDS.sleep(delay); // new KVS(,,,)
					KVS kvStore = new KVS(this);
					kvStore.restoreKVS(kvs, errors);
				} catch(Exception e) {}
			}
			
			errors.add("->r_step:Webhooks");
			Webhooks.restore(this, backupJsons.get("Webhook.List.json"), delay, errors);

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
				
				TimeUnit.MILLISECONDS.sleep(delay);
				JsonNode apNode = config.at("/wifi/ap"); // e.g. wall display -> isMissingNode()
				if(currentConnection != Network.AP && apNode.isMissingNode() == false &&
						((userPref.containsKey(RestoreMsg.RESTORE_WI_FI_AP) || apNode.path("is_open").asBoolean() || apNode.path("enable").asBoolean() == false))) {
					errors.add(WIFIManagerG2.restoreAP_roam(this, config.get("wifi"), userPref.get(RestoreMsg.RESTORE_WI_FI_AP)));
				} else {
					errors.add(WIFIManagerG2.restoreRoam(this, config.get("wifi")));
				}
			}
			
			errors.add("->r_step:LoginManagerG2");
			TimeUnit.MILLISECONDS.sleep(delay);
			LoginManagerG2 lm = new LoginManagerG2(this, true);
			if(userPref.containsKey(RestoreMsg.RESTORE_LOGIN)) {
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(lm.set(null, userPref.get(RestoreMsg.RESTORE_LOGIN).toCharArray()));
			} else if(backupJsons.get("Shelly.GetDeviceInfo.json").path("auth_en").asBoolean() == false) {
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(lm.disable());
			}
		} catch(RuntimeException | InterruptedException e) {
			LOG.error("restore - RuntimeException", e);
			errors.add(RestoreMsg.ERR_UNKNOWN.toString());
		}
		return errors;
	}

	// Shelly.GetConfig.json
	protected void restoreCommonConfig(JsonNode config, final long delay, Map<RestoreMsg, String> userPref, List<String> errors) throws InterruptedException, IOException {
		ObjectNode outConfig = JsonNodeFactory.instance.objectNode();

		// BLE.SetConfig
		outConfig.set("config", config.get("ble")/*.deepCopy()*/);
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
		ObjectNode outSys = JsonNodeFactory.instance.objectNode();
		
		ObjectNode outDevice = (ObjectNode)sys.get("device")/*.deepCopy()*/; // todo test (anche caso name = null)
		outDevice.remove("mac");
		outDevice.remove("fw_id");
		outDevice.remove("addon_type");
		outDevice.remove("profile"); // postCommand("Shelly.setprofile", "{\"name\":\"" + shelly.get("profile") +"\"}");
		outSys.set("device", outDevice);

		outSys.set("sntp", sys.get("sntp")/*.deepCopy()*/);
		
		outSys.set("debug", sys.get("debug"));

		outConfig.set("config", outSys);
		TimeUnit.MILLISECONDS.sleep(delay);
		errors.add(postCommand("Sys.SetConfig", outConfig));
		
		JsonNode matter = config.get("matter");
		if(matter != null) {
			outConfig.set("config", matter/*.deepCopy()*/);
			TimeUnit.MILLISECONDS.sleep(delay);
			errors.add(postCommand("Matter.SetConfig", outConfig));
		}
		
		final JsonNode mqtt = config.path("mqtt");
		if(userPref.containsKey(RestoreMsg.RESTORE_MQTT) || mqtt.path("enable").asBoolean() == false || mqtt.path("user").asText("").isEmpty()) {
			TimeUnit.MILLISECONDS.sleep(delay);
			errors.add(MQTTManagerG2.restore(this, mqtt, userPref.get(RestoreMsg.RESTORE_MQTT)));
		}
	}
	
	public static ObjectNode createIndexedRestoreNode(JsonNode backConfig, String type, int index) { // todo addon, input, switch
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", index);
		ObjectNode data = backConfig.get(type + ":" + index).deepCopy();
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
} // 477 - 474 - 525 - 568 - 637