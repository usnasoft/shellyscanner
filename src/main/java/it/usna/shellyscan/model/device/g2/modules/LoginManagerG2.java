package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.AuthenticationStore;
import org.eclipse.jetty.client.DigestAuthentication;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;

import it.usna.shellyscan.model.DeviceUnauthorizedException;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.LoginManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

//https://shelly-api-docs.shelly.cloud/gen2/General/Authentication
//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonServices/Shelly#shellysetauth
public class LoginManagerG2 implements LoginManager {
	public static String LOGIN_USER = "admin";
	private final AbstractG2Device device;
	private boolean enabled;
	private String realm;
	private static Random rnd = new Random();
	private static String ha2;
	static {
		try {
			ha2 = sha256toHex("dummy_method:dummy_uri");
		} catch (NoSuchAlgorithmException e) {
			ha2 = null;
		}
	}
	private static final String DEF_NC = "1";

	public LoginManagerG2(AbstractG2Device d) throws IOException {
		this.device = d;
		init();
	}
	
	public LoginManagerG2(AbstractG2Device d, boolean noInit) throws IOException {
		this.device = d;
		if(noInit == false) {
			init();
		} else {
			this.realm = d.getHostname();
		}
	}

	private void init() throws IOException {
		JsonNode shelly = device.getJSON("/shelly");
		this.enabled = shelly.get("auth_en").asBoolean();
		this.realm = shelly.get("id").asString("");
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
		String msg = device.postCommand("Shelly.SetAuth", "{\"user\":\"" + LOGIN_USER + "\",\"realm\":\"" + realm + "\",\"ha1\":null}");
		if(msg == null) {
			device.setAuthentication(null);
			device.setPwd(null);
		}
		return msg;
	}

	@Override
	public String set(String dummy, char[] pwd) {
		String ha1 = LOGIN_USER + ":" + realm + ":" + new String(pwd);
		try {
			String encodedhash = sha256toHex(ha1);
			String msg = device.postCommand("Shelly.SetAuth", "{\"user\":\"" + LOGIN_USER + "\",\"realm\":\"" + realm + "\",\"ha1\":\"" + encodedhash + "\"}");
			if(msg == null) {
				device.setAuthentication(new DigestAuthentication(URI.create("http://" + device.getAddressAndPort().getRepresentation()), DigestAuthentication.ANY_REALM, LOGIN_USER, new String(pwd)));
				device.setPwd(pwd);
			}
			return msg;
		} catch (NoSuchAlgorithmException e) {
			return e.toString();
		}
	}
	
	public static int testDigestAuthentication(HttpClient httpClient, final InetAddress address, int port, /*String user,*/ char[] pwd, /*String realm,*/ String testCommand) {
		URI uri = URI.create("http://" + address.getHostAddress() + ":" + port);
		DigestAuthentication da = new DigestAuthentication(uri, DigestAuthentication.ANY_REALM/*realm*/, LOGIN_USER, new String(pwd));
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
	
	/**
	 https://shelly-api-docs.shelly.cloud/gen2/General/Authentication
	 (response: string, encoding of the string <ha1> + ":" + <nonce> + ":" + <nc> + ":" + <cnonce> + ":" + "auth" + ":" + <ha2>
	 (ha1: string, <user>:<realm>:<password> encoded in SHA256)
	 (ha2: string, "dummy_method:dummy_uri" encoded in SHA256)
	 */
	private static String getHashResponse(String nonce, String nc, String cnonce, String realm, char[] pwd) throws NoSuchAlgorithmException {
		String ha1 = sha256toHex(LOGIN_USER + ":" + realm + ":" + new String(pwd));
		String resp = ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2;
		return sha256toHex(resp);
	}
	
	private static String sha256toHex(String in) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(in.getBytes(StandardCharsets.UTF_8));
		final StringBuilder hexString = new StringBuilder();
		for (byte b : hash) {
            final String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1)
              hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
	}
	
	public static JsonNode getAuthNode(JsonNode authResp, char[] pwd) throws DeviceUnauthorizedException {
		try {
			String cnonce = "ShSc" + rnd.nextInt();
			ObjectNode auth = (ObjectNode)authResp.deepCopy();
			String nc = auth.hasNonNull("nc") ? auth.remove("nc").asString() : DEF_NC;
			String response = getHashResponse(authResp.get("nonce").asString(), nc, cnonce, authResp.get("realm").asString(), pwd);
			return auth.put("cnonce", cnonce).put("response", response).put("username", LOGIN_USER);
		} catch(NoSuchAlgorithmException | RuntimeException e) {
			throw new DeviceUnauthorizedException(authResp);
		}
	}
	
	// e.g. [Digest qop=auth, realm=shellypmminig3-xxxx, nonce=1770886322, algorithm=SHA-256]
	public static String getAuthString(List<String> wwwAuthenticate, char[] pwd) throws DeviceUnauthorizedException {
		try {
			StringBuilder auth = new StringBuilder("auth.username=admin");
			String realm = null;
			String nonce = null;
			String algorithm = null;
			String nc = DEF_NC;
			for(String val: wwwAuthenticate) {
				if(val.startsWith("realm")) realm = val.substring(6);
				else if(val.startsWith("nonce")) nonce = val.substring(6);
				else if(val.startsWith("algorithm")) algorithm = val.substring(10);
				else if(val.startsWith("nc")) nc = val.substring(3);
			}
			String cnonce = "ShSc" + rnd.nextInt();
			String response = getHashResponse(nonce, nc, cnonce, realm, pwd);
			auth.append("&auth.realm=").append(URLEncoder.encode(realm, StandardCharsets.UTF_8.name()));
			auth.append("&auth.nonce=").append(URLEncoder.encode(nonce, StandardCharsets.UTF_8.name()));
			auth.append("&auth.cnonce=").append(URLEncoder.encode(cnonce, StandardCharsets.UTF_8.name()));
			auth.append("&auth.algorithm=").append(URLEncoder.encode(algorithm, StandardCharsets.UTF_8.name()));
			auth.append("&auth.response=").append(URLEncoder.encode(response, StandardCharsets.UTF_8.name()));
			auth.append("&auth.nc=").append(URLEncoder.encode(nc, StandardCharsets.UTF_8.name()));
			return auth.toString();
		} catch(NoSuchAlgorithmException | RuntimeException | UnsupportedEncodingException e) {
			throw new DeviceUnauthorizedException();
		}
	}
}