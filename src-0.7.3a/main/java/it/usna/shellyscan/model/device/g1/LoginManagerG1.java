package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.LoginManager;

public class LoginManagerG1 implements LoginManager {
	private final AbstractG1Device d;
	private boolean enabled;
	private String user;

	public LoginManagerG1(AbstractG1Device d) throws IOException {
		this.d = d;
		init();
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

	@Override
	public String set(String user, String pwd) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pwd));
		return set(user, pwd, credsProvider);
	}

	public String set(String user, String pwd, CredentialsProvider credsProvider) {
		try {
			String cmd = 
					"/settings/login?enabled=true&username=" + URLEncoder.encode(user, StandardCharsets.UTF_8.toString()) +
					"&password=" + URLEncoder.encode(pwd, StandardCharsets.UTF_8.toString());
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