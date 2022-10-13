package it.usna.shellyscan.model.device.g1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;
import it.usna.shellyscan.model.device.g1.modules.Actions;

public abstract class AbstractG1Device extends ShellyAbstractDevice {
	private final static JsonPointer RSSI = JsonPointer.valueOf("/wifi_sta/rssi");

	protected AbstractG1Device(InetAddress address, CredentialsProvider credentialsProv) {
		super(address, credentialsProv);
	}
	
	protected void fillOnce(JsonNode settings) throws IOException {
		JsonNode deviceNode = settings.get("device");
		this.hostname = deviceNode.get("hostname").asText("");
		this.type = deviceNode.get("type").asText();
	}
	
	protected void fillSettings(JsonNode settings) throws IOException {
//		JsonNode staNode = settings.get("wifi_sta");
//		ssid = staNode.get("ssid").asText("");
//		ipv4Method = staNode.get("ipv4_method").asText("");

		// this.fw = settings.get("fw").asText();
		this.name = settings.path("name").asText("");
		this.debugEnabled = settings.path("debug_enable").asBoolean(false) ? LogMode.FILE : LogMode.NO; // missing in flood (20201128-102432/v1.9.2@e83f7025)
	}

	protected void fillStatus(JsonNode status) throws IOException {
		final JsonNode cloud = status.get("cloud");
		this.cloudEnabled = cloud.get("enabled").asBoolean();
		this.cloudConnected = cloud.get("connected").asBoolean();
		this.rssi = status.at(RSSI).asInt(0);
		this.uptime = status.get("uptime").asInt();
	}
	
//	public JsonNode getJSON(final String command) throws IOException, JsonParseException { //JsonProcessingException extends IOException
//		final ObjectMapper mapper = new ObjectMapper();
//		URLConnection uc = getUrlConnection(command);
//		try {
//			JsonNode res = mapper.readTree(uc.getInputStream());
//			status = Status.ON_LINE;
//			return res;
//		} catch (JsonParseException | RuntimeException e) {
//			status = Status.ERROR;
//			throw e;
//		} catch (IOException e) {
//			manageConnectionError(((HttpURLConnection)uc).getResponseCode());
//			throw e;
//		}
//	}

	public String sendCommand(final String command) {
		HttpGet httpget = new HttpGet(command);
		try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(httpHost, httpget, clientContext)) {
			int statusCode = response.getStatusLine().getStatusCode();
			if(statusCode == HttpURLConnection.HTTP_OK) {
				status = Status.ON_LINE;
			} else if(statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				status = Status.NOT_LOOGGED;
			} else /*if(statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR)*/ {
				status = Status.ERROR;
			} /*else {
				status = Status.OFF_LINE;
			}*/
//			String ret = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).lines().collect(Collectors.joining("\n"));
			String ret = EntityUtils.toString(response.getEntity());
			return (ret == null || ret.length() == 0 || ret/*.trim()*/.startsWith("{")) ? null : ret;
		} catch(IOException e) {
			status = Status.OFF_LINE;
			return "Status-OFFLINE"; //Main.LABELS.getString("err_connection_offline"); //todo
		} catch(RuntimeException e) {
			return e.getMessage();
		}
	}
//	
//	public URLConnection getUrlConnection(String urlPart) throws IOException {
//	//String authString = user + ":" + new String(credentials.getPassword());
//	//String authStringEnc = Base64.getEncoder().encodeToString(authString.getBytes());
//		final URL url = new URL("http://" + httpHost.getAddress().getHostAddress() + urlPart);
//		URLConnection uc = url.openConnection();
////		if(authStringEnc != null) {
////			uc.setRequestProperty("Authorization", "Basic " + authStringEnc);
////		}
//		return uc;
//	}
//	
//	protected void manageConnectionError(int response) {
////try {
////	final int response = ((HttpURLConnection)uc).getResponseCode();
//	if(response == HttpURLConnection.HTTP_UNAUTHORIZED) {
//		status = Status.NOT_LOOGGED;
//	} else {
//		status = Status.OFF_LINE;
//	}
////} catch(IOException ex) {
////	status = Status.OFF_LINE;
////}
//}

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
		// "settings/ap", "settings/sta", "settings/login", "settings/cloud" found into "settings"; "/ota" found into "status"
		return new String[] {"/shelly", "/settings", "/settings/actions", "/status"};
	}
	
	@Override
	public void reboot() throws IOException {
		getJSON("/reboot");
//		status = Status.READING; // here; getJSON("/reboot") put the device on-line
	}
	
	@Override
	public FirmwareManager getFWManager() throws IOException {
		return new FirmwareManagerG1(this);
	}
	
	@Override
	public LoginManager getLoginManager() throws IOException {
		return new LoginManagerG1(this);
	}
	
	@Override
	public WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException {
		return new WIFIManagerG1(this, WIFIManager.Network.SECONDARY);
	}
	
	@Override
	public MQTTManagerG1 getMQTTManager() throws IOException {
		return new MQTTManagerG1(this);
	}
	
	@Override
	public void backup(final File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
			sectionToStream("/settings", "settings.json", out);
			sectionToStream("/settings/actions", "actions.json", out);
		}
	}
	
	@Override
	public String restore(final File file, boolean force) throws IOException {
		try (   ZipFile in = new ZipFile(file);
				InputStream isSettings = in.getInputStream(in.getEntry("settings.json"));
				InputStream isActions = in.getInputStream(in.getEntry("actions.json")) ) {
			final ObjectMapper mapper = new ObjectMapper();
			JsonNode settings = mapper.readTree(isSettings);
			JsonNode actions = mapper.readTree(isActions);
			final String hostname = settings.get("device").get("hostname").asText("");
			final String fileType = settings.get("device").get("type").asText();
			if(fileType.length() > 0 && fileType.equals(this.type) == false) {
				return ERR_RESTORE_MODEL;
			} else if(hostname.equals(this.hostname) || force) {
				final ArrayList<String> errors = new ArrayList<>();
				restore(settings, errors);
				restoreCommons(settings, errors);
				Actions.restore(this, actions, errors);
				return errors.stream().filter(s-> s != null && s.length() > 0).collect(Collectors.joining("; "));
			} else {
				return ERR_RESTORE_HOST;
			}
		} catch(RuntimeException e) {
			return ERR_RESTORE_HOST;
		}
	}

	/**
	 * tz_dst - intentionally ignored (depends by backup date)
	 * debug_enable - intentionally ignored
	 * allow_cross_origin - intentionally ignored
	 * Return errors List
	 */
	protected abstract void restore(JsonNode settings, ArrayList<String> errors) throws IOException;

	private void restoreCommons(JsonNode settings, ArrayList<String> errors) throws UnsupportedEncodingException {
		errors.add(sendCommand("/settings/cloud?enabled=" + settings.get("cloud").get("enabled").asText()));
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings,
				"name", "discoverable", "timezone", "lat", "lng", "tzautodetect", "tz_utc_offset", /*"tz_dst",*/ "tz_dst_auto", "tz_dst_auto", "wifirecovery_reboot_enabled")));

		final JsonNode mqtt = settings.get("mqtt"); // password not restored
		errors.add(MQTTManagerG1.restore(this,  mqtt));
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

	protected static String jsonNodeToURLPar(JsonNode jNode, String ...pars) throws UnsupportedEncodingException {
//		return Arrays.stream(pars).map(p -> {
//			try {
//				return p + "=" + URLEncoder.encode(jNode.get(pars[0]).asText(), StandardCharsets.UTF_8.name());
//			} catch(Exception e) {
//				throw new RuntimeException(e);
//			}
//		}).collect(Collectors.joining("&"));
		String res = pars[0] + "=" +  URLEncoder.encode(jNode.get(pars[0]).asText(), StandardCharsets.UTF_8.name());
		for(int i = 1; i < pars.length; i++) {
			JsonNode thisNode = jNode.get(pars[i]);
			if(thisNode != null) {
				res += "&" + pars[i] + "=" + URLEncoder.encode(thisNode.asText(), StandardCharsets.UTF_8.name());
			} else {
				res += "&" + pars[i] + "=";
			}
		}
		return res;
	}
}