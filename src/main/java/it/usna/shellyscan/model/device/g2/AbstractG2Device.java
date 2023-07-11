package it.usna.shellyscan.model.device.g2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.DigestAuthentication;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;
import it.usna.shellyscan.model.device.WIFIManager.Network;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;

public abstract class AbstractG2Device extends ShellyAbstractDevice {
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
		// fillOnce
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		
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

		JsonNode debugNode = sysNode.path("debug");
		if(debugNode.path("websocket").path("enable").asBoolean()) {
			this.debugEnabled = LogMode.SOCKET;
		} else if(debugNode.path("mqtt").path("enable").asBoolean()) {
			this.debugEnabled = LogMode.MQTT;
		} else if(debugNode.path("udp").path("addr").isNull() == false) {
			this.debugEnabled = LogMode.UDP;
		} else {
			this.debugEnabled = LogMode.NO;
		}
		
		this.cloudEnabled = config.path("cloud").path("enable").asBoolean();
		this.mqttEnabled = config.path("mqtt").path("enable").asBoolean();
		
		this.rangeExtender = config.get("wifi").get("ap").path("range_extender").path("enable").asBoolean();
	}
	
	protected void fillStatus(JsonNode status) throws IOException {
		this.cloudConnected = status.path("cloud").path("connected").asBoolean();
		JsonNode wifiNode = status.get("wifi");
		this.rssi = wifiNode.path("rssi").asInt();
		this.ssid = wifiNode.path("ssid").asText();
		JsonNode sysNode = status.get("sys");
		this.uptime = sysNode.get("uptime").asInt();
		this.rebootRequired = sysNode.path("restart_required").asBoolean();
		this.mqttConnected = status.path("mqtt").path("connected").asBoolean();
		
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
				"/rpc/Shelly.GetDeviceInfo", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus"*/, "/rpc/KVS.List"};
	}
	
	@Override
	public void reboot() throws IOException {
		getJSON("/rpc/Shelly.Reboot");
	}
	
	@Override
	public void setEcoMode(boolean eco) {
		postCommand("Sys.SetConfig", "{\"config\":{\"device\":{\"eco_mode\":" + eco + "}}}");
	}
	
//	public void setDebugMode(LogMode mode) {
//		postCommand("Sys.SetConfig", "{\"config\": {\"debug\":{\"websocket\":{\"enable\": " + (LogMode.SOCKET == mode ? "true" : "false") + "}}}");
//	}
	
	public void setBLEMode(boolean ble) {
		postCommand("BLE.SetConfig", "{\"config\":{\"enable\":" + ble + "}}");
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
			if((error = resp.get("error")) == null) { // todo {"id":1,"src":"shellyplusi4-xxx","result":{"restart_required":true}}
				if(resp.path("result").path("restart_required").asBoolean(false)) {
					rebootRequired = true;
				}
				if(status == Status.NOT_LOOGGED) {
					return "Status-PROTECTED";
				}
				return null;
			} else {
				return error.path("message").asText("Generic error");
			}
		} catch(IOException e) {
//			status = Status.OFF_LINE;
			return "Status-OFFLINE";
		} catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	
	public JsonNode getJSON(final String method, String payload) throws IOException {
		final JsonNode resp = executeRPC(method, payload);
		JsonNode result;
		if((result = resp.get("result")) != null) {
			return result;
		} else {
			JsonNode error = resp.get("error");
			throw new IOException(error.path("code") + ": " + error.path("message").asText("Generic error"));
		}	
	}
	
	private JsonNode executeRPC(final String method, String payload) throws IOException, StreamReadException { // StreamReadException extends ... IOException
		try {
			ContentResponse response = httpClient.POST(uriPrefix + "/rpc")
					.content(new StringContentProvider("{\"id\":1, \"method\":\"" + method + "\", \"params\":" + payload + "}", StandardCharsets.UTF_8))
					.send();
			int statusCode = response.getStatus(); //response.getContentAsString()
			if(statusCode == HttpStatus.OK_200) {
				status = Status.ON_LINE;
			} else if(statusCode == HttpStatus.UNAUTHORIZED_401) {
				status = Status.NOT_LOOGGED;
			} else /*if(statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR || statusCode == HttpURLConnection.HTTP_BAD_REQUEST)*/ {
				status = Status.ERROR;
			}
			return jsonMapper.readTree(response.getContent());
		} catch(InterruptedException | ExecutionException | TimeoutException e) {
			status = Status.OFF_LINE;
			throw new DeviceOfflineException(e);
		}
	}
	
	public Future<Session> connectWebSocketClient(WebSocketListener listener/*, boolean activate*/) throws IOException, InterruptedException, ExecutionException {
		final Future<Session> s = wsClient.connect(listener, URI.create("ws://" + address.getHostAddress() + ":" + port + "/rpc"));
		s.get().getRemote().sendStringByFuture("{\"id\":2, \"src\":\"S_Scanner\", \"method\":\"Shelly.GetDeviceInfo\"}");
		return s;
	}
	
	public Future<Session> connectWebSocketLogs(WebSocketListener listener) throws IOException, InterruptedException, ExecutionException {
		return wsClient.connect(listener, URI.create("ws://" + address.getHostAddress() + ":" + port + "/debug/log"));
	}
		
	@Override
	public boolean backup(final File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Schedule.List", "Schedule.List.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			final byte[] scripts = sectionToStream("/rpc/Script.List", "Script.List.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try { // On device with active sensor add-on
				sectionToStream("/rpc/SensorAddon.GetPeripherals", SensorAddOn.BACKUP_SECTION, out);
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			} catch(Exception e) {}
			// Scripts
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
		} catch(InterruptedException e) {
			LOG.error("", e);
		}
		return true;
	}

	@Override
	public Map<Restore, String> restoreCheck(/*final File file*/Map<String, JsonNode> backupJsons) throws IOException {
		HashMap<Restore, String> res = new HashMap<>();
		try /*(   ZipFile in = new ZipFile(file, StandardCharsets.UTF_8);
				InputStream isDevInfo = in.getInputStream(in.getEntry("Shelly.GetDeviceInfo.json"));
				InputStream isConfig = in.getInputStream(in.getEntry("Shelly.GetConfig.json"));
				)*/ {
//			JsonNode devInfo = jsonMapper.readTree(isDevInfo);
//			JsonNode config = jsonMapper.readTree(isConfig);
			JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
			final String fileHostname = devInfo.get("id").asText("");
			final String fileType = devInfo.get("app").asText();
			if(this.getTypeID().equals(fileType) == false) {
				res.put(Restore.ERR_RESTORE_MODEL, null);
			} else {
				boolean sameHost = fileHostname.equals(this.hostname);
				if(sameHost == false) {
					res.put(Restore.ERR_RESTORE_HOST, null);
				}
				if(devInfo.path("auth_en").asBoolean()) {
					res.put(Restore.RESTORE_LOGIN, LoginManagerG2.LOGIN_USER);
				}
				Network currentConnection = WIFIManagerG2.currentConnection(this);
				JsonNode wifi = config.at("/wifi/sta");
				if(wifi.path("enable").asBoolean() && (sameHost || wifi.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.PRIMARY) {
					if(wifi.path("is_open").asBoolean() == false) {
						res.put(Restore.RESTORE_WI_FI1, wifi.path("ssid").asText());
					}
				}
				JsonNode wifi2 = config.at("/wifi/sta1");
				if(wifi2.path("enable").asBoolean() && (sameHost || wifi2.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.SECONDARY) {
					if(wifi2.path("is_open").asBoolean() == false) {
						res.put(Restore.RESTORE_WI_FI2, wifi2.path("ssid").asText());
					}
				}
				JsonNode wifiAP = config.at("/wifi/ap");
				if(wifiAP.path("enable").asBoolean() && currentConnection != Network.AP) {
					if(wifiAP.path("is_open").asBoolean() == false) {
						res.put(Restore.RESTORE_WI_FI_AP, wifiAP.path("ssid").asText());
					}
				}
				
				if(config.at("/mqtt/enable").asBoolean() && config.at("/mqtt/user").asText("").length() > 0) {
					res.put(Restore.RESTORE_MQTT, config.at("/mqtt/user").asText());
				}
				// device specific
				restoreCheck(backupJsons, res);
			}
		} catch(RuntimeException e) {
			LOG.error("restoreCheck", e);
			res.put(Restore.ERR_RESTORE_MODEL, null);
		}
		return res;
	}
	
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<Restore, String> res) throws IOException {}
	
	@Override
	public final String restore(Map<String, JsonNode> backupJsons, Map<Restore, String> data) throws IOException {
		try {
			final ArrayList<String> errors = new ArrayList<>();
			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
			restore(backupJsons, errors);
			restoreCommonConfig(config, data, errors);
			JsonNode schedule = backupJsons.get("Schedule.List.json");
			if(schedule != null) {  // some devices do not have Schedule.List +H&T
				restoreSchedule(schedule, errors);
			}
			Webhooks.restore(this, backupJsons.get("Webhook.List.json"), errors);

			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			LoginManagerG2 lm = new LoginManagerG2(this, true);
			if(data.containsKey(Restore.RESTORE_LOGIN)) {
				errors.add(lm.set(null, data.get(Restore.RESTORE_LOGIN).toCharArray()));
			} else if(backupJsons.get("Shelly.GetDeviceInfo.json").path("auth_en").asBoolean() == false) {
				errors.add(lm.disable());
			}
			
			Network currentConnection = WIFIManagerG2.currentConnection(this);
			if((data.containsKey(Restore.RESTORE_WI_FI2) || config.at("/wifi/sta1/is_open").asBoolean() || config.at("/wifi/sta1/enable").asBoolean() == false) && currentConnection != Network.SECONDARY) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				WIFIManagerG2 wm = new WIFIManagerG2(this, Network.SECONDARY, true);
				errors.add(wm.restore(config.at("/wifi/sta1"), data.get(Restore.RESTORE_WI_FI2)));
			}
			if((data.containsKey(Restore.RESTORE_WI_FI1) || config.at("/wifi/sta/is_open").asBoolean() || config.at("/wifi/sta/enable").asBoolean() == false) && currentConnection != Network.PRIMARY) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				WIFIManagerG2 wm = new WIFIManagerG2(this, Network.PRIMARY, true);
				errors.add(wm.restore(config.at("/wifi/sta"), data.get(Restore.RESTORE_WI_FI1)));
			}
			if((data.containsKey(Restore.RESTORE_WI_FI_AP) || config.at("/wifi/ap/is_open").asBoolean() || config.at("/wifi/ap/enable").asBoolean() == false) && currentConnection != Network.AP) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(WIFIManagerG2.restoreAP_roam(this, config.get("wifi"), data.get(Restore.RESTORE_WI_FI_AP)));
			}
			final String ret = errors.stream().filter(s-> s != null && s.length() > 0).collect(Collectors.joining("; "));
			if(ret.length() > 0) {
				LOG.error("Restore error {} {}", this, errors);
			}
			return ret;
		} catch(RuntimeException | InterruptedException e) {
			LOG.error("restore", e);
			return Restore.ERR_UNKNOWN.toString();
		}
	}
	
	protected abstract void restore(Map<String, JsonNode> backupJsons, ArrayList<String> errors) throws IOException, InterruptedException;
	
	//curl -X POST -d '{"id": 1, "method": "Sys.SetConfig", "params": {"config": {"location": {"tz": "Europe/Sofia"}}}}' http://${SHELLY}/rpc
	void restoreCommonConfig(JsonNode config, Map<Restore, String> data, ArrayList<String> errors) throws InterruptedException, IOException {
		JsonNodeFactory factory = new JsonNodeFactory(false);
		ObjectNode outConfig = factory.objectNode();
		
		// BLE.SetConfig
		outConfig.set("config", config.get("ble").deepCopy());
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("BLE.SetConfig", outConfig));
		
		// Cloud.SetConfig
		ObjectNode outCloud = factory.objectNode(); // Cloud // https://shelly-api-docs.shelly.cloud/gen2/Components/SystemComponents/Cloud#cloudsetconfig-example
		outCloud.put("enable", config.at("/cloud/enable").asBoolean());
		outConfig.set("config", outCloud);
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Cloud.SetConfig", outConfig));
		
		// Sys.SetConfig
		JsonNode sys = config.get("sys");
		
		JsonNode name = sys.at("/device/name"); // Device name
		ObjectNode outDevice = factory.objectNode();
		outDevice.put("name", name.asText("")); // does not appreciate null
		ObjectNode outSys = factory.objectNode();
		outSys.set("device", outDevice);
		
		outSys.set("sntp", sys.get("sntp").deepCopy());

		outConfig.set("config", outSys);
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Sys.SetConfig", outConfig));

		final JsonNode mqtt = config.path("mqtt");
		if(data.containsKey(Restore.RESTORE_MQTT) || mqtt.path("enable").asBoolean() == false || mqtt.path("user").asText("").length() == 0) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			MQTTManagerG2 mqttM = new MQTTManagerG2(this, true);
			errors.add(mqttM.restore(mqtt, data.get(Restore.RESTORE_MQTT)));
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
} // 457