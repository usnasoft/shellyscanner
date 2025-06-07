package it.usna.shellyscan.model.device;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;

/**
 * Base class for any device
 * @author usna
 */
public abstract class ShellyAbstractDevice {
	private final static Logger LOG = LoggerFactory.getLogger(ShellyAbstractDevice.class);
	protected HttpClient httpClient;
	protected final InetAddressAndPort addressAndPort;
	protected String hostname;
	protected String mac;
	protected boolean cloudEnabled;
	protected boolean cloudConnected;
	protected boolean mqttEnabled;
	protected boolean mqttConnected;
	protected LogMode debugMode = LogMode.NONE;
	protected int rssi;
	protected String ssid;
	protected int uptime;
	protected String name;
	protected Status status;
	protected boolean rebootRequired = false;
	protected long lastConnection = 0;

	protected final String uriPrefix;
	protected final ObjectMapper jsonMapper = new ObjectMapper();
	
	public enum Status {ON_LINE, OFF_LINE, NOT_LOOGGED, READING, ERROR, GHOST}; // GHOST not yet detected (in store)
	public enum LogMode {NONE, FILE, MQTT, SOCKET, UDP, UNDEFINED};

	protected ShellyAbstractDevice(InetAddress address, int port, String hostname) {
		addressAndPort = new InetAddressAndPort(address, port);
		this.hostname = hostname;
		if(address instanceof Inet6Address) {
			if(port == 80) {
				this.uriPrefix = "http://[" + address.getHostAddress() + "]";
			} else {
				this.uriPrefix = "http://[" + address.getHostAddress() + "]:" + port;
			}
		} else {
			this.uriPrefix = "http://" + addressAndPort.getRepresentation();
		}
	}
	
	/**
	 * Non ethernet devices (Blu)
	 */
	protected ShellyAbstractDevice(InetAddressAndPort address) {
		addressAndPort = address;
		InetAddress addr = address.getAddress();
		if(addr instanceof Inet6Address) {
			int port = address.getPort();
			if(port == 80) {
				this.uriPrefix = "http://[" + addr.getHostAddress() + "]";
			} else {
				this.uriPrefix = "http://[" + addr.getHostAddress() + "]:" + port;
			}
		} else {
			this.uriPrefix = "http://" + addressAndPort.getRepresentation();
		}
	}
	
	public JsonNode getJSON(final String command) throws IOException { //JsonProcessingException extends IOException
		ContentResponse response;
		int statusCode;
		try {
			response = httpClient.GET(uriPrefix + command);
			statusCode = response.getStatus();
			if(statusCode == HttpStatus.OK_200) {
				status = Status.ON_LINE;
				return jsonMapper.readTree(response.getContent());
			}
		} catch(InterruptedException | ExecutionException | TimeoutException | SocketTimeoutException e) {
			status = Status.OFF_LINE;
			throw new DeviceOfflineException(e);
		} catch (IOException | RuntimeException e) {
//			if(status == Status.ON_LINE || status == Status.READING) {
				status = Status.ERROR;
//			}
			throw e;
		}
		if(statusCode == HttpStatus.UNAUTHORIZED_401) {
			status = Status.NOT_LOOGGED;
			throw new IOException("Status-" + HttpStatus.UNAUTHORIZED_401);
		} else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR_500) {
			status = Status.ERROR;
			String errorMsg;
			try {
				errorMsg = jsonMapper.readTree(response.getContent()).toString();
			} catch(Exception e) {
				errorMsg = null;
			}
			throw new DeviceAPIException(HttpStatus.INTERNAL_SERVER_ERROR_500, errorMsg);
		} else if(statusCode == HttpStatus.NOT_FOUND_404) {
			status = Status.ERROR;
			throw new IOException("Status-" + HttpStatus.NOT_FOUND_404);
		} else {
			status = Status.OFF_LINE;
			throw new DeviceOfflineException("Status-" + statusCode);
		}
	}
	
	public String httpGetAsString(final String command) throws IOException {
		try {
			return httpClient.GET(uriPrefix + command).getContentAsString();
		} catch(InterruptedException | ExecutionException | TimeoutException e) {
			status = Status.OFF_LINE;
			throw new DeviceOfflineException(e);
		} catch (RuntimeException e) {
			if(status == Status.ON_LINE || status == Status.READING) {
				status = Status.ERROR;
			}
			throw e;
		}
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	public String getMacAddress() {
		return mac;
	}
	
	public void setMacAddress(String mac) {
		this.mac = mac;
	}
	
	public InetAddressAndPort getAddressAndPort() {
		return addressAndPort;
	}

	public boolean getCloudEnabled() {
		return cloudEnabled;
	}
	
	public abstract boolean setDebugMode(LogMode mode, boolean enable);
	
	public LogMode getDebugMode() {
		return debugMode;
	}
	
	public abstract String setCloudEnabled(boolean enable);

	public boolean getCloudConnected() {
		return cloudConnected;
	}
	
	public boolean getMQTTEnabled() {
		return mqttEnabled;
	}
	
	public boolean getMQTTConnected() {
		return mqttConnected;
	}

	public int getRssi() {
		return rssi;
	}
	
	public String getSSID() {
		return ssid;
	}

	public int getUptime() {
		return uptime;
	}

	public String getName() {
		return name == null ? "" : name;
	}
	
	public boolean rebootRequired() {
		return rebootRequired; //return getJSON("/rpc/Sys.GetStatus").path("restart_required").asBoolean(false);
	}

	public abstract String getTypeName();
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status s) {
		status = s;
	}
	
	public Meters[] getMeters() {
		return null;
	}
	
	public long getLastTime() {
		return lastConnection;
	}

	public abstract String[] getInfoRequests();

	public abstract String getTypeID();

	public abstract void reboot() throws IOException;
	
	public abstract boolean setEcoMode(boolean eco);
	
	public abstract void refreshSettings() throws IOException;
	
	public abstract void refreshStatus() throws IOException;
	
	public abstract FirmwareManager getFWManager();
	
	public abstract WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException;
	
	public abstract MQTTManager getMQTTManager() throws IOException;
	
	public abstract LoginManager getLoginManager() throws IOException;
	
	public abstract TimeAndLocationManager getTimeAndLocationManager() throws IOException;
	
	public abstract InputResetManager getInputResetManager() throws IOException;

//	public abstract boolean backup(final File file) throws IOException; // false: use of stored data; could not connect to device
	
	public abstract boolean backup(final Path file) throws IOException; // false: use of stored data; could not connect to device
	
	
	public abstract Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException;
	
	/**
	 * @param backupJsons map of buckup sections (json name-json section)
	 * @param data value returned by restoreCheck(...)
	 * @return list of results for any restore section (element is null if section restored successfully)
	 * @throws IOException
	 */
	public abstract List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) throws IOException;

	/**
	 * Backup basic operation
	 * @param section call whose returned json must be stored
	 * @param entryName ZipEntry name
	 * @param fs FileSystem
	 * @throws IOException on error or response.getStatus() != HttpStatus.OK_200
	 */
	protected JsonNode sectionToStream(String section, String entryName, FileSystem fs) throws IOException {
		try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath(entryName))) {
			JsonNode resp = getJSON(section);
			jsonMapper.writer().writeValue(writer, resp);
			return resp;
		} catch (Exception e) {
			LOG.debug("sectionToStream {}", section, e);
			throw new DeviceOfflineException(e);
		}
	}

	@Override
	public int hashCode() {
		return mac.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o != null && mac.equals(((ShellyAbstractDevice)o).mac);
	}

	@Override
	public String toString() {
		return getTypeName() + "-" + name + ": " + addressAndPort.getRepresentation() + " (" + hostname + ")";
	}
} //278 - 399 - 316 - 251 - 237 - 231 - 247 - 271