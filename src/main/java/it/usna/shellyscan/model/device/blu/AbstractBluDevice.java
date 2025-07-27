package it.usna.shellyscan.model.device.blu;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.StringRequestContent;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.DeviceAPIException;
import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.PageIterator;
import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

public abstract class AbstractBluDevice extends ShellyAbstractDevice {
	public final static String GENERATION = "blu";
	private final static Logger LOG = LoggerFactory.getLogger(AbstractBluDevice.class);
	protected final AbstractG2Device parent;
//	protected WebSocketClient wsClient;
	protected final String componentIndex;
	
	public final static String DEVICE_KEY_PREFIX = DynamicComponents.BTHOME_DEVICE + ":"; // "bthomedevice:";
	public final static String SENSOR_KEY_PREFIX = DynamicComponents.BTHOME_SENSOR + ":"; // "bthomesensor:";
	public final static String GROUP_KEY_PREFIX = DynamicComponents.GROUP_TYPE + ":"; // "group:";
	
	/**
	 * AbstractBluDevice constructor
	 * @param parent
	 * @param compInfo
	 * @param index
	 */
	protected AbstractBluDevice(AbstractG2Device parent, JsonNode compInfo, String index) {
		super(new BluInetAddressAndPort(parent.getAddressAndPort(), Integer.parseInt(index)));
		this.parent = parent;
		this.componentIndex = index;
		this.mac = compInfo.path("config").path("addr").asText();
	}
	
	public void init(HttpClient httpClient/*, WebSocketClient wsClient*/) throws IOException {
		this.httpClient = httpClient;
//		this.wsClient = wsClient;
		refreshStatus();
		refreshSettings();
	}

//	@Override
//	public BluInetAddressAndPort getAddressAndPort() {
//		return (BluInetAddressAndPort)addressAndPort;
//	}
	
	public ShellyAbstractDevice getParent() {
		return parent;
	}
	
	public String getIndex() {
		return componentIndex;
	}
	
	@Override
	public Status getStatus() {
		return (rssi < 0) ? status : Status.OFF_LINE;
	}
	
	public String postCommand(final String method, JsonNode payload) {
		try {
			return postCommand(method, jsonMapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			return e.toString();
		}
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
	
	/**
	 * return null if ok or error description in case of error; cannot use parent.postCommand becouse of the status
	 */
	public String postCommand(final String method, String payload) {
		try {
			final JsonNode resp = executeRPC(method, payload);
			JsonNode error;
			if((error = resp.get("error")) == null) { // {"id":1,"src":"shellyplusi4-xxx","result":{"restart_required":true}}
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

	private JsonNode executeRPC(final String method, String payload) throws IOException, StreamReadException { // StreamReadException extends ... IOException
		try {
			ContentResponse response = httpClient.POST(uriPrefix + "/rpc")
					.body(new StringRequestContent("application/json", "{\"id\":1,\"method\":\"" + method + "\",\"params\":" + payload + "}", StandardCharsets.UTF_8))
					.send();
			int statusCode = response.getStatus();
			if(statusCode == HttpStatus.OK_200) {
				status = Status.ON_LINE;
			} else if(statusCode == HttpStatus.UNAUTHORIZED_401) {
				status = Status.NOT_LOOGGED;
			} else {
				status = Status.ERROR;
				LOG.debug("executeRPC - reponse code: {}", statusCode);
			}
			return jsonMapper.readTree(response.getContent());
		} catch(InterruptedException | ExecutionException | TimeoutException | SocketTimeoutException e) {
			status = Status.OFF_LINE;
			throw new DeviceOfflineException(e);
		}
	}

	@Override
	public void reboot() throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean setDebugMode(LogMode mode, boolean enable) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String setCloudEnabled(boolean enable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setEcoMode(boolean eco) {
		throw new UnsupportedOperationException();
	}

	@Override
	public WIFIManager getWIFIManager(Network net) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MQTTManager getMQTTManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LoginManager getLoginManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TimeAndLocationManager getTimeAndLocationManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public InputResetManager getInputResetManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return getTypeName() + "-" + name + ":" + mac;
	}
}