package it.usna.shellyscan.model.device.modules;

public interface LoginManager {
	boolean isEnabled();
	
	String getUser();
	
	String disable();

	String set(String user, char[] pwd);
}