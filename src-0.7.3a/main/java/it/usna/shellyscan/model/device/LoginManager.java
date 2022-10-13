package it.usna.shellyscan.model.device;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

public interface LoginManager {
	public boolean isEnabled();
	
	public String getUser();
	
	public String disable();

	public String set(String user, String pwd);
	
	public String set(String user, String pwd, CredentialsProvider credsProvider);
	
	public static CredentialsProvider getCredentialsProvider(String user, String pwd) {
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, new String(pwd)));
		return credsProvider;
	}
}
