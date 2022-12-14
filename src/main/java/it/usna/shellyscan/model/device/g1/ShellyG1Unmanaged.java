package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyUnmanagedDevice;

public class ShellyG1Unmanaged extends AbstractG1Device implements ShellyUnmanagedDevice {
	private String type;
	private Exception ex;

	public ShellyG1Unmanaged(InetAddress address, String hostname, CredentialsProvider credentialsProv) {
		super(address, credentialsProv);
		try {
			// try to retrieve minimal information set
			JsonNode settings = getJSON("/settings");
			JsonNode deviceNode = settings.get("device");
			this.type = deviceNode.get("type").asText();
			fillOnce(settings);
			fillSettings(settings);
			fillStatus(getJSON("/status"));
		} catch (/*IO*/Exception e) {
			if(status != Status.NOT_LOOGGED) {
				status = Status.ERROR;
			}
			this.ex = e;
			this.hostname = hostname;
			name = "";
		}
	}
	
	public ShellyG1Unmanaged(InetAddress address, String hostname, CredentialsProvider credentialsProv, Exception e) {
		super(address, /*null*/credentialsProv);
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
	
	public String getTypeName() {
		return "Generic";
	}
	
	@Override
	public String getTypeID() {
		return type;
	}
	
	@Override
	public Exception geException() {
		return ex;
	}
	
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		// basic restore? not in case of error
	}
	
	@Override
	public Status getStatus() {
		if(status == Status.ON_LINE && ex != null) {
			return Status.ERROR;
		} else {
			return status;
		}
	}

	@Override
	public String toString() {
		if(ex == null) {
			return "Shelly (unmanaged) " + type + ": " + super.toString();
		} else {
			return "Shelly (unmanaged): " + super.toString() + " Error: " + ex.getMessage();
		}
	}
}