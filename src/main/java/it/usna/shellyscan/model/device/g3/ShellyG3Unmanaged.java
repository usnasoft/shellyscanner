package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyUnmanagedDeviceInterface;

public class ShellyG3Unmanaged extends AbstractG3Device implements ShellyUnmanagedDeviceInterface {
	private String type;
	private Throwable ex;

	public ShellyG3Unmanaged(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	public ShellyG3Unmanaged(InetAddress address, int port, String hostname, Throwable e) {
		super(address, port, hostname);
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
	
	@Override
	protected void init(JsonNode devInfo) {
		try {
			this.type = devInfo.get("app").asText();
			this.mac = devInfo.get("mac").asText();
			this.hostname = devInfo.get("id").asText("");

			fillSettings(getJSON("/rpc/Shelly.GetConfig"));
			fillStatus(getJSON("/rpc/Shelly.GetStatus"));
		} catch (/*IO*/Exception e) {
			if(status != Status.NOT_LOOGGED) {
				status = Status.ERROR;
			}
			this.ex = e;
			name = "";
		}
	}
	
	public String getTypeName() {
		return "Generic G3";
	}
	
	@Override
	public String getTypeID() {
		return type;
	}
	
	/**
	 * @return null if device type is unknown or exception if an error ha occurred on construction 
	 */
	@Override
	public Throwable getException() {
		return ex;
	}
	
	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) {
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
			return "Shelly G3 (unmanaged) " + type + ": " + super.toString();
		} else {
			return "Shelly G3 (unmanaged): " + super.toString() + " Error: " + ex.getMessage();
		}
	}
}