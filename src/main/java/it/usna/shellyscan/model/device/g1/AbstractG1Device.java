package it.usna.shellyscan.model.device.g1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.modules.Actions;
import it.usna.shellyscan.model.device.g1.modules.FirmwareManagerG1;
import it.usna.shellyscan.model.device.g1.modules.InputResetManagerG1;
import it.usna.shellyscan.model.device.g1.modules.LoginManagerG1;
import it.usna.shellyscan.model.device.g1.modules.MQTTManagerG1;
import it.usna.shellyscan.model.device.g1.modules.TimeAndLocationManagerG1;
import it.usna.shellyscan.model.device.g1.modules.WIFIManagerG1;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

/**
 * Base class for any gen1 Shelly device
 * @author usna
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
			return (ret == null || ret.isEmpty() || ret/*.trim()*/.startsWith("{")) ? null : ret;
		} catch(InterruptedException | TimeoutException e) {
			status = Status.OFF_LINE;
			return "Status-OFFLINE";
		} catch(ExecutionException | RuntimeException e) {
			if(e.getCause() instanceof SocketTimeoutException || e.getCause() instanceof TimeoutException) {
				status = Status.OFF_LINE;
				return "Status-OFFLINE";
			} else {
				LOG.warn("EX", e);
				return e.getMessage();
			}
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
		return new String[] {"/shelly", "/settings", "/settings/actions", "/status" /*, "/cit/d"*/}; // "settings/ap", "settings/sta", "settings/login", "settings/cloud" into "settings"; "/ota" into "status"
	}
	
	@Override
	public void reboot() /*throws IOException*/ {
		if(sendCommand("/reboot") == null) {
			rebootRequired = false;
		}
	}
	
	@Override
	public boolean setEcoMode(boolean eco) {
		if(sendCommand("/settings?eco_mode_enabled=" + eco) == null) {
			rebootRequired = true;
			return true;
		} else {
			return true;
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
	public String setCloudEnabled(boolean enable) {
		String ret = sendCommand("/settings/cloud?enabled=" + enable);
		if(ret == null) {
			this.cloudEnabled = enable;
		}
		return ret;
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
	public TimeAndLocationManager getTimeAndLocationManager() throws IOException {
		return new TimeAndLocationManagerG1(this);
	}
	
	@Override
	public InputResetManager getInputResetManager() throws IOException {
		return new InputResetManagerG1(this);
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
	public final Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
		try {
			JsonNode settings = backupJsons.get("settings.json");
			final String fileHostname = settings.get("device").get("hostname").asText("");
			final String fileType = settings.get("device").get("type").asText();
			if(fileType.length() > 0 && fileType.equals(this.getTypeID()) == false) {
				res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			} else {
				boolean sameHost = fileHostname.equals(this.hostname);
				if(sameHost == false) {
					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, fileHostname);
				}
				if(settings.at("/login/enabled").asBoolean()) {
					res.put(RestoreMsg.RESTORE_LOGIN, settings.at("/login/username").asText());
				}
				Network currentConnection = WIFIManagerG1.currentConnection(this);
				if(currentConnection != Network.UNKNOWN) {
					if(settings.at("/wifi_sta/enabled").asBoolean() && (sameHost || settings.at("/wifi_sta/ipv4_method").asText().equals("dhcp")) && currentConnection != Network.PRIMARY) {
						res.put(RestoreMsg.RESTORE_WI_FI1, settings.at("/wifi_sta/ssid").asText());
					}
					if(settings.at("/wifi_sta1/enabled").asBoolean() && (sameHost || settings.at("/wifi_sta1/ipv4_method").asText().equals("dhcp")) && currentConnection != Network.SECONDARY) {
						res.put(RestoreMsg.RESTORE_WI_FI2, settings.at("/wifi_sta1/ssid").asText());
					}
				}
				if(settings.at("/mqtt/enable").asBoolean() && settings.at("/mqtt/user").asText("").length() > 0) {
					res.put(RestoreMsg.RESTORE_MQTT, settings.at("/mqtt/user").asText());
				}
			}
		} catch(RuntimeException e) {
			LOG.error("restoreCheck", e);
			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
		}
		return res;
	}
	
	@Override
	public final List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) throws IOException {
		final ArrayList<String> errors = new ArrayList<>();
		try {
			final long delay = this instanceof BatteryDeviceInterface ? Devices.MULTI_QUERY_DELAY / 2: Devices.MULTI_QUERY_DELAY;
			JsonNode settings = backupJsons.get("settings.json");
			JsonNode actions = backupJsons.get("actions.json");
			LOG.trace("step 1");
			restore(settings, errors);
			if(status == Status.OFF_LINE) {
				return errors.size() > 0 ? errors : List.of(RestoreMsg.ERR_UNKNOWN.name());
			}
			TimeUnit.MILLISECONDS.sleep(delay);
			LOG.trace("step 2 {}", errors);
			restoreCommons(settings, delay, data, errors);

			Actions.restore(this, actions, delay, errors);
			LOG.trace("step 3 {}", errors);
			JsonNode roam = settings.path("ap_roaming");
			if(roam.isMissingNode() == false) {
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(WIFIManagerG1.restoreRoam(this, roam));
			}
			LOG.trace("step 4 {}", errors);
			TimeUnit.MILLISECONDS.sleep(delay);
			Network currentConnection = WIFIManagerG1.currentConnection(this);
			JsonNode sta1 = settings.path("wifi_sta1"); // "sta1.isMissingNode() == false": motion doesn't have wi-fi2
			if(currentConnection != Network.SECONDARY && sta1.isMissingNode() == false && (data.containsKey(RestoreMsg.RESTORE_WI_FI2) || sta1.path("enabled").asBoolean() == false)) {
				TimeUnit.MILLISECONDS.sleep(delay);
				WIFIManagerG1 wm2 = new WIFIManagerG1(this, Network.SECONDARY, true);
				errors.add(wm2.restore(settings.path("wifi_sta1"), data.get(RestoreMsg.RESTORE_WI_FI2)));
			}
			// last - hereafter we loose connection
			if(currentConnection != Network.PRIMARY && (data.containsKey(RestoreMsg.RESTORE_WI_FI1) || settings.at("/wifi_sta/enabled").asBoolean() == false)) {
				TimeUnit.MILLISECONDS.sleep(delay);
				WIFIManagerG1 wm1 = new WIFIManagerG1(this, Network.PRIMARY, true);
				errors.add(wm1.restore(settings.path("wifi_sta"), data.get(RestoreMsg.RESTORE_WI_FI1)));
			}
			LOG.trace("restore end {}", errors);
		} catch(RuntimeException | InterruptedException e) {
			LOG.error("restore - RuntimeException", e);
			errors.add(RestoreMsg.ERR_UNKNOWN.toString());
		}
		return errors;
	}

	protected abstract void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException;

	/**
	 * tz_dst - intentionally ignored (depends by backup date)
	 * debug_enable - intentionally ignored
	 * Return errors List
	 */
	private void restoreCommons(JsonNode settings, final long delay, Map<RestoreMsg, String> data, ArrayList<String> errors) throws InterruptedException, IOException {
		errors.add(sendCommand("/settings/cloud?enabled=" + settings.get("cloud").get("enabled").asText()));
//		LOG.trace("step 2.1");
		final String[] settigsRestore;
		if(settings.get("pon_wifi_reset") == null) {
			settigsRestore = new String[] {"name", "discoverable", "timezone", "lat", "lng", "tzautodetect", "tz_utc_offset", "tz_dst_auto", "tz_dst_auto", "allow_cross_origin"};
		} else {
			settigsRestore = new String[] {"name", "discoverable", "timezone", "lat", "lng", "tzautodetect", "tz_utc_offset", "tz_dst_auto", "tz_dst_auto", "allow_cross_origin", "pon_wifi_reset"};
		}
		String coiotSettings = "";
		if(settings.get("coiot") != null) {
			coiotSettings = "&coiot_enable=" + settings.at("/coiot/enabled").asText() +
					"&coiot_update_period=" + settings.at("/coiot/update_period").asText() +
					"&coiot_peer=" + URLEncoder.encode(settings.at("/coiot/peer").asText(), StandardCharsets.UTF_8.name());
		}
		String sntpSetting = "";
		JsonNode sntpNode = settings.at("/sntp/server");
		if(sntpNode.isMissingNode() == false) {
			sntpSetting = "&sntp_server=" + sntpNode.asText();
		}
		TimeUnit.MILLISECONDS.sleep(delay);
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, settigsRestore) + coiotSettings + sntpSetting));
		// eco_mode_enabled=" + settings.get("eco_mode_enabled") // no way: device reboot changing this parameter
		TimeUnit.MILLISECONDS.sleep(delay);
		LoginManagerG1 lm = new LoginManagerG1(this, true);
//		LOG.trace("step 2.2");
		if(data.containsKey(RestoreMsg.RESTORE_LOGIN)) {
			errors.add(lm.set(settings.at("/login/username").asText(""), data.get(RestoreMsg.RESTORE_LOGIN).toCharArray()));
		} else if(settings.at("/login/enabled").asBoolean() == false) {
			errors.add(lm.disable());
		}
//		LOG.trace("step 2.3");
		final JsonNode mqtt = settings.get("mqtt");
		if(data.containsKey(RestoreMsg.RESTORE_MQTT) || mqtt.path("enable").asBoolean() == false || mqtt.path("user").asText("").length() == 0) {
			TimeUnit.MILLISECONDS.sleep(delay);
			MQTTManagerG1 mqttM = new MQTTManagerG1(this, true);
			errors.add(mqttM.restore(mqtt, data.get(RestoreMsg.RESTORE_MQTT)));
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