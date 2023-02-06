package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.util.DigestAuthentication;
import org.eclipse.jetty.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.LoginManager;

//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonServices/Shelly#shellysetauth
public class LoginManagerG2 implements LoginManager {
	public static String LOGIN_USER = "admin";
	private final AbstractG2Device d;
	private boolean enabled;
	private String realm;

	public LoginManagerG2(AbstractG2Device d) throws IOException {
		this.d = d;
		init();
	}
	
	public LoginManagerG2(AbstractG2Device d, boolean noInit) throws IOException {
		this.d = d;
		if(noInit == false) {
			init();
		} else {
			this.realm = d.getHostname();
		}
	}

	private void init() throws IOException {
		JsonNode shelly = d.getJSON("/shelly");
		this.enabled = shelly.get("auth_en").asBoolean();
		this.realm = shelly.get("id").asText();
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String getUser() {
		return LOGIN_USER;
	}

	@Override
	public String disable() {
		String msg = d.postCommand("Shelly.SetAuth", "{\"user\":\"" + LOGIN_USER + "\",\"realm\":\"" + realm + "\",\"ha1\":null}");
		if(msg == null) {
			d.setCredentialsProvider(null);
		}
		return msg;
	}
	
//	public static String set(AbstractG2Device d, String pwd) {
//		try {
//			LoginManagerG2 l = new LoginManagerG2(d, true);
//			return l.set("", pwd);
//		} catch (IOException e) {
//			return e.getMessage();
//		}
//	}

	@Override
	public String set(String dummy, char[] pwd) {
		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(/*AuthScope.ANY*/new AuthScope(null, -1), new UsernamePasswordCredentials(LOGIN_USER, pwd));
		return set(null, pwd, credsProvider);
	}

	public String set(String dummy, char[] pwd, CredentialsProvider credsProvider) {
		String ha1 = LOGIN_USER + ":" + realm + ":" + new String(pwd);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String encodedhash = bytesToHex(digest.digest(ha1.getBytes(StandardCharsets.UTF_8)));
			String msg = d.postCommand("Shelly.SetAuth", "{\"user\":\"" + LOGIN_USER + "\",\"realm\":\"" + realm + "\",\"ha1\":\"" + encodedhash + "\"}");
			if(msg == null) {
				d.setCredentialsProvider(credsProvider);
			}
			return msg;
		} catch (NoSuchAlgorithmException e) {
			return e.toString();
		}
	}
	
	private static String bytesToHex(byte[] hash) {
	    StringBuilder sb = new StringBuilder();
	    for (byte b : hash) {
	        sb.append(String.format("%02x", b));
	    }
	   return sb.toString();
	}
	
	public static int testDigestAuthentication(HttpClient httpClient, final InetAddress address, String user, char[] pwd, String testCommand) {
		URI uri = URI.create("http://" + address.getHostAddress() /*+ testCommand*/);
		DigestAuthentication da = new DigestAuthentication(uri, DigestAuthentication.ANY_REALM, user, new String(pwd));
		AuthenticationStore aStore = httpClient.getAuthenticationStore();
		try {
			aStore.addAuthentication(da);
			int status = httpClient.GET("http://" + address.getHostAddress() + testCommand).getStatus();
			if(status == HttpStatus.OK_200) {
				return HttpStatus.OK_200;
			} else {
				aStore.removeAuthentication(da);
				return status;
			}
		} catch (InterruptedException | TimeoutException | ExecutionException e) {
			aStore.removeAuthentication(da);
			return HttpStatus.INTERNAL_SERVER_ERROR_500;
		}
	}
}