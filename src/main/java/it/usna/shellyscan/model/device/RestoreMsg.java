package it.usna.shellyscan.model.device;

public enum RestoreMsg {
	// restoreCheck
	PRE_QUESTION_RESTORE_HOST(Type.PRE),
	
	ERR_RESTORE_MODEL(Type.ERROR),
	ERR_RESTORE_CONF(Type.ERROR),
	ERR_RESTORE_MSG(Type.ERROR),
	ERR_UNKNOWN(Type.ERROR),
	
	ERR_RESTORE_MODE_COVER(Type.ERROR),
	ERR_RESTORE_MODE_THERM(Type.ERROR),
	ERR_RESTORE_MODE_TRIPHASE(Type.ERROR),
	ERR_RESTORE_PROFILE(Type.ERROR),
	
	WARN_RESTORE_ADDON_CANT_INSTALL(Type.WARN),
	WARN_RESTORE_ADDON_INSTALL(Type.WARN),
	WARN_RESTORE_ADDON_ENABLE(Type.WARN),
	WARN_RESTORE_XMOD_IO(Type.WARN),
	WARN_RESTORE_BTHOME(Type.WARN),
	
	RESTORE_LOGIN(Type.ASK),
	RESTORE_WI_FI1(Type.ASK),
	RESTORE_WI_FI2(Type.ASK),
	RESTORE_WI_FI_AP(Type.ASK),
	RESTORE_MQTT(Type.ASK),
	RESTORE_OPEN_MQTT(Type.ASK),
	QUESTION_RESTORE_SCRIPTS_OVERRIDE(Type.ASK),
	QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP(Type.ASK);
	
	public enum Type {
//		INFO,
		PRE,
		WARN,
		ERROR,
		ASK
	}
	
	private Type type;
	
	private RestoreMsg(Type t) {
		type = t;
	}

	public Type getType() {
		return type;
	}
}