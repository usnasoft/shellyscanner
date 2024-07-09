package it.usna.shellyscan.model.device;

import java.io.IOException;

public class DeviceAPIException extends IOException {
	private static final long serialVersionUID = 1L;
	private final int code;
	private final String msg;
	
	public DeviceAPIException(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	public DeviceAPIException(int code) {
		this.code = code;
		this.msg = null;
	}

	@Override
	public String getMessage() {
		return code + ": " + ((msg != null && msg.isEmpty() == false) ? msg : "Generic error");
	}

	public int getErrorCode() {
		return code;
	}
	
	public String getErrorMessage() {
		return msg;
	}
}
