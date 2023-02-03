package it.usna.shellyscan.model.device;

import java.net.URI;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.util.BasicAuthentication;

public interface LoginManager {
	public boolean isEnabled();
	
	public String getUser();
	
	public String disable();

	public String set(String user, char[] pwd);
	
	public String set(String user, char[] pwd, CredentialsProvider credsProvider);
	
	public static CredentialsProvider getCredentialsProvider(String user, char[] pwd) {
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(user, pwd));
		return credsProvider;
	}
	
	public static Authentication.Result getCredentialsProvider(String uriStr, String user, char[] pwd) {
		URI uri = URI.create(uriStr);
		return new BasicAuthentication.BasicResult(uri, user, new String(pwd));
	}
}