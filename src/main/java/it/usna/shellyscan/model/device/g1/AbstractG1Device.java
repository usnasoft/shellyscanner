package it.usna.shellyscan.model.device.g1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.Authentication;
import org.eclipse.jetty.client.AuthenticationStore;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;
import it.usna.shellyscan.model.device.WIFIManager.Network;
import it.usna.shellyscan.model.device.g1.modules.Actions;

/**
 * Base class for any gen1 Shelly device
 * usna
 */
public abstract class AbstractG1Device extends ShellyAbstractDevice {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractG1Device.class);

	protected AbstractG1Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	public void init(HttpClient httpClient, JsonNode shelly) throws IOException {
		this.httpClient = httpClient;
		this.mac = shelly.get("mac").asText().toUpperCase();
		init();
	}
	
	protected void init() throws IOException {
		JsonNode settings = getJSON("/settings");
		this.hostname = settings.get("device").get("hostname").asText("");
		fillSettings(settings);
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		fillStatus(getJSON("/status"));
	}

	public void setAuthenticationResult(Authentication.Result auth) {
		AuthenticationStore store = httpClient.getAuthenticationStore();
		Authentication.Result ar = store.findAuthenticationResult(URI.create(uriPrefix));
		if(ar != null) {
			store.removeAuthenticationResult(ar);
		}
		if(auth != null) {
			store.addAuthenticationResult(auth);
		}
	}
	
	protected void fillSettings(JsonNode settings) throws IOException {
		this.name = settings.path("name").asText("");
		JsonNode dubugNode;
		if((dubugNode = settings.get("debug_enable")) != null) {
			this.debugEnabled = dubugNode.booleanValue() ? LogMode.FILE : LogMode.NO; // missing in flood (20201128-102432/v1.9.2@e83f7025)
		} else {
			this.debugEnabled = LogMode.UNDEFINED;
		}
		this.mqttEnabled = settings.path("mqtt").path("enable").booleanValue();
	}

	protected void fillStatus(JsonNode status) throws IOException {
		final JsonNode cloud = status.get("cloud");
		this.cloudEnabled = cloud.get("enabled").booleanValue();
		this.cloudConnected = cloud.get("connected").booleanValue();
		final JsonNode wifi = status.path("wifi_sta");
		this.rssi = wifi.path("rssi").asInt(0);
		this.ssid = wifi.path("ssid").asText("");
		this.uptime = status.get("uptime").asInt();
		this.mqttConnected = status.path("mqtt").path("connected").booleanValue();
		
		lastConnection = System.currentTimeMillis();
	}
	
	public String sendCommand(final String command) {
		try {
			ContentResponse response = httpClient.GET(uriPrefix + command);
			int statusCode = response.getStatus();
			if(statusCode == HttpStatus.OK_200) {
				status = Status.ON_LINE;
			} else if(statusCode == HttpStatus.UNAUTHORIZED_401) {
				status = Status.NOT_LOOGGED;
			} else /*if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR_500)*/ {
				status = Status.ERROR;
			}
			String ret = response.getContentAsString();
			return (ret == null || ret.length() == 0 || ret/*.trim()*/.startsWith("{")) ? null : ret;
		} catch(ExecutionException | RuntimeException e) {
			return e.getMessage();
		} catch (TimeoutException | InterruptedException e) {
			status = Status.OFF_LINE;
			return "Status-OFFLINE";
		}
	}

	@Override
	public void refreshSettings() throws IOException {
		fillSettings(getJSON("/settings"));
	}
	
	@Override
	public void refreshStatus() throws IOException {
		fillStatus(getJSON("/status"));
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {"/shelly", "/settings", "/settings/actions", "/status"}; // "settings/ap", "settings/sta", "settings/login", "settings/cloud" into "settings"; "/ota" into "status"
	}
	
	@Override
	public void reboot() /*throws IOException*/ {
		if(sendCommand("/reboot") == null) {
			rebootRequired = false;
		}
	}
	
	@Override
	public void setEcoMode(boolean eco) {
		if(sendCommand("/settings?eco_mode_enabled=" + eco) == null) {
			rebootRequired = true;
		}
	}
	
	/** not all devices accept this command */
	public void setLEDMode(boolean on) {
		sendCommand("/settings?led_status_disable=" + on);
	}
	
	public void setDebugMode(LogMode mode) {
		try {
			this.debugEnabled = getJSON("/settings?debug_enable=" + (mode != LogMode.NO)).path("debug_enable").asBoolean(false) ? LogMode.FILE : LogMode.NO;
		} catch (IOException e) {
			LOG.warn("setDebugMode: {}", mode, e);
		}
	}
	
	@Override
	public FirmwareManager getFWManager() {
		return new FirmwareManagerG1(this);
	}
	
	@Override
	public LoginManager getLoginManager() throws IOException {
		return new LoginManagerG1(this);
	}
	
	@Override
	public WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException {
		return new WIFIManagerG1(this, net);
	}
	
	@Override
	public MQTTManagerG1 getMQTTManager() throws IOException {
		return new MQTTManagerG1(this);
	}
	
	@Override
	public boolean backup(final File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			sectionToStream("/settings", "settings.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/settings/actions", "actions.json", out);
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}
	
	@Override
	public final Map<Restore, String> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		EnumMap<Restore, String> res = new EnumMap<>(Restore.class);
		try {
			JsonNode settings = backupJsons.get("settings.json");
			final String fileHostname = settings.get("device").get("hostname").asText("");
			final String fileType = settings.get("device").get("type").asText();
			if(fileType.length() > 0 && fileType.equals(this.getTypeID()) == false) {
				res.put(Restore.ERR_RESTORE_MODEL, null);
			} else {
				boolean sameHost = fileHostname.equals(this.hostname);
				if(sameHost == false) {
					res.put(Restore.ERR_RESTORE_HOST, fileHostname);
				}
				if(settings.at("/login/enabled").asBoolean()) {
					res.put(Restore.RESTORE_LOGIN, settings.at("/login/username").asText());
				}
				Network currentConnection = WIFIManagerG1.currentConnection(this);
				if(settings.at("/wifi_sta/enabled").asBoolean() && (sameHost || settings.at("/wifi_sta/ipv4_method").asText().equals("dhcp")) && currentConnection != Network.PRIMARY) {
					res.put(Restore.RESTORE_WI_FI1, settings.at("/wifi_sta/ssid").asText());
				}
				if(settings.at("/wifi_sta1/enabled").asBoolean() && (sameHost || settings.at("/wifi_sta1/ipv4_method").asText().equals("dhcp")) && currentConnection != Network.SECONDARY) {
					res.put(Restore.RESTORE_WI_FI2, settings.at("/wifi_sta1/ssid").asText());
				}
				if(settings.at("/mqtt/enable").asBoolean() && settings.at("/mqtt/user").asText("").length() > 0) {
					res.put(Restore.RESTORE_MQTT, settings.at("/mqtt/user").asText());
				}
			}
		} catch(RuntimeException e) {
			LOG.error("restoreCheck", e);
			res.put(Restore.ERR_RESTORE_MODEL, null);
		}
		return res;
	}
	
	@Override
	public final String restore(Map<String, JsonNode> backupJsons, Map<Restore, String> data) throws IOException {
		try {
			final ArrayList<String> errors = new ArrayList<>();
			JsonNode settings = backupJsons.get("settings.json");
			JsonNode actions = backupJsons.get("actions.json");
						
			restore(settings, errors);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			restoreCommons(settings, data, errors);

			Actions.restore(this, actions, errors);

			JsonNode roam = settings.path("ap_roaming");
			if(roam.isMissingNode() == false) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(WIFIManagerG1.restoreRoam(this, roam));
			}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			Network currentConnection = WIFIManagerG1.currentConnection(this);
			JsonNode sta1 = settings.path("wifi_sta1"); // warning: motion doesn't have wi-fi2
			if(currentConnection != Network.SECONDARY && sta1.isMissingNode() == false && (data.containsKey(Restore.RESTORE_WI_FI2) || sta1.path("enabled").asBoolean() == false)) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				WIFIManagerG1 wm2 = new WIFIManagerG1(this, Network.SECONDARY, true);
				errors.add(wm2.restore(settings.path("wifi_sta1"), data.get(Restore.RESTORE_WI_FI2)));
			}
			// last - hereafter we loose connection
			if(currentConnection != Network.PRIMARY && (data.containsKey(Restore.RESTORE_WI_FI1) || settings.at("/wifi_sta/enabled").asBoolean() == false)) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				WIFIManagerG1 wm1 = new WIFIManagerG1(this, Network.PRIMARY, true);
				errors.add(wm1.restore(settings.path("wifi_sta"), data.get(Restore.RESTORE_WI_FI1)));
			}
			final String ret = errors.stream().filter(s-> s != null && s.length() > 0).collect(Collectors.joining("\n"));
			if(ret.length() > 0) {
				LOG.error("Restore error {} {}", this, errors);
			}
			return ret;
		} catch(RuntimeException | InterruptedException e) {
			LOG.error("restore", e);
			return Restore.ERR_UNKNOWN.toString();
		}
	}

	/**
	 * tz_dst - intentionally ignored (depends by backup date)
	 * debug_enable - intentionally ignored
	 * allow_cross_origin - intentionally ignored
	 * Return errors List
	 */
	protected abstract void restore(JsonNode settings, ArrayList<String> errors) throws IOException, InterruptedException;

	private void restoreCommons(JsonNode settings, Map<Restore, String> data, ArrayList<String> errors) throws InterruptedException, IOException {
		errors.add(sendCommand("/settings/cloud?enabled=" + settings.get("cloud").get("enabled").asText()));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings?" +
				jsonNodeToURLPar(settings, "name", "discoverable", "timezone", "lat", "lng", "tzautodetect", "tz_utc_offset", /*"tz_dst",*/ "tz_dst_auto", "tz_dst_auto", "allow_cross_origin")));
		// eco_mode_enabled=" + settings.get("eco_mode_enabled") // no way: device reboot changing this parameter
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		LoginManagerG1 lm = new LoginManagerG1(this, true);
		if(data.containsKey(Restore.RESTORE_LOGIN)) {
			errors.add(lm.set(settings.at("/login/username").asText(""), data.get(Restore.RESTORE_LOGIN).toCharArray()));
		} else if(settings.at("/login/enabled").asBoolean() == false) {
			errors.add(lm.disable());
		}
		final JsonNode mqtt = settings.get("mqtt");
		if(data.containsKey(Restore.RESTORE_MQTT) || mqtt.path("enable").asBoolean() == false || mqtt.path("user").asText("").length() == 0) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			MQTTManagerG1 mqttM = new MQTTManagerG1(this, true);
			errors.add(mqttM.restore(mqtt, data.get(Restore.RESTORE_MQTT)));
		}
	}

	public static String jsonEntryToURLPar(Entry<String, JsonNode> jsonEntry) throws UnsupportedEncodingException {
		final String name = jsonEntry.getKey();
		final JsonNode val = jsonEntry.getValue();
		if(val.isArray()) {
			String res;
			if(val.size() > 0) {
				res = name + "[]=" + URLEncoder.encode(val.get(0).asText(), StandardCharsets.UTF_8.name());
				for(int i=1; i < val.size(); i++) {
					res += "&" + name + "[]=" + URLEncoder.encode(val.get(i).asText(), StandardCharsets.UTF_8.name());
				}
			} else {
				res = name + "[]=";
			}
			return res;
		} else {
			return name + "=" + URLEncoder.encode(val.asText(), StandardCharsets.UTF_8.name());
		}
	}
	
	public static String jsonEntryIteratorToURLPar(Iterator<Entry<String, JsonNode>> pars) throws UnsupportedEncodingException {
		if(pars.hasNext()) {
			String command = AbstractG1Device.jsonEntryToURLPar(pars.next());
			while(pars.hasNext()) {
				command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
			}
			return command;
		}
		return "";
	}

	public static String jsonNodeToURLPar(JsonNode jNode, String ... pars) throws UnsupportedEncodingException {
		String res = pars[0] + "=" +  URLEncoder.encode(jNode.get(pars[0]).asText(), StandardCharsets.UTF_8.name());
		for(int i = 1; i < pars.length; i++) {
			JsonNode thisNode = jNode.get(pars[i]);
			if(thisNode != null && thisNode instanceof NullNode == false) {
				res += "&" + pars[i] + "=" + URLEncoder.encode(thisNode.asText(), StandardCharsets.UTF_8.name());
			} else {
				res += "&" + pars[i] + "=";
			}
		}
		return res;
	}
}