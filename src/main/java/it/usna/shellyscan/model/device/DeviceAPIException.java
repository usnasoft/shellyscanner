package it.usna.shellyscan.model.device;

import java.io.IOException;

public class DeviceAPIException extends IOException {
	private static final long serialVersionUID = 1L;
	// Common Errors - https://shelly-api-docs.shelly.cloud/gen2/General/CommonErrors
	public static final int INVALID_ARGUMENT = -103;
	public static final int DEADLINE_EXCEEDED = -104;
	public static final int RESOURCE_EXHAUSTED = -108;
	public static final int FAILED_PRECONDITION = -109;
	public static final int UNAVAILABLE = -114;
	
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