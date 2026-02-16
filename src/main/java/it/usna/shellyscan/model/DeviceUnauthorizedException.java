package it.usna.shellyscan.model;

import java.io.IOException;

import org.eclipse.jetty.http.HttpStatus;

import tools.jackson.databind.JsonNode;

public class DeviceUnauthorizedException extends IOException {
	private static final long serialVersionUID = 1L;
	private JsonNode authdDetails;
	
	public DeviceUnauthorizedException() {
		super("Status-" + HttpStatus.UNAUTHORIZED_401);
	}
	
	public DeviceUnauthorizedException(JsonNode details) {
		super("Status-" + HttpStatus.UNAUTHORIZED_401);
		authdDetails = details;
	}
	
	public DeviceUnauthorizedException(String msg) {
		super(msg);
	}
	
	public JsonNode getDetails() {
		return authdDetails;
	}
}
