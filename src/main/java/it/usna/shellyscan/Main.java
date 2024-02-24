package it.usna.shellyscan;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
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
import org.slf4j.simple.SimpleLogger;

import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.model.NonInteractiveDevices;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.chart.MeasuresChart;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UpplicationUpdateCHK;
import it.usna.swing.UsnaSwingUtils;
import it.usna.util.AppProperties;
import it.usna.util.CLI;

public class Main {
	public final static String APP_NAME = "Shelly Scanner";
	public final static String VERSION = "1.0.4 alpha";
	public final static String VERSION_CODE = "001.000.004r000"; // r0xx alpha; r1xx beta; r2xx stable
	public final static Image ICON = Toolkit.getDefaultToolkit().createImage(Main.class.getResource("/images/ShSc24.png"));
	public final static String BACKUP_FILE_EXT = "sbk";
	public final static String ARCHIVE_FILE_EXT = "arc";
	
	public final static ResourceBundle LABELS = ResourceBundle.getBundle("LabelsBundle");
	public final static Color BG_COLOR = new Color(50, 60, 65);
	public final static Color TAB_LINE1 = new Color(240, 240, 240);
	public final static Color TAB_LINE2 = new Color(160, 180, 255)/*Color.lightGray*/;
	public final static Color STATUS_LINE = new Color(200, 220, 255);
	public final static String TAB_VERSION = "4"; // on version change reset table settings

	private final static String PROP_FILE = System.getProperty("user.home") + File.separator + ".shellyScanner";
	private final static AppProperties appProp = new AppProperties(PROP_FILE);

	private final static String IP_SCAN_PAR_FORMAT = "^((?:(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?))\\.(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)-(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

	public static void main(final String ... args) {
//		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "error");
		System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "javax.jmdns", "warn");
		System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
		final Logger LOG = LoggerFactory.getLogger(Main.class);

		LOG.info(APP_NAME + " " + VERSION_CODE);
//		System.setProperty("java.net.preferIPv4Stack" , "true");
//		UsnaSwingUtils.initializeFontSize(1.2f);
		try { // in case of error or no file (true) use default configuration
			appProp.load(true);
		} catch (Exception e) {
			Msg.errorMsg(e);
		}
		
		CLI cli = new CLI(args);
		
		boolean fullScan = false;
		byte [] baseIP = null;
		int firstIP = 0;
		int lastIP  = 0;
		
		int cliIndex;
		// Scan mode
		if(cli.hasEntry("-fullscan", "-full") >= 0) {
			fullScan = true;
		} else if(cli.hasEntry("-localscan", "-local") >= 0) {
			fullScan = false;
		} else if((cliIndex = cli.hasEntry("-ipscan", "-ip")) >= 0) {
			try {
				Matcher m = Pattern.compile(IP_SCAN_PAR_FORMAT).matcher(cli.getParameter(cliIndex));
				m.find();
				final String baseIPPar = m.group(1);
				String ipS[] = baseIPPar.split("\\.");
				firstIP = Integer.parseInt(m.group(2));
				lastIP = Integer.parseInt(m.group(3));
				baseIP = new byte[] {(byte)Integer.parseInt(ipS[0]), (byte)Integer.parseInt(ipS[1]), (byte)Integer.parseInt(ipS[2]), 0};
			} catch (Exception e) {
				System.err.println("Wrong parameter format; example: -ipscan 192.168.1.1-254");
				System.exit(1);
			}
		} else if((cliIndex = cli.hasEntry("-noscan")) >= 0) { // only archive (it's actually an IP scan with firstIP > lastIP)
			baseIP = new byte[] {127, 0, 0, 1};
			firstIP = 1;
			lastIP = 0;
		} else {
			final String scanMode = appProp.getProperty(DialogAppSettings.PROP_SCAN_MODE, DialogAppSettings.PROP_SCAN_MODE_DEFAULT);
			if(scanMode.equals("IP")) {
				final String baseIPPar = appProp.getProperty(DialogAppSettings.BASE_SCAN_IP);
				String ipS[] = baseIPPar.split("\\.");
				firstIP = appProp.getIntProperty(DialogAppSettings.FIRST_SCAN_IP);
				lastIP = appProp.getIntProperty(DialogAppSettings.LAST_SCAN_IP);
				baseIP = new byte[] {(byte)Integer.parseInt(ipS[0]), (byte)Integer.parseInt(ipS[1]), (byte)Integer.parseInt(ipS[2]), 0};
			} else if(scanMode.equals("OFFLINE")) {
				baseIP = new byte[] {127, 0, 0, 1};
				firstIP = 1;
				lastIP = 0;
			} else {
				fullScan = scanMode.equals("FULL");
			}
		}
		
		// Credentials (from configuration only)
		String lUser = appProp.getProperty(DialogAppSettings.PROP_LOGIN_USER);
		if(lUser != null && lUser.length() > 0) {
			try {
				char[] pDecoded = new String(Base64.getDecoder().decode(appProp.getProperty(DialogAppSettings.PROP_LOGIN_PWD).substring(1))).toCharArray();
				DevicesFactory.setCredential(lUser, pDecoded);
			} catch(RuntimeException e) {}
		}
		
		// Non interactive commands
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
			// look for unused CLI entries
			if(cli.unused().length > 0) {
				System.err.println("Wrong parameter(s): " + Arrays.stream(cli.unused()).collect(Collectors.joining("; ")));
				System.exit(10);
			}
			LOG.info("Backup devices in {}", path);
			try (NonInteractiveDevices model = (baseIP == null) ? new NonInteractiveDevices(fullScan) : new NonInteractiveDevices(baseIP, firstIP, lastIP)) {
				model.execute(d -> {
					try {
						d.backup(new File(dirPath.toFile(), d.getHostname().replaceAll("[^\\w_-]+", "_") + "." + Main.BACKUP_FILE_EXT));
						System.out.println(d.getHostname() + " success");
					} catch (Exception e) {
						System.out.println(d.getHostname() + " error - " + e.toString());
					}
				});
				LOG.info("Backup end");
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else if(cli.hasEntry("-list") >= 0) {
			// look for unused CLI entries
			if(cli.unused().length > 0) {
				System.err.println("Wrong parameter(s): " + Arrays.stream(cli.unused()).collect(Collectors.joining("; ")));
				System.exit(10);
			}
			LOG.info("Retriving list ...");
			try (NonInteractiveDevices model = (baseIP == null) ? new NonInteractiveDevices(fullScan) : new NonInteractiveDevices(baseIP, firstIP, lastIP)) {
				model.execute(d -> System.out.println(d));
				LOG.info("List end");
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// Go interactive
		try {
			UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NIMBUS);
		} catch (Exception e) {
			Msg.errorMsg(e);
		}
//		UIManager.put("Table.background", new ColorUIResource(TAB_LINE1));
//		UIManager.put("Table.alternateRowColor", TAB_LINE2);
//		UIManager.getLookAndFeelDefaults().put("Table:\"Table.cellRenderer\".background", new ColorUIResource(TAB_LINE1));
		
		if(TAB_VERSION.equals(appProp.getProperty("TAB_VER")) == false) {
			appProp.setProperty("TAB_VER", TAB_VERSION);
			appProp.remove("TAB.COL_P");
			appProp.remove("TAB_EXT.COL_P");
		}
		try {
			final Devices model = new Devices();
			DeferrablesContainer.init(model); // first model listener
			final MainView view = new MainView(model, appProp);

			// final values for thread
			final boolean fullScanx = fullScan;
			final byte [] ipFin = baseIP;
			final int firstIPFin = firstIP;
			final int lastIPFin = lastIP;
			
			SwingUtilities.invokeLater(() -> {
				view.setVisible(true);
				try {
					view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					view.requestFocus(); // remove random focus on toolbar button
					boolean useArchive = appProp.getBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, true);
					if(useArchive) {
						try {
							model.loadFromStore(Paths.get(appProp.getProperty(DialogAppSettings.PROP_ARCHIVE_FILE, DialogAppSettings.PROP_ARCHIVE_FILE_DEFAULT)));
						} catch (/*IO*/Exception e) {
							appProp.setBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, false);
							Msg.errorMsg(view, e);
						}
					}
					final int refreshStatusInterval = appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_ITERVAL, DialogAppSettings.PROP_REFRESH_ITERVAL_DEFAULT) * 1000;
					final int refreshConfigTics = appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_CONF, DialogAppSettings.PROP_REFRESH_CONF_DEFAULT);
					if(ipFin != null) {
						model.scannerInit(ipFin, firstIPFin, lastIPFin, refreshStatusInterval, refreshConfigTics);
					} else {
						model.scannerInit(fullScanx, refreshStatusInterval, refreshConfigTics, appProp.getBoolProperty(DialogAppSettings.PROP_AUTORELOAD_ARCHIVE, false) && useArchive);
					}
				} catch (/*IO*/Exception e) {
					Msg.errorMsg(e);
					System.exit(1);
				} finally {
					view.setCursor(Cursor.getDefaultCursor());
				}
			});
			new Thread(() -> UpplicationUpdateCHK.chechForUpdates(view, appProp)).start();

			MeasuresChart.setDoOutStream(cli.hasEntry("-graphs") >= 0);
			if(cli.unused().length > 0) {
				System.err.println("Ignored parameter(s): " + Arrays.stream(cli.unused()).collect(Collectors.joining("; ")));
			}
		} catch (Throwable ex) {
			Msg.errorMsg(ex);
			ex.printStackTrace();
			System.exit(1);
		}
	}
}