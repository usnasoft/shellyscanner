package it.usna.shellyscan.model.device;

public interface LoginManager {
	public boolean isEnabled();
	
	public String getUser();
	
	public String disable();

	public String set(String user, char[] pwd);
	
//	public String set(String user, char[] pwd, CredentialsProvider credsProvider);
}