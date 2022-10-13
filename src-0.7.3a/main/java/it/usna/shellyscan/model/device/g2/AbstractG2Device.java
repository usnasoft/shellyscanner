package it.usna.shellyscan.model.device.g2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.FirmwareManager;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;

public abstract class AbstractG2Device extends ShellyAbstractDevice {
	private final static JsonPointer DEV_NAME = JsonPointer.valueOf("/sys/device/name");
	private final static JsonPointer CLOUD_CONF = JsonPointer.valueOf("/cloud/enable");
	private final static JsonPointer CLOUD_STATUS = JsonPointer.valueOf("/cloud/connected");
	private final static JsonPointer RSSI = JsonPointer.valueOf("/wifi/rssi");
	private final static JsonPointer UPTIME = JsonPointer.valueOf("/sys/uptime");
	private final static JsonPointer DEBUG = JsonPointer.valueOf("/sys/debug");
	
//	private HttpClientContext clientContext;
//	private HttpHost httpHost;

	protected AbstractG2Device(InetAddress address, CredentialsProvider credentialsProv) {
		super(address, credentialsProv);
	}
	
	protected void fillOnce() throws JsonParseException, IOException {
		JsonNode device = getJSON("/rpc/Shelly.GetDeviceInfo");
		this.hostname = device.get("id").asText("");
		this.type = device.get("app").asText();
	}
	
	protected void fillSettings(JsonNode configuration) throws IOException {
		this.name = configuration.at(DEV_NAME).asText("");

		this.cloudEnabled = configuration.at(CLOUD_CONF).asBoolean();

		JsonNode debugNode = configuration.at(DEBUG);
		if(debugNode.at("/websocket/enable").asBoolean()) {
			this.debugEnabled = LogMode.SOCKET;
		} else if(debugNode.at("/mqtt/enable").asBoolean()) {
			this.debugEnabled = LogMode.MQTT;
		} else if(debugNode.at("/udp/addr").isNull() == false) {
			this.debugEnabled = LogMode.UDP;
		} else {
			this.debugEnabled = LogMode.NO;
		}
	}
	
	protected void fillStatus(JsonNode status) throws IOException {
		this.cloudConnected = status.at(CLOUD_STATUS).asBoolean();
		
		this.rssi = status.at(RSSI).asInt();
		this.uptime = status.at(UPTIME).asInt();
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
		return new String[] {"/rpc/Shelly.GetDeviceInfo", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List", "/rpc/Script.List"/*, "/rpc/Sys.GetStatus"*/};
	}
	
	@Override
	public void reboot() throws IOException {
		getJSON("/rpc/Shelly.Reboot");
	}
	
	@Override
	public FirmwareManager getFWManager() throws IOException {
		return new FirmwareManagerG2(this);
	}
	
	@Override
	public WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException {
		return new WIFIManagerG2(this, net);
	}
	
	@Override
	public LoginManager getLoginManager() throws IOException {
		return new LoginManagerG2(this);
	}
	
	@Override
	public MQTTManagerG2 getMQTTManager() throws IOException {
		return new MQTTManagerG2(this);
	}

	public String postCommand(final String method, String payload) {
		HttpPost httpPost = new HttpPost("/rpc");
		try {
			httpPost.setEntity(new StringEntity("{\"id\":1, \"method\":\"" + method + "\", \"params\":" + payload + "}"));
		} catch (UnsupportedEncodingException e1) {}
		try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(httpHost, httpPost, clientContext)) {
			int statusCode = response.getStatusLine().getStatusCode();
			if(statusCode == HttpURLConnection.HTTP_OK) {
				status = Status.ON_LINE;
			} else if(statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				status = Status.NOT_LOOGGED;
			} else /*if(statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR || statusCode == HttpURLConnection.HTTP_BAD_REQUEST)*/ {
				status = Status.ERROR;
			} /*else {
				status = Status.OFF_LINE;
			}*/
			final ObjectMapper mapper = new ObjectMapper();
			final JsonNode resp = mapper.readTree(response.getEntity().getContent());
			JsonNode error;
			if((error = resp.get("error")) == null) {
				return null;
			} else {
				return error.path("message").asText("Generic error");
			}
		} catch(IOException e) {
			status = Status.OFF_LINE;
			return "Status-OFFLINE";
		} catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	
	@Override
	public void backup(final File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", out);
			sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", out);
			sectionToStream("/rpc/Schedule.List", "Schedule.List.json", out);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
			sectionToStream("/rpc/Script.List", "Script.List.json", out);
		}
	}
	
	@Override
	public String restore(final File file, boolean force) throws IOException, NullPointerException {
		try (   ZipFile in = new ZipFile(file);
				InputStream isDevice = in.getInputStream(in.getEntry("Shelly.GetDeviceInfo.json"));
				InputStream isConfig = in.getInputStream(in.getEntry("Shelly.GetConfig.json"));
				InputStream isSchedule = in.getInputStream(in.getEntry("Schedule.List.json"));
				InputStream isWebhook = in.getInputStream(in.getEntry("Webhook.List.json")) ) {
			final ObjectMapper mapper = new ObjectMapper();
			JsonNode device = mapper.readTree(isDevice);
			final String hostname = device.get("id").asText("");
			final String fileType = device.get("app").asText();
			if(this.type.equals(fileType) == false) {
				return ERR_RESTORE_MODEL;
			} else if(hostname.equals(this.hostname) || force) {
				final ArrayList<String> errors = new ArrayList<>();
				JsonNode config = mapper.readTree(isConfig);
				restore(config, errors);
				restoreCommonConfig(config, errors);
				JsonNode schedule = mapper.readTree(isSchedule);
				restoreSchedule(schedule, errors);
				JsonNode webhooks = mapper.readTree(isWebhook);
				restoreWebhook(webhooks, errors);
				return errors.stream().filter(s-> s != null && s.length() > 0).collect(Collectors.joining("; "));
			} else {
				return ERR_RESTORE_HOST;
			}
		} catch(RuntimeException e) {
			return ERR_RESTORE_MODEL;
		}
//		return "Not supported";
	}
	
	protected abstract void restore(JsonNode fileConfig, ArrayList<String> errors) throws IOException;
	
	//curl -X POST -d '{"id": 1, "method": "Sys.SetConfig", "params": {"config": {"location": {"tz": "Europe/Sofia"}}}}' http://${SHELLY}/rpc
	private void restoreCommonConfig(JsonNode config, ArrayList<String> errors) {
		JsonNodeFactory factory = new JsonNodeFactory(false);
		ObjectNode outConfig = factory.objectNode();
		
		// BLE.SetConfig
		outConfig.set("config", config.get("ble").deepCopy());
		errors.add(postCommand("BLE.SetConfig", outConfig));
		
		// Cloud.SetConfig
		ObjectNode outCloud = factory.objectNode(); // Cloud // https://shelly-api-docs.shelly.cloud/gen2/Components/SystemComponents/Cloud#cloudsetconfig-example
		outCloud.put("enable", config.at("/cloud/enable").asBoolean());
		outConfig.set("config", outCloud);
		errors.add(postCommand("Cloud.SetConfig", outConfig));
		
		// MQTT.SetConfig
		outConfig.set("config", config.get("mqtt").deepCopy());
		errors.add(postCommand("MQTT.SetConfig", outConfig));
		
		// Sys.SetConfig
		JsonNode sys = config.get("sys");
		
		JsonNode name = sys.at("/device/name"); // Device name
		ObjectNode outDevice = factory.objectNode();
		outDevice.put("name", name.asText());
		ObjectNode outSys = factory.objectNode();
		outSys.set("device", outDevice);
		
		outSys.set("sntp", sys.get("sntp").deepCopy());

		outConfig.set("config", outSys);
		errors.add(postCommand("Sys.SetConfig", outConfig));
		
		WIFIManagerG2.restore(this, config.get("wifi"));
	}
	
	private void restoreSchedule(JsonNode schedule, ArrayList<String> errors) {
		errors.add(postCommand("Schedule.DeleteAll", "{}"));
		for(JsonNode sc: schedule.get("jobs")) {
			ObjectNode thisSc = (ObjectNode)sc.deepCopy();
			thisSc.remove("id");
			errors.add(postCommand("Schedule.Create", thisSc));
		}
	}
	
	private void restoreWebhook(JsonNode webhooks, ArrayList<String> errors) {
		errors.add(postCommand("Webhook.DeleteAll", "{}"));
		for(JsonNode ac: webhooks.get("hooks")) {
			ObjectNode thisAction = (ObjectNode)ac.deepCopy();
			thisAction.remove("id");
			errors.add(postCommand("Webhook.Create", thisAction));
		}
	}
	
//	private static void put(ObjectNode out, JsonNode origin, String ... keyes) {
//		for(String key: keyes) {
//			JsonNode val = origin.get(key);
//			if(val.isInt() || val.isLong()) {
//				out.put(key, val.asLong());
//			} else if(val.isBoolean()) {
//				out.put(key, val.asBoolean());
//			} else {
//				out.put(key, val.asText());
//			}	
//		}
//	}
	
	public String postCommand(final String method, JsonNode payload) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			String pl = mapper.writeValueAsString(payload);
			return postCommand(method, pl);
		} catch (JsonProcessingException e) {
			return e.toString();
		}
	}
}