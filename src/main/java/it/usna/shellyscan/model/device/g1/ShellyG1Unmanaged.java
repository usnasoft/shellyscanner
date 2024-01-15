package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyUnmanagedDeviceInterface;

public class ShellyG1Unmanaged extends AbstractG1Device implements ShellyUnmanagedDeviceInterface {
	private String type;
//	private boolean unrecoverable;
	private Throwable ex;
	
	public ShellyG1Unmanaged(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

//	public ShellyG1Unmanaged(InetAddress address, int port, String hostname, Exception e) {
//		this(address, port, hostname, null, e);
//	}
	
	public ShellyG1Unmanaged(InetAddress address, int port, String hostname/*, HttpClient httpClient*/, Throwable e) {
		super(address, port, hostname);
//		this.httpClient = httpClient;
		this.ex = e;
		this.hostname = hostname;
		name = "";
		if(e instanceof IOException && "Status-401".equals(e.getMessage())) {
			status = Status.NOT_LOOGGED;
		} else if(e instanceof IOException && e instanceof JsonProcessingException == false) { // JsonProcessingException extends IOException
			status = Status.OFF_LINE;
		} else {
			status = Status.ERROR;
		}
	}
	
//	public void setUnrecoverable(boolean unrecoverable) {
//		this.unrecoverable = unrecoverable;
//	}

	@Override
	protected void init() { // try to retrieve minimal information set
		try {
			JsonNode settings = getJSON("/settings");
			this.hostname = settings.get("device").get("hostname").asText("");
			this.type = settings.get("device").get("type").asText();
//			fillOnce(settings);
			fillSettings(settings);
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
			fillStatus(getJSON("/status"));
		} catch (/*IO*/Exception e) {
			if(status != Status.NOT_LOOGGED) {
				status = Status.ERROR;
			}
			this.ex = e;
			name = "";
		}
	}

	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}
	
	public String getTypeName() {
		return "Generic";
	}
	
	@Override
	public String getTypeID() {
		return type;
	}
	
	@Override
	public Throwable getException() {
		return ex;
	}
	
	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException {
		// basic restore? not in case of error
	}
	
	@Override
	public Status getStatus() {
		if((status == Status.ON_LINE && ex != null) /*|| unrecoverable*/) {
			return Status.ERROR;
		} else {
			return status;
		}
	}

	@Override
	public String toString() {
		if(ex == null) {
			return "Shelly G1 (unmanaged) " + type + ": " + super.toString();
		} else {
			return "Shelly G1 (unmanaged): " + super.toString() + " Error: " + ex.getMessage();
		}
	}
}