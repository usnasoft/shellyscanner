package it.usna.shellyscan.model.device;

public interface MQTTManager {
	
	public boolean isEnabled();
	
	public String getServer();
	
	public String getUser();
	
	public String getPrefix();
	
	public String disable();
	
	public String set(String server, String user, String pwd, String prefix);
}
