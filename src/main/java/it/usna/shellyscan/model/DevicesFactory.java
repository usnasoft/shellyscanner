package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.Button1;
import it.usna.shellyscan.model.device.g1.LoginManagerG1;
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
import it.usna.shellyscan.model.device.g2.ShellyPlusPlugIT;
import it.usna.shellyscan.model.device.g2.ShellyPlusi4;
import it.usna.shellyscan.model.device.g2.ShellyPro1;
import it.usna.shellyscan.model.device.g2.ShellyPro2;
import it.usna.shellyscan.model.device.g2.ShellyPro2PM;
import it.usna.shellyscan.model.device.g2.ShellyPro4PM;
import it.usna.shellyscan.view.DialogAuthentication;

public class DevicesFactory {
	private DevicesFactory() {}

	private final static Logger LOG = LoggerFactory.getLogger(DevicesFactory.class);
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
//	private static CredentialsProvider lastCredentialsProv;
	private static String lastUser;
	private static char[] lastP;

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
		ShellyAbstractDevice d;
		try {
			final boolean auth = info.get("auth").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // white for this in order to authenticate all subsequent
					//					if(lastCredentialsProv != null && testAuthentication(address, lastCredentialsProv, "/settings") == HttpURLConnection.HTTP_OK) {
					//						credsProvider = lastCredentialsProv;
					//					} else {
					//						DialogAuthentication credentials = new DialogAuthentication(
					//								Main.LABELS.getString("dlgAuthTitle"),
					//								Main.LABELS.getString("labelUser"),
					//								Main.LABELS.getString("labelPassword"));
					//						credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
					//						String user;
					//						do {
					//							credentials.setVisible(true);
					//							if((user = credentials.getUser()) != null) {
					//								lastCredentialsProv = credsProvider = LoginManager.getCredentialsProvider(user, credentials.getPassword().clone());
					//							}
					//							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
					//						} while(user != null && testAuthentication(address, credsProvider, "/settings") != HttpURLConnection.HTTP_OK);
					//						credentials.dispose();
					//					}
					if(lastUser == null || LoginManagerG1.testBasicAuthentication(httpClient, address, lastUser, lastP, "/settings") != HttpStatus.OK_200) {
						DialogAuthentication credentials = new DialogAuthentication(
								Main.LABELS.getString("dlgAuthTitle"),
								Main.LABELS.getString("labelUser"),
								Main.LABELS.getString("labelPassword"));
						credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentials.setVisible(true);
							if((user = credentials.getUser()) != null) {
								setCredential(user, credentials.getPassword().clone());
							}
							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && LoginManagerG1.testBasicAuthentication(httpClient, address, lastUser, lastP, "/settings") != HttpStatus.OK_200);
						credentials.dispose();
					}
				}
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			
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
		} catch(Exception e) { // really unexpected
			LOG.error("create", e);
			d = new ShellyG1Unmanaged(address, name, e);
		}
		try {
			d.init(httpClient);
		} catch(IOException e) {
			if("Status-401".equals(e.getMessage()) == false) {
				LOG.warn("create - init", e);
			}
		} catch(RuntimeException e) {
			LOG.error("create - init", e);
		}
		return d;
	}

	private static ShellyAbstractDevice createG2(HttpClient httpClient, final InetAddress address, JsonNode info, String name) {
		ShellyAbstractDevice d;
		try {
			final boolean auth = info.get("auth_en").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // white for this in order to authenticate all subsequent
					if(lastUser == null || LoginManagerG2.testDigestAuthentication(httpClient, address, LoginManagerG2.LOGIN_USER, lastP, "/rpc/Shelly.GetStatus") != HttpStatus.OK_200) {
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
								setCredential(user, credentials.getPassword().clone()); // ... .clone(): DialogAuthentication clear password after dispose() call
							}
							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && LoginManagerG2.testDigestAuthentication(httpClient, address, LoginManagerG2.LOGIN_USER, lastP, "/rpc/Shelly.GetStatus") != HttpStatus.OK_200);
						credentials.dispose();
					}
//					name = info.path("id").asText(name);
//					if(lastCredentialsProv != null && testAuthentication(address, lastCredentialsProv, "/rpc/Shelly.GetStatus") == HttpURLConnection.HTTP_OK) {
//						credsProvider = lastCredentialsProv;
//					} else {
//						DialogAuthentication credentials = new DialogAuthentication(
//								Main.LABELS.getString("dlgAuthTitle"),
//								null,
//								Main.LABELS.getString("labelPassword"));
//						credentials.setUser(LoginManagerG2.LOGIN_USER);
//						credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
//						String user;
//						do {
//							credentials.setVisible(true);
//							if((user = credentials.getUser()) != null) {
//								lastCredentialsProv = credsProvider = LoginManager.getCredentialsProvider(LoginManagerG2.LOGIN_USER, credentials.getPassword().clone());
//							}
//							credentials.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
//					
//						} while(user != null && testAuthentication(address, credsProvider, "/rpc/Shelly.GetStatus") != HttpURLConnection.HTTP_OK);
//						testAuthentication(address, credsProvider, "/rpc/Shelly.GetStatus");
//						credentials.dispose();
//					}
				}
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
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
			case ShellyPlusPlugIT.ID: d = new ShellyPlusPlugIT(address, name);
			break;
			// PRO
			case ShellyPro1.ID: d = new ShellyPro1(address, name);
			break;
			case ShellyPro2PM.ID: d = new ShellyPro2PM(address, name);
			break;
			case ShellyPro2.ID: d = new ShellyPro2(address, name);
			break;
			case ShellyPro4PM.ID: d = new ShellyPro4PM(address, name);
			break;
			default: d = new ShellyG2Unmanaged(address, name);
			break;
			}
		} catch(Exception e) { // really unexpected
			LOG.error("create", e);
			d = new ShellyG2Unmanaged(address, name, e);
		}
		try {
			d.init(httpClient);
		} catch(IOException e) {
			if("Status-401".equals(e.getMessage()) == false) {
				LOG.warn("create - init", e);
			}
		} catch(RuntimeException e) {
			LOG.error("create - init", e);
		}
		return d;
	}

	private static JsonNode getDeviceBasicInfo(HttpClient httpClient, final InetAddress address) throws IOException, TimeoutException, InterruptedException, ExecutionException {
		ContentResponse response = httpClient.newRequest("http://" + address.getHostAddress() + "/shelly").send();
		return JSON_MAPPER.readTree(response.getContent());
	}
	
	public static void setCredential(String user, char[] p) {
		lastUser = user;
		lastP = p;
	}
}