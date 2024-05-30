package it.usna.shellyscan.model.device;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jetty.client.HttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.WIFIManager.Network;

/**
 * Generic Shelly device model for unknown generation
 * @author usna
 */
public class ShellyGenericUnmanagedImpl extends ShellyAbstractDevice implements ShellyUnmanagedDeviceInterface {
	private final static Pattern MAC_PATTERN = Pattern.compile("^[A-F0-9]{12}$");
	private Throwable ex;

	public ShellyGenericUnmanagedImpl(InetAddress address, int port, String hostname, HttpClient httpClient) {
		super(address, port, hostname);
		this.httpClient = httpClient;
		if(hostname.length() > 12) {
			String mac = hostname.substring(Math.max(hostname.length() - 12, 0), hostname.length()).toUpperCase();
			this.mac = MAC_PATTERN.matcher(mac).matches() ? mac : "";
		} else {
			this.mac = "";
		}
		this.name = this.ssid = "";
		ex = new UnsupportedOperationException("Unmanaged Shelly generation");
	}
	
	public ShellyGenericUnmanagedImpl(InetAddress address, int port, String hostname, HttpClient httpClient, Throwable e) {
		this(address, port, hostname, httpClient);
		this.ex = e;
		if(e instanceof IOException && "Status-401".equals(e.getMessage())) {
			status = Status.NOT_LOOGGED;
		} else if(e instanceof IOException && e instanceof JsonProcessingException == false) { // JsonProcessingException extends IOException
			status = Status.OFF_LINE;
		} else {
			status = Status.ERROR;
		}
	}

	@Override
	public Throwable getException() {
		return ex;
	}

	@Override
	public void refreshSettings() {
		// none
	}

	@Override
	public void refreshStatus() {
		// none
	}

	@Override
	public Status getStatus() {
		return Status.ERROR;
	}

	@Override
	public String[] getInfoRequests() {
		return new String[] {"/shelly"};
	}

	@Override
	public String getTypeName() {
		return "Generic";
	}

	@Override
	public String getTypeID() {
		return "";
	}

	@Override
	public void reboot() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setEcoMode(boolean eco) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FirmwareManager getFWManager() {
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
	public boolean backup(File file) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Restore, String> restoreCheck(Map<String, JsonNode> backupJsons) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<Restore, String> data) {
		throw new UnsupportedOperationException();
	}
}