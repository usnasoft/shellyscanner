package it.usna.shellyscan.model.device;

import java.io.IOException;

public class DeviceOfflineException extends IOException {
	private static final long serialVersionUID = 1L;
	
	public DeviceOfflineException(Throwable cause) {
		super("Status-OFFLINE", cause);
	}
	
	public DeviceOfflineException(String msg) {
		super(msg);
	}
}
