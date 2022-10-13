package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyUnmanagedDevice;

public class ShellyG2Unmanaged extends AbstractG2Device implements ShellyUnmanagedDevice {
	private String type;
	private Exception ex;

	public ShellyG2Unmanaged(InetAddress address, String hostname, CredentialsProvider credentialsProv) {
		super(address, credentialsProv);
		try {
			JsonNode device = getJSON("/rpc/Shelly.GetDeviceInfo");
			this.type = device.get("app").asText();
			fillOnce(device);
			fillSettings(getJSON("/rpc/Shelly.GetConfig"));
			fillStatus(getJSON("/rpc/Shelly.GetStatus"));
		} catch (/*IO*/Exception e) {
			if(status != Status.NOT_LOOGGED) {
				status = Status.ERROR;
			}
			this.ex = e;
			this.hostname = hostname;
			name = "";
		}
	}
	
	public ShellyG2Unmanaged(InetAddress address, String hostname, CredentialsProvider credentialsProv, Exception e) {
		super(address, null);
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
		return "Generic G2";
	}
	
	@Override
	public String getTypeID() {
		return type;
	}
	
	/**
	 * @return null if device type is unknown or exception if an error ha occurred on construction 
	 */
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
			return "Shelly G2 (unmanaged) " + type + ": " + super.toString();
		} else {
			return "Shelly G2 (unmanaged): " + super.toString() + " Error: " + ex.getMessage();
		}
	}
}