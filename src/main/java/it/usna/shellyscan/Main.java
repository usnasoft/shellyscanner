package it.usna.shellyscan;

import java.awt.Color;
import java.awt.Cursor;
import java.io.File;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.util.Msg;
import it.usna.util.AppProperties;

public class Main {
	public final static String APP_NAME = "Shelly Scanner";
	public final static String VERSION = "0.9.x alpha";
	public final static String ICON = "/images/ShSc24.png";
	public final static String BACKUP_FILE_EXT = "sbk";
	
	public final static ResourceBundle LABELS = ResourceBundle.getBundle("LabelsBundle");
	public static Color BG_COLOR = new Color(50, 60, 65);
	public static Color TAB_LINE1 = new Color(240, 240, 240);
	public static Color TAB_LINE2 = new Color(160, 180, 255)/*Color.lightGray*/;
	public static Color STATUS_LINE = new Color(200, 220, 255);
	public final static String TAB_VERSION = "3"; // on change reset table settings

	private final static String PROP_FILE = System.getProperty("user.home") + File.separator + ".shellyScanner";
	private final static AppProperties appProp = new AppProperties(PROP_FILE);

	private final static String IP_SCAN_PAR_FORMAT = "^((?:(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?))\\.(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)-(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

	private final static Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(final String ... args) {
		LOG.info(APP_NAME + " " + VERSION);

		//		Package mainPackage = Main.class.getPackage();
		//		String version = mainPackage.getImplementationVersion();
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					
//					UIManager.put("Table.background", new ColorUIResource(TAB_LINE1));
//					UIManager.put("Table.alternateRowColor", TAB_LINE2);
//					UIManager.getLookAndFeelDefaults().put("Table:\"Table.cellRenderer\".background", new ColorUIResource(TAB_LINE1));
					break;
				}
			}
		} catch (Exception e) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ex) {
				Msg.errorMsg(ex);
			}
		}

		try {
			appProp.load(true);
			if(TAB_VERSION.equals(appProp.getProperty("TAB_VER")) == false) {
				appProp.setProperty("TAB_VER", TAB_VERSION);
				appProp.remove("TAB.COL_P");
			}
			final Devices model = new Devices();
			final MainView view = new MainView(model, appProp);
			SwingUtilities.invokeLater(() -> {
				view.setVisible(true);
				view.requestFocus(); // remove random focus on toolbar button
				try {
					view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					final String scanMode = appProp.getProperty(DialogAppSettings.PROP_SCAN_MODE, DialogAppSettings.PROP_SCAN_MODE_DEFAULT);
					boolean fullScan = false;
					String baseIP = null;
					int firstIP = 0;
					int lastIP  = 0;
					if(scanMode.equals("IP")) {
						baseIP = appProp.getProperty(DialogAppSettings.BASE_SCAN_IP);
						firstIP = appProp.getIntProperty(DialogAppSettings.FIRST_SCAN_IP);
						lastIP = appProp.getIntProperty(DialogAppSettings.LAST_SCAN_IP);
					} else {
						fullScan = scanMode.equals("FULL");
					}

//					try {
						for(int i = 0; i < args.length; i++) {
							if(args[i].equals("-fullscan")) {
								fullScan = true;
								baseIP = null;
							} else if(args[i].equals("-localscan")) {
								fullScan = false;
								baseIP = null;
							} else if(args[i].equals("-ipscan")) {
								try {
									Matcher m = Pattern.compile(IP_SCAN_PAR_FORMAT).matcher(args[++i]);
									m.find();
									baseIP = m.group(1);
									firstIP = Integer.parseInt(m.group(2));
									lastIP = Integer.parseInt(m.group(3));
								} catch (Exception e) {
									baseIP = null;
									LOG.info("Wrong format; example: -ipscan 192.168.1.1-254");
								}
//							} else if(args[i].equals("-p")) {
								
							} else {
								LOG.info("Unknown parameter: {}", args[i]);
							}
						} 
//					} catch (Exception e) {
//						LOG.warn("Wrong parmeter at position {}", i + 1);
//					}
					
					String lUser = appProp.getProperty(DialogAppSettings.PROP_LOGIN_USER);
					if(lUser != null && lUser.length() > 0) {
						try {
							char[] pDecoded = new String(Base64.getDecoder().decode(appProp.getProperty(DialogAppSettings.PROP_LOGIN_PWD).substring(1))).toCharArray();
//							CredentialsProvider cp = LoginManager.getCredentialsProvider(lUser, pDecoded);
							DevicesFactory.setCredential(lUser, pDecoded);
						} catch(RuntimeException e) {}
					}
					final int refreshStatusInterval = appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_ITERVAL, DialogAppSettings.PROP_REFRESH_ITERVAL_DEFAULT) * 1000;
					final int refreshConfigTics = appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_CONF, DialogAppSettings.PROP_REFRESH_CONF_DEFAULT);
					if(baseIP != null) {
						String ipS[] = baseIP.split("\\.");
						byte [] ip = new byte[] {(byte)Integer.parseInt(ipS[0]), (byte)Integer.parseInt(ipS[1]), (byte)Integer.parseInt(ipS[2]), 0};
						model.scannerInit(ip, firstIP, lastIP, refreshStatusInterval, refreshConfigTics);
					} else {
						model.scannerInit(fullScan, refreshStatusInterval, refreshConfigTics);
					}
					view.setCursor(Cursor.getDefaultCursor());
				} catch (/*IO*/Exception e) {
					Msg.errorMsg(e);
					System.exit(1);
				}
			});
		} catch (Throwable ex) {
			Msg.errorMsg(ex);
			System.exit(1);
		}
	}
}

// https://www.softicons.com/web-icons/web-grey-buttons-by-axialis-team
// https://www.softicons.com/web-icons/circular-icons-by-pro-theme-design/
// https://www.veryicon.com/

//-D"org.slf4j.simpleLogger.log.it.usna=debug" -D"org.slf4j.simpleLogger.showDateTime=true"

//color weel https://stackoverflow.com/questions/36252778/how-to-draw-a-rgb-color-wheel-in-java

//0.5.5
// aggiunto Plug
// riscontro "restricted login" e "wi-fi bu" come per backup
// errore cloud per alcuni dispositivi (motion)

//0.6.0
//stato dispositivi (on-line, off-line, login, reading, error)
//colonna "nome"
//doppio click "Full device info" (non su command)
//login al singolo dispositivi protetto
//edit fine rgbw
//duo
//i3
//print table

//0.6.1
//flood

//0.7.0
//refresh parallelo continuo
//shelly 2, UNI
//export CSV

//0.7.2
//nome colonna stato (csv)
//restore mqtt (no pwd)
//doppia scheda di rete (Luk McFagnan)
//settings
// -localscan
//supporto parziale plus
//flag "defaul" su mqtt prefix

//0.7.3
//separazione fill status - file settings (che potrebbe essere chiamato meno)
//ShellyBulb
//supporto parziale

//0.7.4
//script G2
//filtro per nome
//selezione colonne
//posizione colonne
//3EM

//0.8.0
// miglioramenti fw update
// copy hostname
// copy mac address
// copy cell
// col SSID
//colonna misure

//0.8.1
// addon temperatura 1, 1PM
// colonna MAC
// sliders migliorati
// --- alpha2
// errore MQTT settings G1 multipli
// errore poco frequente restore null -> "null"
// restore restricted login
// restore wi-fi2
// link a manuale e download da [?]

//0.8.2
//add column - MQTT enabled
//add column - source
//default tabella non tutte le colonne
//optionally allow users to write credentials on application settings
//full scan default
//DialogDeviceInfo refresh
//+2PM
//restore + improved
//backup progress
//detailed info improvement for battery operated devices (some stored info shows when offline; experimental only on button 1)

//0.8.3
//Motion
//lettura conf disp batteria
//conservare i parametri della tabella ed eventualmente ripristinarli su cancel (dialog app settings)
//esclusione dei dispositivi non pertinenti/offline sui settaggi globali
//backup con stored data
//global settings "enable" disabilitato durante "show()

//0.8.4 beta
// H&T
// i4 (parziale)
// miglioramento restore (delay)
// [+] detailed view
//--- beta2
// sistemata altezza righe al ritorno dalla vista dettagliata

//0.8.5
// i4
// IP scan
// sort IP
// Ottimizzazione // final ObjectMapper mapper = new ObjectMapper(); condiviso
// bug: mqtt no pwd

//0.8.6
// stato input
// fw update - selection buttons / counters
// mqtt -copy 
// wifi2 - copy
// ^S combo change selection

//0.8.7
// fw update su tre colonne
// wi-fi 1
// restore script da backup
// mqtt status: connected
// chk table (blt, eco mode, led, AP, logs, ...)
// mqtt specific G2 settings

//0.8.8
// TRV
// ultima connessione (tooltip off-line)
// edit rgbw2 white

//0.9.0
// charts
// pro2 - pro2pm
// V on 2.5

//0.9.1
// org.apache.httpcomponents.client5 -> org.eclipse.jetty (https://www.eclipse.org/jetty/documentation/jetty-11/programming-guide/index.html)
// Riconosciuti i tipi per i dispositivi protetti
// pro4PM (parziale)
// plus plug IT

//0.9.3
// org.java-websocket -> org.eclipse.jetty.websocket)
// Riconosciuti i tipi per i dispositivi protetti
// Recover generic - error
// pro4PM - pro1 - pro1pm - pro3 (full)
// pro2 - pro2pm (full)
// plus plug IT/US
// device info
// Enhanced FW update dialog

//0.9.x
//plus H&T
//rebootRequired

// TODO
// extender WiFi.ListAPClients
// https://docs.oracle.com/javase/8/docs/api/javax/security/auth/callback/CallbackHandler.html
// https://www.baeldung.com/java-authentication-authorization-service
// archivio (additivo) dispositivi collegati opzionalmente caricabile (con eventuale default da settings)
// 2.5 ottimizzazione array ralay 2.5
// global settings: wi-fi 2 (chiedere se il dispositivo e' connesso a questo)
// global settings: wi-fi 1 (chiedere se il dispositivo e' connesso a questo)
// bottone resume refresh (log)
// gestione "reading": se stato online leggo uptime, se uptime diverso dal precedente memorizzo il timestamp else Quando il time stamp corrente - memorizzato > intervallo * 3 modifico lo stato - si, ma dove?
// suspended process (dispositivi a batteria)
// backup non interattivo (scan ip - parametri su cli)