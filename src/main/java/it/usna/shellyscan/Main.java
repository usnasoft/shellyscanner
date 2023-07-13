package it.usna.shellyscan;

import java.awt.Color;
import java.awt.Cursor;
import java.io.File;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.UsnaSwingUtils;
import it.usna.util.AppProperties;

public class Main {
	public final static String APP_NAME = "Shelly Scanner";
	public final static String VERSION = "0.9.9 alpha";
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
		
//		UsnaSwingUtils.initializeFontSize(1.2f);
		try {
			UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NMBUS);
		} catch (Exception e) {
			Msg.errorMsg(e);
		}
//		UIManager.put("Table.background", new ColorUIResource(TAB_LINE1));
//		UIManager.put("Table.alternateRowColor", TAB_LINE2);
//		UIManager.getLookAndFeelDefaults().put("Table:\"Table.cellRenderer\".background", new ColorUIResource(TAB_LINE1));

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
				//todo
				model.load();
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
