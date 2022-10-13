package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.LoginManager;

public class LoginManagerG1 implements LoginManager {
	private AbstractG1Device d;
	private boolean enabled;
	private String user;

	public LoginManagerG1(AbstractG1Device d) throws IOException {
		this.d = d;
		init();
	}
	
	public LoginManagerG1(AbstractG1Device d, boolean noInit) throws IOException {
		this.d = d;
		if(noInit == false) {
			init();
		}
	}

	private void init() throws IOException {
		JsonNode login = d.getJSON("/settings/login");
		this.enabled = login.get("enabled").asBoolean();
		this.user = login.get("username").asText();
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public String disable() {
		String msg = d.sendCommand("/settings/login?enabled=false");
		if(msg == null) {
			d.setCredentialsProvider(null);
		}
		return msg;
	}
	
//	public static String set(AbstractG1Device d, String user, String pwd) {
//		try {
//			LoginManagerG1 l = new LoginManagerG1(d, true);
//			return l.set(user, pwd);
//		} catch (IOException e) {
//			return e.getMessage();
//		}
//	}

	@Override
	public String set(String user, char[] pwd) {
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(/*AuthScope.ANY*/new AuthScope(null, -1), new UsernamePasswordCredentials(user, pwd));
		return set(user, pwd, credsProvider);
	}

	public String set(String user, char[] pwd, CredentialsProvider credsProvider) {
		try {
			String cmd = 
					"/settings/login?enabled=true&username=" + URLEncoder.encode(user, StandardCharsets.UTF_8.toString()) +
					"&password=" + URLEncoder.encode(new String(pwd), StandardCharsets.UTF_8.toString());
			String msg = d.sendCommand(cmd);
			if(msg == null) {
				d.setCredentialsProvider(credsProvider);
			}
			return msg;
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}
}