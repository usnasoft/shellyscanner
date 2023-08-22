package it.usna.shellyscan;

import java.awt.Color;
import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.model.NonInteractiveDevices;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.UsnaSwingUtils;
import it.usna.util.AppProperties;
import it.usna.util.CLI;

public class Main {
	public final static String APP_NAME = "Shelly Scanner";
	public final static String VERSION = "1.0.0 beta";
	public final static String VERSION_CODE = "001.000.000r100"; // r0xx alpha; r1xx beta; r2xx stable
	public final static String REVISION = "0";
	public final static String ICON = "/images/ShSc24.png";
	public final static String BACKUP_FILE_EXT = "sbk";
	public final static String ARCHIVE_FILE_EXT = "arc";
	
	public final static ResourceBundle LABELS = ResourceBundle.getBundle("LabelsBundle");
	public static Color BG_COLOR = new Color(50, 60, 65);
	public static Color TAB_LINE1 = new Color(240, 240, 240);
	public static Color TAB_LINE2 = new Color(160, 180, 255)/*Color.lightGray*/;
	public static Color STATUS_LINE = new Color(200, 220, 255);
	public final static String TAB_VERSION = "4"; // on change reset table settings

	private final static String PROP_FILE = System.getProperty("user.home") + File.separator + ".shellyScanner";
	private final static AppProperties appProp = new AppProperties(PROP_FILE);

	private final static String IP_SCAN_PAR_FORMAT = "^((?:(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?))\\.(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)-(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

	private final static Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(final String ... args) {
		LOG.info(APP_NAME + " " + VERSION + " r." + REVISION);
		//		Package mainPackage = Main.class.getPackage();
		//		String version = mainPackage.getImplementationVersion();
		
//		UsnaSwingUtils.initializeFontSize(1.2f);
		try { // in case of error use default configuration
			appProp.load(true);
		} catch (Exception e) {
			Msg.errorMsg(e);
		}
		
		CLI cli = new CLI(args);
		
		boolean fullScan = false;
		byte [] ip = null;
		int firstIP = 0;
		int lastIP  = 0;
		
		int cliIndex;
		if(cli.hasEntry("-fullscan", "-fs") >= 0) {
			fullScan = true;
		} else if(cli.hasEntry("-localscan", "-ls") >= 0) {
			fullScan = false;
		} else if((cliIndex = cli.hasEntry("-ipscan", "-ips")) >= 0) {
			try {
				Matcher m = Pattern.compile(IP_SCAN_PAR_FORMAT).matcher(cli.getParameter(cliIndex));
				m.find();
				String baseIP = m.group(1);
				firstIP = Integer.parseInt(m.group(2));
				lastIP = Integer.parseInt(m.group(3));
				String ipS[] = baseIP.split("\\.");
				ip = new byte[] {(byte)Integer.parseInt(ipS[0]), (byte)Integer.parseInt(ipS[1]), (byte)Integer.parseInt(ipS[2]), 0};
			} catch (Exception e) {
				System.err.println("Wrong parameter format; example: -ipscan 192.168.1.1-254");
				System.exit(1);
			}
		} else {
			final String scanMode = appProp.getProperty(DialogAppSettings.PROP_SCAN_MODE, DialogAppSettings.PROP_SCAN_MODE_DEFAULT);
			if(scanMode.equals("IP")) {
				String baseIP = appProp.getProperty(DialogAppSettings.BASE_SCAN_IP);
				firstIP = appProp.getIntProperty(DialogAppSettings.FIRST_SCAN_IP);
				lastIP = appProp.getIntProperty(DialogAppSettings.LAST_SCAN_IP);
				String ipS[] = baseIP.split("\\.");
				ip = new byte[] {(byte)Integer.parseInt(ipS[0]), (byte)Integer.parseInt(ipS[1]), (byte)Integer.parseInt(ipS[2]), 0};
			} else {
				fullScan = scanMode.equals("FULL");
			}
		}
		
		if((cliIndex = cli.hasEntry("-backup")) >= 0) {
			String path = cli.getParameter(cliIndex);
			if(path == null) {
				System.err.println("mandatory parameter after -backup (must be an existing path)");
				System.exit(1);
			}
			Path dirPath = Paths.get(path);
			if(path == null || Files.exists(dirPath) == false || Files.isDirectory(dirPath) == false) {
				System.err.println("parameter after -backup must be an existing path");
				System.exit(1);
			}
			try (NonInteractiveDevices model = new NonInteractiveDevices()) {
				if(ip == null) {
					model.scannerInit(fullScan);
				} else {
					model.scannerInit(ip, firstIP, lastIP);
				}
				//todo
				model.execute(d -> System.out.println(d));
//				System.out.println(model.size());
				System.out.println("non interactive backup in: " + path);
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else if(cli.hasEntry("-list") >= 0) {
			try (NonInteractiveDevices model = new NonInteractiveDevices()) {
				if(ip == null) {
					model.scannerInit(fullScan);
				} else {
					model.scannerInit(ip, firstIP, lastIP);
				}
				model.execute(d -> System.out.println(d));
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		// find unused CLI entries
		if(cli.unused().length > 0) {
			System.err.println("Wrong parameter(s): " + Arrays.stream(cli.unused()).collect(Collectors.joining("; ")));
			System.exit(10);
		}

		try {
			UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NMBUS);
		} catch (Exception e) {
			Msg.errorMsg(e);
		}
//		UIManager.put("Table.background", new ColorUIResource(TAB_LINE1));
//		UIManager.put("Table.alternateRowColor", TAB_LINE2);
//		UIManager.getLookAndFeelDefaults().put("Table:\"Table.cellRenderer\".background", new ColorUIResource(TAB_LINE1));
		
		if(TAB_VERSION.equals(appProp.getProperty("TAB_VER")) == false) {
			appProp.setProperty("TAB_VER", TAB_VERSION);
			appProp.remove("TAB.COL_P");
		}
		try {
			final Devices model = new Devices();
			final MainView view = new MainView(model, appProp);
			// final values for thread
			final boolean fullScanx = fullScan;
			final byte [] ipFin = ip;
			final int firstIPFin = firstIP;
			final int lastIPFin = lastIP;
			
			SwingUtilities.invokeLater(() -> {
				view.setVisible(true);
				try {
					view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					view.requestFocus(); // remove random focus on toolbar button
					if(appProp.getBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, true)) {
						try {
							model.loadFromStore(Paths.get(appProp.getProperty(DialogAppSettings.PROP_ARCHIVE_FILE, DialogAppSettings.PROP_ARCHIVE_FILE_DEFAULT)));
						} catch (IOException e) {
							appProp.setBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, false);
							Msg.errorMsg(e);
						}
					}
					
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
					if(ipFin != null) {
						model.scannerInit(ipFin, firstIPFin, lastIPFin, refreshStatusInterval, refreshConfigTics);
					} else {
						model.scannerInit(fullScanx, refreshStatusInterval, refreshConfigTics);
					}
				} catch (/*IO*/Exception e) {
					Msg.errorMsg(e);
					System.exit(1);
				} finally {
					view.setCursor(Cursor.getDefaultCursor());
				}
			});
		} catch (Throwable ex) {
			Msg.errorMsg(ex);
			ex.printStackTrace();
			System.exit(1);
		}
	}
}