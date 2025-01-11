package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyGenericUnmanagedImpl;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.blu.BTHomeDevice;
import it.usna.shellyscan.model.device.blu.BluTRV;
import it.usna.shellyscan.model.device.blu.ShellyBluUnmanaged;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
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
import it.usna.shellyscan.model.device.g1.ShellyMotion2;
import it.usna.shellyscan.model.device.g1.ShellyPlug;
import it.usna.shellyscan.model.device.g1.ShellyPlugE;
import it.usna.shellyscan.model.device.g1.ShellyPlugS;
import it.usna.shellyscan.model.device.g1.ShellyPlugUS;
import it.usna.shellyscan.model.device.g1.ShellyRGBW2;
import it.usna.shellyscan.model.device.g1.ShellyTRV;
import it.usna.shellyscan.model.device.g1.ShellyUNI;
import it.usna.shellyscan.model.device.g1.modules.LoginManagerG1;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.ShellyG2Unmanaged;
import it.usna.shellyscan.model.device.g2.ShellyGateway;
import it.usna.shellyscan.model.device.g2.ShellyMini1;
import it.usna.shellyscan.model.device.g2.ShellyMini1PM;
import it.usna.shellyscan.model.device.g2.ShellyMiniPM;
import it.usna.shellyscan.model.device.g2.ShellyPlus0_10VDimmer;
import it.usna.shellyscan.model.device.g2.ShellyPlus1;
import it.usna.shellyscan.model.device.g2.ShellyPlus1PM;
import it.usna.shellyscan.model.device.g2.ShellyPlus2PM;
import it.usna.shellyscan.model.device.g2.ShellyPlusHT;
import it.usna.shellyscan.model.device.g2.ShellyPlusPlugIT;
import it.usna.shellyscan.model.device.g2.ShellyPlusPlugS;
import it.usna.shellyscan.model.device.g2.ShellyPlusPlugUK;
import it.usna.shellyscan.model.device.g2.ShellyPlusPlugUS;
import it.usna.shellyscan.model.device.g2.ShellyPlusRGBW;
import it.usna.shellyscan.model.device.g2.ShellyPlusSmoke;
import it.usna.shellyscan.model.device.g2.ShellyPlusUNI;
import it.usna.shellyscan.model.device.g2.ShellyPlusi4;
import it.usna.shellyscan.model.device.g2.ShellyPro1;
import it.usna.shellyscan.model.device.g2.ShellyPro1PM;
import it.usna.shellyscan.model.device.g2.ShellyPro2;
import it.usna.shellyscan.model.device.g2.ShellyPro2PM;
import it.usna.shellyscan.model.device.g2.ShellyPro3;
import it.usna.shellyscan.model.device.g2.ShellyPro3EM;
import it.usna.shellyscan.model.device.g2.ShellyPro4PM;
import it.usna.shellyscan.model.device.g2.ShellyProDimmer1;
import it.usna.shellyscan.model.device.g2.ShellyProEM50;
import it.usna.shellyscan.model.device.g2.ShellyWallDimmer;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.model.device.g2.modules.LoginManagerG2;
import it.usna.shellyscan.model.device.g3.AbstractG3Device;
import it.usna.shellyscan.model.device.g3.Shelly0_10VPMG3;
import it.usna.shellyscan.model.device.g3.Shelly1G3;
import it.usna.shellyscan.model.device.g3.Shelly1PMG3;
import it.usna.shellyscan.model.device.g3.Shelly2PMG3;
import it.usna.shellyscan.model.device.g3.ShellyG3Unmanaged;
import it.usna.shellyscan.model.device.g3.ShellyGatewayG3;
import it.usna.shellyscan.model.device.g3.ShellyHTG3;
import it.usna.shellyscan.model.device.g3.ShellyI4G3;
import it.usna.shellyscan.model.device.g3.ShellyMini1G3;
import it.usna.shellyscan.model.device.g3.ShellyMini1PMG3;
import it.usna.shellyscan.model.device.g3.ShellyMiniPMG3;
import it.usna.shellyscan.model.device.g3.ShellyPlugSG3;
import it.usna.shellyscan.model.device.g3.ShellyXMOD1;
import it.usna.shellyscan.view.DialogAuthentication;

public class DevicesFactory {
	private DevicesFactory() {}

	private final static Logger LOG = LoggerFactory.getLogger(DevicesFactory.class);
	private static String lastUser;
	private static char[] lastP;
	
	public static ShellyAbstractDevice create(HttpClient httpClient, WebSocketClient wsClient, final InetAddress address, int port, JsonNode info, String name) {
		final int gen = info.path("gen").intValue();
		if(gen == 2) {
			return createG2(httpClient, wsClient, address, port, info, name);
		} else if(gen == 0) { // gen1 (info.get("gen") == null)
			return createG1(httpClient, address, port, info, name);
		} else if(gen >= 3) {
			return createG3(httpClient, wsClient, address, port, info, name);
		} else { // unknown gen
			return new ShellyGenericUnmanagedImpl(address, port, name, httpClient);
		}
	}

	public static ShellyAbstractDevice createWithError(HttpClient httpClient, final InetAddress address, int port, String name, Throwable e) {
		return new ShellyGenericUnmanagedImpl(address, port, name, httpClient, e);
	}

	private static AbstractG1Device createG1(HttpClient httpClient, final InetAddress address, int port, JsonNode info, String name) {
		AbstractG1Device d;
		try {
			final boolean auth = info.get("auth").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // wait for this in order to authenticate next protected devices
					if(lastUser == null || LoginManagerG1.testBasicAuthentication(httpClient, address, port, lastUser, lastP, "/settings") != HttpStatus.OK_200) {
						DialogAuthentication credentialsDlg = new DialogAuthentication(
								Main.LABELS.getString("dlgAuthTitle"),
								Main.LABELS.getString("labelUser"),
								Main.LABELS.getString("labelPassword"));
						credentialsDlg.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentialsDlg.setVisible(true);
							if((user = credentialsDlg.getUser()) != null) {
								setCredential(user, credentialsDlg.getPassword().clone());
							}
							credentialsDlg.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && LoginManagerG1.testBasicAuthentication(httpClient, address, port, lastUser, lastP, "/settings") != HttpStatus.OK_200);
						credentialsDlg.dispose();
					}
				}
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			
			d = switch(info.get("type").asText()) {
				case Shelly1.ID -> new Shelly1(address, port, name);
				case Shelly1L.ID -> new Shelly1L(address, port, name);
				case Shelly1PM.ID -> new Shelly1PM(address, port, name);
				case Shelly2.ID -> new Shelly2(address, port, name);
				case Shelly25.ID -> new Shelly25(address, port, name);
				case ShellyDimmer.ID -> new ShellyDimmer(address, port, name);
				case ShellyDimmer2.ID -> new ShellyDimmer2(address, port, name);
				case ShellyDUORGB.ID -> new ShellyDUORGB(address, port, name);
				case ShellyDUO.ID -> new ShellyDUO(address, port, name);
				case ShellyBulb.ID -> new ShellyBulb(address, port, name);
				case ShellyRGBW2.ID -> new ShellyRGBW2(address, port, name);
				case ShellyEM.ID -> new ShellyEM(address, port, name);
				case Shelly3EM.ID -> new Shelly3EM(address, port, name);
				case ShellyI3.ID -> new ShellyI3(address, port, name);
				case Button1.ID -> new Button1(address, port, name);
				case ShellyPlugS.ID -> new ShellyPlugS(address, port, name);
				case ShellyPlug.ID -> new ShellyPlug(address, port, name);
				case ShellyPlugE.ID -> new ShellyPlugE(address, port, name);
				case ShellyPlugUS.ID -> new ShellyPlugUS(address, port, name);
				case ShellyUNI.ID -> new ShellyUNI(address, port, name);
				// Battery
				case ShellyDW.ID -> new ShellyDW(address, port, name);
				case ShellyDW2.ID -> new ShellyDW2(address, port, name);
				case ShellyFlood.ID -> new ShellyFlood(address, port, name);
				case ShellyHT.ID -> new ShellyHT(address, port, name);
				case ShellyMotion.ID -> new ShellyMotion(address, port, name);
				case ShellyMotion2.ID -> new ShellyMotion2(address, port, name);
				case ShellyTRV.ID -> new ShellyTRV(address, port, name);
				default -> new ShellyG1Unmanaged(address, port, name);
			};
		} catch(Exception e) { // really unexpected
			LOG.error("create", e);
			d = new ShellyG1Unmanaged(address, port, name, e);
		}
		try {
			d.init(httpClient, info);
		} catch(IOException e) {
			if("Status-401".equals(e.getMessage()) == false) {
				LOG.warn("create - init", e);
			}
		} catch(RuntimeException e) {
			LOG.error("create - init {}:{}", address, port, e);
		}
		return d;
	}

	private static AbstractG2Device createG2(HttpClient httpClient, WebSocketClient wsClient, final InetAddress address, int port, JsonNode info, String name) {
		AbstractG2Device d;
		try {
			final boolean auth = info.get("auth_en").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // wait for this in order to authenticate all subsequent
					if(lastUser == null || LoginManagerG2.testDigestAuthentication(httpClient, address, port, lastP, "/rpc/Shelly.GetStatus") != HttpStatus.OK_200) {
						DialogAuthentication credentialsDlg = new DialogAuthentication(
								Main.LABELS.getString("dlgAuthTitle"),
								null /*labelUser*/,
								Main.LABELS.getString("labelPassword"));
						credentialsDlg.setUser(LoginManagerG2.LOGIN_USER);
						credentialsDlg.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentialsDlg.setVisible(true);
							if((user = credentialsDlg.getUser()) != null) {
								setCredential(user, credentialsDlg.getPassword().clone()); // ... .clone(): DialogAuthentication clear password after dispose() call
							}
							credentialsDlg.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && LoginManagerG2.testDigestAuthentication(httpClient, address, port, lastP, "/rpc/Shelly.GetStatus") != HttpStatus.OK_200);
						credentialsDlg.dispose();
					}
//					URI uri = URI.create("http://" + address.getHostAddress() + ":" + port/*+ testCommand*/);
//					Authentication.Result creds = new BasicAuthentication.BasicResult(uri, "admin", new String( "1234"));
//					httpClient.getAuthenticationStore().addAuthenticationResult(creds);
				}
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			d = switch(info.get("app").asText()) {
				// Plus
				case ShellyPlus1.ID -> new ShellyPlus1(address, port, name);
				case ShellyPlus1PM.ID -> new ShellyPlus1PM(address, port, name);
				case ShellyPlus2PM.ID -> new ShellyPlus2PM(address, port, name);
				case ShellyPlusi4.ID -> new ShellyPlusi4(address, port, name);
				case ShellyMini1.ID -> new ShellyMini1(address, port, name);
				case ShellyMini1PM.ID -> new ShellyMini1PM(address, port, name);
				case ShellyMiniPM.ID -> new ShellyMiniPM(address, port, name);
				case ShellyPlusPlugS.ID -> new ShellyPlusPlugS(address, port, name);
				case ShellyPlusPlugUK.ID -> new ShellyPlusPlugUK(address, port, name);
				case ShellyPlusPlugIT.ID -> new ShellyPlusPlugIT(address, port, name);
				case ShellyPlusPlugUS.ID -> new ShellyPlusPlugUS(address, port, name);
				case ShellyWallDimmer.ID -> new ShellyWallDimmer(address, port, name);
				case ShellyPlusRGBW.ID -> new ShellyPlusRGBW(address, port, name);
				case ShellyPlus0_10VDimmer.ID -> new ShellyPlus0_10VDimmer(address, port, name);
				case ShellyGateway.ID -> new ShellyGateway(address, port, name);
				case WallDisplay.ID -> new WallDisplay(address, port, name);
				case ShellyPlusUNI.ID -> new ShellyPlusUNI(address, port, name);
				// Plus - Battery
				case ShellyPlusHT.ID -> new ShellyPlusHT(address, port, name);
				case ShellyPlusSmoke.ID -> new ShellyPlusSmoke(address, port, name);
				// PRO
				case ShellyPro1PM.ID -> new ShellyPro1PM(address, port, name);
				case ShellyPro1.ID -> new ShellyPro1(address, port, name);
				case ShellyPro2PM.ID -> new ShellyPro2PM(address, port, name);
				case ShellyPro2.ID -> new ShellyPro2(address, port, name);
				case ShellyPro3.ID -> new ShellyPro3(address, port, name);
				case ShellyPro4PM.ID -> new ShellyPro4PM(address, port, name);
				case ShellyProDimmer1.ID -> new ShellyProDimmer1(address, port, name);
				case ShellyProEM50.ID -> new ShellyProEM50(address, port, name);
				case ShellyPro3EM.ID -> new ShellyPro3EM(address, port, name);
				default -> new ShellyG2Unmanaged(address, port, name);
			};
		} catch(Exception e) { // really unexpected
			LOG.error("create", e);
			d = new ShellyG2Unmanaged(address, port, name, e);
		}
		try {
			d.init(httpClient, wsClient, info);
		} catch(IOException e) {
			if("Status-401".equals(e.getMessage()) == false) {
				LOG.warn("create - init", e);
			}
		} catch(RuntimeException e) {
			LOG.error("create - init {}:{}", address, port, e);
		}
		return d;
	}
	
	private static AbstractG3Device createG3(HttpClient httpClient, WebSocketClient wsClient, final InetAddress address, int port, JsonNode info, String name) {
		AbstractG3Device d;
		try {
			final boolean auth = info.get("auth_en").asBoolean();
			if(auth) {
				synchronized (DevicesFactory.class) { // wait for this in order to authenticate all subsequent
					if(lastUser == null || LoginManagerG2.testDigestAuthentication(httpClient, address, port, lastP, "/rpc/Shelly.GetStatus") != HttpStatus.OK_200) {
						DialogAuthentication credentialsDlg = new DialogAuthentication(
								Main.LABELS.getString("dlgAuthTitle"),
								null /*labelUser*/,
								Main.LABELS.getString("labelPassword"));
						credentialsDlg.setUser(LoginManagerG2.LOGIN_USER);
						credentialsDlg.setMessage(String.format(Main.LABELS.getString("dlgAuthMessage"), name));
						String user;
						do {
							credentialsDlg.setVisible(true);
							if((user = credentialsDlg.getUser()) != null) {
								setCredential(user, credentialsDlg.getPassword().clone()); // ... .clone(): DialogAuthentication clear password after dispose() call
							}
							credentialsDlg.setMessage(String.format(Main.LABELS.getString("dlgAuthMessageError"), name));
						} while(user != null && LoginManagerG2.testDigestAuthentication(httpClient, address, port, lastP, "/rpc/Shelly.GetStatus") != HttpStatus.OK_200);
						credentialsDlg.dispose();
					}
				}
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			d = switch(info.get("app").asText()) {
			case Shelly1G3.ID -> new Shelly1G3(address, port, name);
			case Shelly1PMG3.ID -> new Shelly1PMG3(address, port, name);
			case Shelly2PMG3.ID -> new Shelly2PMG3(address, port, name);
			case Shelly0_10VPMG3.ID -> new Shelly0_10VPMG3(address, port, name);
			case ShellyI4G3.ID -> new ShellyI4G3(address, port, name);
			case ShellyMini1G3.ID -> new ShellyMini1G3(address, port, name);
			case ShellyMini1PMG3.ID -> new ShellyMini1PMG3(address, port, name);
			case ShellyMiniPMG3.ID -> new ShellyMiniPMG3(address, port, name);
			case ShellyPlugSG3.ID -> new ShellyPlugSG3(address, port, name);
			case ShellyHTG3.ID -> new ShellyHTG3(address, port, name);
			case ShellyGatewayG3.ID -> new ShellyGatewayG3(address, port, name);
			// X
			case ShellyXMOD1.ID -> new ShellyXMOD1(address, port, name);
			default -> new ShellyG3Unmanaged(address, port, name);
			};
		} catch(Exception e) { // really unexpected
			LOG.error("create", e);
			d = new ShellyG3Unmanaged(address, port, name, e);
		}
		try {
			d.init(httpClient, wsClient, info);
		} catch(IOException e) {
			if("Status-401".equals(e.getMessage()) == false) {
				LOG.warn("create - init", e);
			}
		} catch(RuntimeException e) {
			LOG.error("create - init {}:{}", address, port, e);
		}
		return d;
	}
	
	public static AbstractBluDevice createBlu(AbstractG2Device parent, HttpClient httpClient, /*WebSocketClient wsClient,*/ JsonNode info, String key) {
		AbstractBluDevice blu;
		try {
			if(key.startsWith(AbstractBluDevice.DEVICE_KEY_PREFIX)) {
				final String type = info.path("config").path("meta").path("ui").path("local_name").asText();
				blu = new BTHomeDevice(parent, info, type, key.substring(13));
			} else { // currently only BluTRV
				blu = new BluTRV(parent, info, key.substring(7));
			}
		} catch(Exception e) { // really unexpected
			LOG.error("createBlu", e);
			String index = key.substring(key.indexOf(':') + 1);
			blu = new ShellyBluUnmanaged(parent, info, index, e);
		}
		try {
			blu.init(httpClient/*, wsClient*/);
		} catch (IOException e) {
			LOG.error("createBlu {} - init", key, e);
		} catch(RuntimeException e) {
			LOG.error("createBlu {} - init {}", key, parent.getAddressAndPort(), e);
		}
		return blu;
	}

	// default credentials
	public static void setCredential(String user, char[] p) {
		lastUser = user;
		lastP = p;
	}
}