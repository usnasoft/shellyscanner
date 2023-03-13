package it.usna.shellyscan.model.device;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class ShellyAbstractDevice {
	protected HttpClient httpClient;
	protected final InetAddress address;
	protected String hostname;
	protected String mac;
	protected boolean cloudEnabled;
	protected boolean cloudConnected;
	protected boolean mqttEnabled;
	protected boolean mqttConnected;
	protected LogMode debugEnabled = LogMode.NO;
	protected int rssi;
	protected String ssid;
	protected int uptime;
	protected String name;
	protected Status status;
	protected long lastConnection = 0;

	protected final String uriPrefix;
	protected final ObjectMapper jsonMapper = new ObjectMapper();
	
	public enum Status {ON_LINE, OFF_LINE, NOT_LOOGGED, READING, ERROR};
	public enum LogMode {NO, FILE, MQTT, SOCKET, UDP};

	public enum Restore {ERR_RESTORE_HOST, ERR_RESTORE_MODEL, ERR_RESTORE_CONF, ERR_RESTORE_MSG,
		RESTORE_LOGIN, RESTORE_WI_FI1, RESTORE_WI_FI2,  RESTORE_WI_FI_AP, RESTORE_MQTT, RESTORE_OPEN_MQTT,
		ERR_UNKNOWN};

	protected ShellyAbstractDevice(InetAddress address, String hostname) {
		this.address = address;
		this.hostname = hostname;
		this.uriPrefix = "http://" + address.getHostAddress();
	}

//	public void init(HttpClient httpClient) throws IOException {
//		this.httpClient = httpClient;
//		init();
//	}
	
//	protected abstract void init() throws IOException;
	
	public JsonNode getJSON(final String command) throws IOException  { //JsonProcessingException extends IOException
		try {
			ContentResponse response = httpClient.GET(uriPrefix + command);
			int statusCode = response.getStatus();
			if(statusCode == HttpStatus.OK_200) {
				status = Status.ON_LINE;
				return jsonMapper.readTree(response.getContent());
			}
			if(statusCode == HttpStatus.UNAUTHORIZED_401) {
				status = Status.NOT_LOOGGED;
				throw new IOException("Status-" + statusCode);
			} else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR_500) {
				status = Status.ERROR;
				throw new IOException("Status-" + statusCode);
			} else {
				status = Status.OFF_LINE;
				throw new DeviceOfflineException("Status-" + statusCode);
			}
		} catch(InterruptedException | ExecutionException | TimeoutException e) {
			status = Status.OFF_LINE;
			throw new DeviceOfflineException(e);
		} catch (IOException | RuntimeException e) {
			if(status == Status.ON_LINE || status == Status.READING) {
				status = Status.ERROR;
			}
			throw e;
		}
	}
	
	public String getAsString(final String command) throws IOException {
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
	
	public InetAddress getAddress() {
		return address;
	}
	
//	public HttpClient getHttpClient() {
//		return httpClient;
//	}

	public boolean getCloudEnabled() {
		return cloudEnabled;
	}

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

	public LogMode getDebugMode() {
		return debugEnabled;
	}

	public int getUptime() {
		return uptime;
	}

	public String getName() {
		return name == null ? "" : name;
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
	
//	public LocalDateTime getLastTimestamp() {
//		return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastConnection), TimeZone.getDefault().toZoneId());
//	}

	public abstract String[] getInfoRequests();

	public abstract String getTypeID();

	public abstract void reboot() throws IOException;
	
	public abstract void refreshSettings() throws IOException;
	
	public abstract void refreshStatus() throws IOException;
	
	public abstract FirmwareManager getFWManager();
	
	public abstract WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException;
	
	public abstract MQTTManager getMQTTManager() throws IOException;
	
	public abstract LoginManager getLoginManager() throws IOException;

	public abstract boolean backup(final File file) throws IOException; // false: use of stored data; cound not connect to device
	
	public abstract Map<Restore, String> restoreCheck(final File file) throws IOException;
	
	public abstract String restore(final File file, Map<Restore, String> data) throws IOException;

	protected void sectionToStream(String section, String entryName, ZipOutputStream out) throws IOException {
		ZipEntry entry = new ZipEntry(entryName);
		out.putNextEntry(entry);
		try {
			ContentResponse response = httpClient.GET(uriPrefix + section);
			byte[] buffer = response.getContent();
			out.write(buffer, 0, buffer.length);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new IOException(e);
		} finally {
			out.closeEntry();
		}
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof ShellyAbstractDevice) ? hostname.equalsIgnoreCase(((ShellyAbstractDevice)o).hostname) : false; // equalsIgnoreCase for some devices hostname registered in not == to Shelly.GetDeviceInfo/id
	}

	@Override
	public String toString() {
		return getTypeName() + "-" + name + ": " + address.getHostAddress() + " (" + hostname + ")";
	}
} //278 - 399 - 316 - 251 - 237 - 209