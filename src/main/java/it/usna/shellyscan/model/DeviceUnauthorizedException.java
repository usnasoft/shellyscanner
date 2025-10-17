package it.usna.shellyscan.model;

import java.io.IOException;

import org.eclipse.jetty.http.HttpStatus;

public class DeviceUnauthorizedException extends IOException {
	private static final long serialVersionUID = 1L;
	
	public DeviceUnauthorizedException() {
		super("Status-" + HttpStatus.UNAUTHORIZED_401);
	}
	
	public DeviceUnauthorizedException(String msg) {
		super(msg);
	}
}
