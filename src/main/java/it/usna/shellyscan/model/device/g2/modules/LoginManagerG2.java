package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
	private final AbstractG2Device d;
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
		String msg = d.postCommand("Shelly.SetAuth", "{\"user\":\"" + LOGIN_USER + "\",\"realm\":\"" + realm + "\",\"ha1\":null}");
		if(msg == null) {
			d.setAuthentication(null);
		}
		return msg;
	}

	@Override
	public String set(String dummy, char[] pwd) {
		String ha1 = LOGIN_USER + ":" + realm + ":" + new String(pwd);
		try {
			String encodedhash = sha256toHex(ha1);
			String msg = d.postCommand("Shelly.SetAuth", "{\"user\":\"" + LOGIN_USER + "\",\"realm\":\"" + realm + "\",\"ha1\":\"" + encodedhash + "\"}");
			if(msg == null) {
				d.setAuthentication(new DigestAuthentication(URI.create("http://" + d.getAddressAndPort().getRepresentation()), DigestAuthentication.ANY_REALM, LOGIN_USER, new String(pwd)));
			}
			return msg;
		} catch (NoSuchAlgorithmException e) {
			return e.toString();
		}
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
			String nc = auth.remove("nc").asString();
			String response = getHashResponse(authResp.get("nonce").asString(), nc, cnonce, authResp.get("realm").asString(), pwd);
			return auth.put("cnonce", cnonce).put("response", response).put("username", LOGIN_USER);
		} catch(NoSuchAlgorithmException | RuntimeException e) {
			throw new DeviceUnauthorizedException(authResp);
		}
	}
	
//	public static void main(String ...strings) throws NoSuchAlgorithmException {
//		// ws://192.168.1.30/debug/log?
//		// auth.username=admin
//		//auth.realm=ShellyWallDisplay-00A90B3358D4
//		//auth.nonce=1769249562
//		//auth.cnonce=1769249562579
//		//auth.algorithm=SHA-256
//		//auth.response=4dfcde8a65a150467cb21edbc8c971323005c397ac3512c593559296071055ba
//		//auth.nc=0000002a
//		System.out.println(getResponse("1769249562", "0000002a", "1769249562579", "ShellyWallDisplay-00A90B3358D4", "1234"));
//	}
}