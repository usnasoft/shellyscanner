package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.DigestAuthentication;
import org.eclipse.jetty.http.HttpStatus;
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
import it.usna.shellyscan.model.device.g1.Shelly3EM;
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
import it.usna.shellyscan.model.device.g1.ShellyHT;
import it.usna.shellyscan.model.device.g1.ShellyI3;
import it.usna.shellyscan.model.device.g1.ShellyMotion;
import it.usna.shellyscan.model.device.g1.ShellyPlug;
import it.usna.shellyscan.model.device.g1.ShellyPlugE;
import it.usna.shellyscan.model.device.g1.ShellyPlugS;
import it.usna.shellyscan.model.device.g1.ShellyPlugUS;
import it.usna.shellyscan.model.device.g1.ShellyRGBW2;
import it.usna.shellyscan.model.device.g1.ShellyTRV;
import it.usna.shellyscan.model.device.g1.ShellyUNI;
import it.usna.shellyscan.model.device.g2.LoginManagerG2;
import it.usna.shellyscan.model.device.g2.ShellyG2Unmanaged;
import it.usna.shellyscan.model.device.g2.ShellyPlus1;
import it.usna.shellyscan.model.device.g2.ShellyPlus1PM;
import it.usna.shellyscan.model.device.g2.ShellyPlus2PM;
import it.usna.shellyscan.model.device.g2.ShellyPlusi4;
import it.usna.shellyscan.model.device.g2.ShellyPro2;
import it.usna.shellyscan.model.device.g2.ShellyPro2PM;
import it.usna.shellyscan.view.DialogAuthentication;

public class DevicesFactory {
	private DevicesFactory() {}

	private final static Logger LOG = LoggerFactory.getLogger(DevicesFactory.class);
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static CredentialsProvider lastCredentialsProv;

	public static ShellyAbstractDevice create(HttpClient httpClient, final InetAddress address, String name) {
		try {
			final JsonNode info = getDeviceBasicInfo(httpClient, address);
			if("2".equals(info.path("gen").asText())) {
				return createG2(httpClient, address, info, name);
			} else {
				return createG1(httpClient, address, info, name);
			}
		} catch(IOException | TimeoutException | InterruptedException | ExecutionException e) {
			LOG.error("create", e);
			return new ShellyG1Unmanaged(address, name, e); 
		}
	}

	private static ShellyAbstractDevice createG1(HttpClient httpClient, final InetAddress address, JsonNode info, String name) {
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
								Main.LABELS.getString("labelPassword"));
						credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentials.setVisible(true);
							if((user = credentials.getUser()) != null) {
								lastCredentialsProv = credsProvider = LoginManager.getCredentialsProvider(user, credentials.getPassword().clone());
							}
							
							// TEST ********
//							testBasicAuthentication(httpClient, address, user, credentials.getPassword(), "/settings");
							// TEST ********
							
							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && testAuthentication(address, credsProvider, "/settings") != HttpURLConnection.HTTP_OK);
						credentials.dispose();
					}
				}
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			ShellyAbstractDevice d;
			switch(info.get("type").asText()) {
			case Shelly1.ID: d = new Shelly1(address, name);
			break;
			case Shelly1L.ID: d = new Shelly1L(address, name);
			break;
			case Shelly1PM.ID: d = new Shelly1PM(address, name);
			break;
			case Shelly2.ID: d = new Shelly2(address, name);
			break;
			case Shelly25.ID: d = new Shelly25(address, name);
			break;
			case ShellyDimmer.ID: d = new ShellyDimmer(address, name);
			break;
			case ShellyDimmer2.ID: d = new ShellyDimmer2(address, name);
			break;
			case ShellyDUORGB.ID: d = new ShellyDUORGB(address, name);
			break;
			case ShellyDUO.ID: d = new ShellyDUO(address, name);
			break;
			case ShellyBulb.ID: d = new ShellyBulb(address, name);
			break;
			case ShellyRGBW2.ID: d = new ShellyRGBW2(address, name);
			break;
			case ShellyEM.ID: d = new ShellyEM(address, name);
			break;
			case Shelly3EM.ID: d = new Shelly3EM(address, name);
			break;
			case ShellyI3.ID: d = new ShellyI3(address, name);
			break;
			case Button1.ID: d = new Button1(address, info, name);
			break;
			case ShellyPlugS.ID: d = new ShellyPlugS(address, name);
			break;
			case ShellyPlug.ID: d = new ShellyPlug(address, name);
			break;
			case ShellyPlugE.ID: d = new ShellyPlugE(address, name);
			break;
			case ShellyPlugUS.ID: d = new ShellyPlugUS(address, name);
			break;
			case ShellyUNI.ID: d = new ShellyUNI(address, name);
			break;
			case ShellyDW.ID: d = new ShellyDW(address, info, name);
			break;
			case ShellyDW2.ID: d = new ShellyDW2(address, info, name);
			break;
			case ShellyFlood.ID: d = new ShellyFlood(address, info, name);
			break;
			case ShellyHT.ID: d = new ShellyHT(address, info, name);
			break;
			case ShellyMotion.ID: d = new ShellyMotion(address, name);
			break;
			case ShellyTRV.ID: d = new ShellyTRV(address, name);
			break;
			default: d = new ShellyG1Unmanaged(address, name);
			break;
			}
			try {
				d.init(httpClient, credsProvider);
			} catch(IOException e) {
				LOG.warn("create - init", e);
			}
			return d;
		} catch(Exception e) { // really unexpected
			LOG.error("create", e);
			return new ShellyG1Unmanaged(address, name, e); 
		}
	}

	private static ShellyAbstractDevice createG2(HttpClient httpClient, final InetAddress address, JsonNode info, String name) {
		CredentialsProvider credsProvider = null;
		try {
			final boolean auth = info.get("auth_en").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // white for this in order to authenticate all subsequent
					name = info.path("id").asText(name);
					if(lastCredentialsProv != null && testAuthentication(address, lastCredentialsProv, "/rpc/Shelly.GetStatus") == HttpURLConnection.HTTP_OK) {
						credsProvider = lastCredentialsProv;
					} else {
						DialogAuthentication credentials = new DialogAuthentication(
								Main.LABELS.getString("dlgAuthTitle"),
								null,
								Main.LABELS.getString("labelPassword"));
						credentials.setUser(LoginManagerG2.LOGIN_USER);
						credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentials.setVisible(true);
							if((user = credentials.getUser()) != null) {
								
								
								// TEST ********
								testDigestAuthentication(httpClient, address, LoginManagerG2.LOGIN_USER, credentials.getPassword(), "/rpc/Shelly.GetStatus");
								// TEST ********
								
								
								// ... .clone(): DialogAuthentication clear password after dispose() call
								lastCredentialsProv = credsProvider = LoginManager.getCredentialsProvider(LoginManagerG2.LOGIN_USER, credentials.getPassword().clone());
							}
							
							
							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
					
						} while(user != null && testAuthentication(address, credsProvider, "/rpc/Shelly.GetStatus") != HttpURLConnection.HTTP_OK);
						testAuthentication(address, credsProvider, "/rpc/Shelly.GetStatus");
						credentials.dispose();
					}
				}
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			ShellyAbstractDevice d;
			switch(info.get("app").asText()) {
			// Plus
			case ShellyPlus1.ID: d = new ShellyPlus1(address, name);
			break;
			case ShellyPlus1PM.ID: d = new ShellyPlus1PM(address, name);
			break;
			case ShellyPlus2PM.ID: d = new ShellyPlus2PM(address, name);
			break;
			case ShellyPlusi4.ID: d = new ShellyPlusi4(address, name);
			break;
			// PRO
			case ShellyPro2PM.ID: d = new ShellyPro2PM(address, name);
			break;
			case ShellyPro2.ID: d = new ShellyPro2(address, name);
			break;
			default: d = new ShellyG2Unmanaged(address, name);
			break;
			}
			try {
				d.init(httpClient, credsProvider);
			} catch(IOException e) {
				LOG.warn("create - init", e);
			}
			return d;
		} catch(Exception e) { // really unexpected
			LOG.error("create", e);
			return new ShellyG2Unmanaged(address, name); 
		}
	}

	private static JsonNode getDeviceBasicInfo(HttpClient httpClient, final InetAddress address) throws IOException, TimeoutException, InterruptedException, ExecutionException {
		ContentResponse response = httpClient.newRequest("http://" + address.getHostAddress() + "/shelly").send();
		return JSON_MAPPER.readTree(response./*getContentAsString()*/getContent());
	}

	private static int testAuthentication(final InetAddress address, CredentialsProvider credsProvider, String testCommand) {
		HttpHost httpHost = new HttpHost(null, address, address.getHostAddress(), 80);
		AuthCache authCache = new BasicAuthCache();
		authCache.put(httpHost, new BasicScheme());
		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			return httpClient.execute(httpHost, new HttpGet(testCommand), context, response -> {
				return response.getCode();
			});
		} catch(IOException | RuntimeException e) {
			return HttpURLConnection.HTTP_INTERNAL_ERROR;
		}
		// todo https://stackoverflow.com/questions/18108783/apache-httpclient-doesnt-set-basic-authentication-credentials
	}
	
	private static int testBasicAuthentication(HttpClient httpClient, final InetAddress address, String user, char[] pwd, String testCommand) {
		URI uri = URI.create("http://" + address.getHostAddress());
		Authentication.Result creds = new BasicAuthentication.BasicResult(uri, user, new String(pwd));
		Request request = httpClient.newRequest("http://" + address.getHostAddress() + testCommand);
		creds.apply(request);
		try {
			int status = request.send().getStatus();
			if(status == HttpStatus.OK_200) {
				httpClient.getAuthenticationStore().addAuthentication(new BasicAuthentication(uri, BasicAuthentication.ANY_REALM, user, new String(pwd)));
				return HttpStatus.OK_200;
			} else {
				return status;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			return HttpStatus.INTERNAL_SERVER_ERROR_500;
		}
	}
	
	private static int testDigestAuthentication(HttpClient httpClient, final InetAddress address, String user, char[] pwd, String testCommand) {
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

	public static void setCredentialProvider(CredentialsProvider cp) {
		DevicesFactory.lastCredentialsProv = cp;
	}
}