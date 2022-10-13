package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.Button1;
import it.usna.shellyscan.model.device.g1.Shelly1;
import it.usna.shellyscan.model.device.g1.Shelly1L;
import it.usna.shellyscan.model.device.g1.Shelly1PM;
import it.usna.shellyscan.model.device.g1.Shelly2;
import it.usna.shellyscan.model.device.g1.Shelly25;
import it.usna.shellyscan.model.device.g1.ShellyBulb;
import it.usna.shellyscan.model.device.g1.ShellyDUO;
import it.usna.shellyscan.model.device.g1.ShellyDUORGB;
import it.usna.shellyscan.model.device.g1.ShellyDW;
import it.usna.shellyscan.model.device.g1.ShellyDW2;
import it.usna.shellyscan.model.device.g1.ShellyDimmer;
import it.usna.shellyscan.model.device.g1.ShellyDimmer2;
import it.usna.shellyscan.model.device.g1.ShellyEM;
import it.usna.shellyscan.model.device.g1.ShellyFlood;
import it.usna.shellyscan.model.device.g1.ShellyG1Unmanaged;
import it.usna.shellyscan.model.device.g1.ShellyI3;
import it.usna.shellyscan.model.device.g1.ShellyPlug;
import it.usna.shellyscan.model.device.g1.ShellyPlugE;
import it.usna.shellyscan.model.device.g1.ShellyPlugS;
import it.usna.shellyscan.model.device.g1.ShellyPlugUS;
import it.usna.shellyscan.model.device.g1.ShellyRGBW2;
import it.usna.shellyscan.model.device.g1.ShellyUNI;
import it.usna.shellyscan.model.device.g2.LoginManagerG2;
import it.usna.shellyscan.model.device.g2.ShellyG2Unmanaged;
import it.usna.shellyscan.model.device.g2.ShellyPlus1;
import it.usna.shellyscan.model.device.g2.ShellyPlus1PM;
import it.usna.shellyscan.view.DialogAuthentication;

public class DevicesFactory {
	private DevicesFactory() {}

	private final static Logger LOG = LoggerFactory.getLogger(DevicesFactory.class);
	private static CredentialsProvider lastCredentialsProv;

	public static ShellyAbstractDevice create(final InetAddress address, String name/*, Observable o*/) {
		try {
			final JsonNode info = getDeviceBasicInfo(address);
			if("2".equals(info.path("gen").asText())) {
				return createG2(address, info, name);
			} else {
				return createG1(address, info, name);
			}
		} catch(IOException e) {
			LOG.error("create", e);
			return new ShellyG1Unmanaged(address, name, null, e); 
		}
	}

	private static ShellyAbstractDevice createG1(final InetAddress address, JsonNode info, String name) {
		CredentialsProvider credsProvider = null;
		try {
			final boolean auth = info.get("auth").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // white for this in order to authenticate all subsequent
					if(lastCredentialsProv != null && testAuthentication(address, lastCredentialsProv, "/settings") == HttpURLConnection.HTTP_OK) {
						credsProvider = lastCredentialsProv;
					} else {
						DialogAuthentication credentials = new DialogAuthentication(
								Main.LABELS.getString("dlgAuthTitle"),
								Main.LABELS.getString("labelUser"),
								Main.LABELS.getString("labelPassword"),
								"");
						credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentials.setVisible(true);
							if((user = credentials.getUser()) != null) {
								lastCredentialsProv = credsProvider = LoginManager.getCredentialsProvider(user, new String(credentials.getPassword()));
							}
							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && testAuthentication(address, credsProvider, "/settings") != HttpURLConnection.HTTP_OK);
						credentials.dispose();
					}

				}
			}
			switch(info.get("type").asText()) {
			//case ShellyPlugS.ID: return new Shelly25(address, authStringEnc); // test
			case Shelly1.ID: return new Shelly1(address, credsProvider);
			case Shelly1L.ID: return new Shelly1L(address, credsProvider);
			case Shelly1PM.ID: return new Shelly1PM(address, credsProvider);
			case Shelly2.ID: return new Shelly2(address, credsProvider);
			case Shelly25.ID: return new Shelly25(address, credsProvider);
			case ShellyDimmer.ID: return new ShellyDimmer(address, credsProvider);
			case ShellyDimmer2.ID: return new ShellyDimmer2(address, credsProvider);
			case ShellyDUORGB.ID: return new ShellyDUORGB(address, credsProvider);
			case ShellyDUO.ID: return new ShellyDUO(address, credsProvider);
			case ShellyBulb.ID: return new ShellyBulb(address, credsProvider);
			case ShellyRGBW2.ID: return new ShellyRGBW2(address, credsProvider);
			case ShellyEM.ID: return new ShellyEM(address, credsProvider);
			case ShellyI3.ID: return new ShellyI3(address, credsProvider);
			case Button1.ID: return new Button1(address, credsProvider);
			case ShellyPlugS.ID: return new ShellyPlugS(address, credsProvider);
			case ShellyPlug.ID: return new ShellyPlug(address, credsProvider);
			case ShellyPlugE.ID: return new ShellyPlugE(address, credsProvider);
			case ShellyPlugUS.ID: return new ShellyPlugUS(address, credsProvider);
			case ShellyUNI.ID: return new ShellyUNI(address, credsProvider);
			case ShellyDW.ID: return new ShellyDW(address, credsProvider);
			case ShellyDW2.ID: return new ShellyDW2(address, credsProvider);
			case ShellyFlood.ID: return new ShellyFlood(address, credsProvider);
			default: return new ShellyG1Unmanaged(address, name, credsProvider);
			}
		} catch(Exception e) {
			LOG.warn("create", e);
			return new ShellyG1Unmanaged(address, name, credsProvider, e); 
		}
	}

	private static ShellyAbstractDevice createG2(final InetAddress address, JsonNode info, String name) {
		CredentialsProvider credsProvider = null;
		try {
			final boolean auth = info.get("auth_en").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // white for this in order to authenticate all subsequent
					if(lastCredentialsProv != null && testAuthentication(address, lastCredentialsProv, "/rpc/Shelly.GetStatus") == HttpURLConnection.HTTP_OK) {
						credsProvider = lastCredentialsProv;
					} else {
						DialogAuthentication credentials = new DialogAuthentication(
								Main.LABELS.getString("dlgAuthTitle"),
								Main.LABELS.getString("labelUser"),
								Main.LABELS.getString("labelPassword"),
								LoginManagerG2.LOGIN_USER);
						credentials.showUser(false);
						credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentials.setVisible(true);
							if((user = credentials.getUser()) != null) {
								lastCredentialsProv = credsProvider = LoginManager.getCredentialsProvider(LoginManagerG2.LOGIN_USER, new String(credentials.getPassword()));
							}
							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && testAuthentication(address, credsProvider, "/rpc/Shelly.GetStatus") != HttpURLConnection.HTTP_OK);
						credentials.dispose();
					}
				}
			}
			switch(info.get("app").asText()) {
			case ShellyPlus1.ID: return new ShellyPlus1(address, credsProvider);
			case ShellyPlus1PM.ID: return new ShellyPlus1PM(address, credsProvider);
			default: return new ShellyG2Unmanaged(address, name, credsProvider);
			}
		} catch(Exception e) {
			LOG.warn("create", e);
			return new ShellyG2Unmanaged(address, name, credsProvider); 
		}
	}

	private static JsonNode getDeviceBasicInfo(final InetAddress address) throws IOException {
		final URL url = new URL("http://" + address.getHostAddress() + "/shelly");
		return new ObjectMapper().readTree(url);
	}

	private static int testAuthentication(final InetAddress address, CredentialsProvider credsProvider, String testCommand) {
		HttpHost httpHost = new HttpHost(address, address.getHostAddress(), 80, HttpHost.DEFAULT_SCHEME_NAME);
		AuthCache authCache = new BasicAuthCache();
		authCache.put(httpHost, new BasicScheme());
		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);

		try (CloseableHttpClient httpClient = HttpClients.createDefault(); CloseableHttpResponse response = httpClient.execute(httpHost, new HttpGet(testCommand), context)) {
			return response.getStatusLine().getStatusCode();
		} catch(IOException | RuntimeException e) {
			return HttpURLConnection.HTTP_INTERNAL_ERROR;
		}
	}
}

//todo si potrebbero inviare messaggi per l'autenticazione alla view