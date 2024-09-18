package it.usna.shellyscan.model.device.modules;

public interface LoginManager {
	
	public boolean isEnabled();
	
	public String getUser();
	
	public String disable();

	public String set(String user, char[] pwd);
}