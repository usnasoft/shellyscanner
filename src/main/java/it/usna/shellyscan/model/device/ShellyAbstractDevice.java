package it.usna.shellyscan.model.device;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class ShellyAbstractDevice {
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
	
	protected final ObjectMapper jsonMapper = new ObjectMapper();
	
	public enum Status {ON_LINE, OFF_LINE, NOT_LOOGGED, READING, ERROR};
	public enum LogMode {NO, FILE, MQTT, SOCKET, UDP};

	public enum Restore {ERR_RESTORE_HOST, ERR_RESTORE_MODEL, ERR_RESTORE_CONF, ERR_RESTORE_MSG,
		RESTORE_LOGIN, RESTORE_WI_FI1, RESTORE_WI_FI2,  RESTORE_WI_FI_AP, RESTORE_MQTT, RESTORE_OPEN_MQTT,
		ERR_UNKNOWN};
	
	protected HttpClientContext clientContext;
	protected HttpHost httpHost;

	protected ShellyAbstractDevice(InetAddress address, CredentialsProvider credentialsProv) {
//		httpHost = new HttpHost(address.getHostAddress()); // new HttpHost(address) too slow (reverse DNS)
//		httpHost = new HttpHost(address, address.getHostAddress(), 80, HttpHost.DEFAULT_SCHEME_NAME);
		httpHost = new HttpHost(null, address, address.getHostAddress(), 80);
		setCredentialsProvider(credentialsProv);
	}

	public void setCredentialsProvider(CredentialsProvider credentialsProv) {
		if(credentialsProv != null) {
			AuthCache authCache = new BasicAuthCache();
			authCache.put(httpHost, new BasicScheme());
			clientContext = HttpClientContext.create();
			clientContext.setCredentialsProvider(credentialsProv);
			clientContext.setAuthCache(authCache);
		} else {
			clientContext = null;
		}
	}
	
	public JsonNode getJSON(final String command) throws IOException { //JsonProcessingException extends IOException
		HttpGet httpget = new HttpGet(command);
		int statusCode;
		try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(httpHost, httpget, clientContext)) {
			statusCode = response./*getStatusLine().getStatusCode();*/getCode();
			if(statusCode == HttpURLConnection.HTTP_OK) {
				status = Status.ON_LINE;
//				final ObjectMapper mapper = new ObjectMapper();
				return jsonMapper.readTree(response.getEntity().getContent());
			}
		}  catch(SocketException | SocketTimeoutException e) {
			status = Status.OFF_LINE;
			throw e;
		} catch(/*JsonParseException |*/ IOException | RuntimeException e) {
			status = Status.ERROR;
			throw e;
		}
		if(statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
			status = Status.NOT_LOOGGED;
		} else if(statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
			status = Status.ERROR;
		} else {
			status = Status.OFF_LINE;
		}
		throw new IOException("Status-" + statusCode);
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

//	public InetAddress getAddress() {
//		return address;
//	}
	
	public HttpHost getHttpHost() {
		return httpHost;
	}
	
	public HttpClientContext getClientContext() {
		return clientContext;
	}

	//	public String getFw() {
	//		return fw;
	//	}
	
//	public String getSSID() {
//		return ssid;
//	}

//	public String getIPv4Method() {
//		return ipv4Method;
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
		return name;
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
	
//	protected void setStatus(HttpResponse response) {
//		int statusCode = response.getStatusLine().getStatusCode();
//		if(statusCode == HttpURLConnection.HTTP_OK) {
//			status = Status.ON_LINE;
//		} else if(statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
//			status = Status.NOT_LOOGGED;
//		} else if(statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
//			status = Status.ON_LINE;
//		} else {
//			status = Status.OFF_LINE;
//		}
//	}

	public abstract String[] getInfoRequests();

	public abstract String getTypeID();

	public abstract void reboot() throws IOException;
	
	public abstract void refreshSettings() throws IOException;
	
	public abstract void refreshStatus() throws IOException;
	
	public abstract FirmwareManager getFWManager() throws IOException;
	
	public abstract WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException;
	
	public abstract MQTTManager getMQTTManager() throws IOException;
	
	public abstract LoginManager getLoginManager() throws IOException;

	public abstract boolean backup(final File file) throws IOException; // false: use of stored data; cound not connect to device
	
	public abstract Map<Restore, String> restoreCheck(final File file) throws IOException;
	
	public abstract String restore(final File file, Map<Restore, String> data) throws IOException;

	// used by backup
	protected void sectionToStream(String section, String entryName, ZipOutputStream out) throws IOException {
		ZipEntry entry = new ZipEntry(entryName);
		out.putNextEntry(entry);
//		URLConnection uc = getUrlConnection(section);
		byte[] buffer = new byte[4096];
		int l;

		HttpGet httpget = new HttpGet(section);
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
				CloseableHttpResponse response = httpClient.execute(httpHost, httpget, clientContext);
				BufferedInputStream br = new BufferedInputStream(response.getEntity().getContent())) {
			while ((l = br.read(buffer)) >= 0) {
				out.write(buffer, 0, l);
			}
		}
		out.closeEntry();
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof ShellyAbstractDevice) ? hostname.equalsIgnoreCase(((ShellyAbstractDevice)o).hostname) : false; // equalsIgnoreCase for some devices hostname registered in not == to Shelly.GetDeviceInfo/id
	}

	@Override
	public String toString() {
		return getTypeName() + "-" + name + ": " + httpHost.getHostName() + " (" + hostname + ")";
	}
} //278 - 399 - 316 - 251 - 237