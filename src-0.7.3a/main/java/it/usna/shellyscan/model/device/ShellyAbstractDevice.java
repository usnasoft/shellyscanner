package it.usna.shellyscan.model.device;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class ShellyAbstractDevice {
//	protected String ssid;
//	protected String ipv4Method;
	protected String type;
	protected String hostname;
	protected boolean cloudEnabled;
	protected boolean cloudConnected;
	protected LogMode debugEnabled = LogMode.NO;
	protected int rssi;
	protected int uptime;
	protected String name;
	protected Status status;
	
	public enum Status {ON_LINE, OFF_LINE, NOT_LOOGGED, READING, ERROR};
	public enum LogMode {NO, FILE, MQTT, SOCKET, UDP};

	public static String ERR_RESTORE_HOST = "msgRestoreDifferent";
	public static String ERR_RESTORE_MODEL = "msgRestoreDifferentModel";
	
	protected HttpClientContext clientContext;
	protected HttpHost httpHost;

	protected ShellyAbstractDevice(InetAddress address, CredentialsProvider credentialsProv) {
//		httpHost = new HttpHost(address.getHostAddress()); // new HttpHost(address) too slow (reverse DNS)
		httpHost = new HttpHost(address, address.getHostAddress(), 80, HttpHost.DEFAULT_SCHEME_NAME);
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
			statusCode = response.getStatusLine().getStatusCode();
			if(statusCode == HttpURLConnection.HTTP_OK) {
				status = Status.ON_LINE;
				final ObjectMapper mapper = new ObjectMapper();
				return mapper.readTree(response.getEntity().getContent());
			}
		}  catch(SocketTimeoutException | HttpHostConnectException e) {
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

	public int getRssi() {
		return rssi;
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

	public abstract void reboot() throws IOException;
	
	public abstract void refreshSettings() throws IOException;
	
	public abstract void refreshStatus() throws IOException;
	
	public abstract FirmwareManager getFWManager() throws IOException;
	
	public abstract WIFIManager getWIFIManager(WIFIManager.Network net) throws IOException;
	
	public abstract MQTTManager getMQTTManager() throws IOException;
	
	public abstract LoginManager getLoginManager() throws IOException;

	public abstract void backup(final File file) throws IOException;
	
	public abstract String restore(final File file, boolean force) throws IOException, NullPointerException;

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
} //278 - 399 - 316 - 251