package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.AuthenticationStore;
import org.eclipse.jetty.client.DigestAuthentication;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.LoginManager;

//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonServices/Shelly#shellysetauth
//https://shelly-api-docs.shelly.cloud/gen2/0.14/General/Authentication#authentication
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
			d.setAuthentication(null);
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

//	@Override
//	public String set(String dummy, char[] pwd) {
//		BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
//		credsProvider.setCredentials(/*AuthScope.ANY*/new AuthScope(null, -1), new UsernamePasswordCredentials(LOGIN_USER, pwd));
//		return set(null, pwd, credsProvider);
//	}

	@Override
	public String set(String dummy, char[] pwd) {
		String ha1 = LOGIN_USER + ":" + realm + ":" + new String(pwd);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String encodedhash = bytesToHex(digest.digest(ha1.getBytes(StandardCharsets.UTF_8)));
			String msg = d.postCommand("Shelly.SetAuth", "{\"user\":\"" + LOGIN_USER + "\",\"realm\":\"" + realm + "\",\"ha1\":\"" + encodedhash + "\"}");
			if(msg == null) {
//				d.setCredentialsProvider(credsProvider);
				d.setAuthentication(new DigestAuthentication(URI.create("http://" + d.getAddress().getHostAddress() + ":" + d.getPort()), DigestAuthentication.ANY_REALM, LOGIN_USER, new String(pwd)));
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
	
	public static int testDigestAuthentication(HttpClient httpClient, final InetAddress address, int port, /*String user,*/ char[] pwd, String testCommand) {
		URI uri = URI.create("http://" + address.getHostAddress() + ":" + port/*+ testCommand*/);
		DigestAuthentication da = new DigestAuthentication(uri, DigestAuthentication.ANY_REALM, LOGIN_USER, new String(pwd));
		AuthenticationStore aStore = httpClient.getAuthenticationStore();
		try {
			aStore.addAuthentication(da);
			int status = httpClient.GET("http://" + address.getHostAddress() + ":" + port + testCommand).getStatus();
			if(status != HttpStatus.OK_200) {
				aStore.removeAuthentication(da);
			}
			return status;
		} catch (InterruptedException | TimeoutException | ExecutionException e) {
			aStore.removeAuthentication(da);
			return HttpStatus.INTERNAL_SERVER_ERROR_500;
		}
	}
	
	////////////////////////
	// https://shelly-api-docs.shelly.cloud/gen2/General/Authentication
	// (response: string, encoding of the string <ha1> + ":" + <nonce> + ":" + <nc> + ":" + <cnonce> + ":" + "auth" + ":" + <ha2>
	// (ha1: string, <user>:<realm>:<password> encoded in SHA256)
	// (ha2: string, "dummy_method:dummy_uri" encoded in SHA256)
	
//	public static String getResponse(String nonce, String cnonce, String realm, String pwd) throws NoSuchAlgorithmException {
//		String ha1 = sha256toHex("admin:" + realm + ":" + pwd);
//		String ha2 = sha256toHex("dummy_method:dummy_uri"); // const
//		String resp = ha1 + ":" + nonce + ":1:" + cnonce + ":auth:" + ha2;
//		return sha256toHex(resp);
//	}
//	
//	private static String sha256toHex(String in) throws NoSuchAlgorithmException {
//		MessageDigest digest = MessageDigest.getInstance("SHA-256");
//		byte[] hash = digest.digest(in.getBytes(StandardCharsets.UTF_8));
//		final StringBuilder hexString = new StringBuilder();
//        for (int i = 0; i < hash.length; i++) {
//            final String hex = Integer.toHexString(0xff & hash[i]);
//            if(hex.length() == 1) 
//              hexString.append('0');
//            hexString.append(hex);
//        }
//        return hexString.toString();
//	}
	
//	public static void main(String ...strings) throws NoSuchAlgorithmException {
//		getResponse("1714902300", "shellyplus2pm-485519a2bb1c", "1234");
//	}
}