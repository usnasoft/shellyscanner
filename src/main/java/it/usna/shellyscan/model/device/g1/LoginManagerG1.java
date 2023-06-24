package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.http.HttpStatus;

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
//			d.setAuthentication(null);
			d.setAuthenticationResult(null);
		}
		return msg;
	}

	@Override
	public String set(String user, char[] pwd) {
		try {
			String cmd = 
					"/settings/login?enabled=true&username=" + URLEncoder.encode(user, StandardCharsets.UTF_8.toString()) +
					"&password=" + URLEncoder.encode(new String(pwd), StandardCharsets.UTF_8.toString());
			String msg = d.sendCommand(cmd);
			if(msg == null) {
//				d.setAuthentication(new BasicAuthentication(URI.create("http://" + d.getAddress().getHostAddress()), BasicAuthentication.ANY_REALM, user, new String(pwd)));
				d.setAuthenticationResult(new BasicAuthentication.BasicResult(URI.create("http://" + d.getAddress().getHostAddress()  + ":" + d.getPort()), user, new String(pwd)));
			}
			return msg;
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}
	
	public static int testBasicAuthentication(HttpClient httpClient, final InetAddress address, int port, String user, char[] pwd, String testCommand) {
		final URI uri = URI.create("http://" + address.getHostAddress() + ":" + port);
		Authentication.Result creds = new BasicAuthentication.BasicResult(uri, user, new String(pwd));
		Request request = httpClient.newRequest("http://" + address.getHostAddress() + ":" + port + testCommand);
		creds.apply(request);
		try {
			int status = request.send().getStatus();
			if(status == HttpStatus.OK_200) {
//				httpClient.getAuthenticationStore().addAuthentication(new BasicAuthentication(uri, BasicAuthentication.ANY_REALM, user, new String(pwd)));
				httpClient.getAuthenticationStore().addAuthenticationResult(creds);
			}
			return status;
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			return HttpStatus.INTERNAL_SERVER_ERROR_500;
		}
	}
}