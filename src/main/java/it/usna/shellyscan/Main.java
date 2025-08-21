package it.usna.shellyscan;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.simple.SimpleLogger;

import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.model.IPCollection;
import it.usna.shellyscan.model.NonInteractiveDevices;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.chart.ChartType;
import it.usna.shellyscan.view.chart.MeasuresChart;
import it.usna.shellyscan.view.chart.NonInteractiveMeasuresChart;
import it.usna.shellyscan.view.util.ApplicationUpdateCHK;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.swing.UsnaSwingUtils;
import it.usna.util.CLI;

public class Main {
	static {
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS"); // macOS specific - cmd-Q / -Dapple.eawt.quitStrategy=CLOSE_ALL_WINDOWS
	}
	public static final String APP_NAME = "Shelly Scanner";
	public static final String VERSION = "1.2.7 alpha";
	public static final String VERSION_CODE = "001.002.007r000"; // r0xx alpha; r1xx beta; r2xx stable
	public static final Image ICON = Toolkit.getDefaultToolkit().createImage(Main.class.getResource("/images/ShSc24.png"));
	public static final String BACKUP_FILE_EXT = "sbk";
	public static final String ARCHIVE_FILE_EXT = "arc";

	public static final ResourceBundle LABELS = ResourceBundle.getBundle("LabelsBundle");
//	public static final Color BG_COLOR = new Color(50, 60, 65);
	public static final Color BG_COLOR = new Color(60, 70, 90);
	public static final Color TAB_LINE1_COLOR = new Color(240, 240, 240);
//	public static final Color TAB_LINE2 = new Color(160, 180, 255);
	public static final Color TAB_LINE2_COLOR = new Color(210, 218, 255);
//	public static final Color STATUS_LINE = new Color(200, 220, 255);
	public static final Color STATUS_LINE_COLOR = new Color(172, 195, 230);
	public static final String TAB_VERSION = "5"; // on version change reset table settings

	private static final String IP_SCAN_PAR_FORMAT = "^((?:(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){2}(?:0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?))\\.(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)-(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

	public static void main(final String ... args) {
		// System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "error");
		System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "javax.jmdns", "warn");
		System.setProperty(SimpleLogger.LOG_KEY_PREFIX + "org.eclipse.jetty", "warn");
		System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
		final Logger LOG = LoggerFactory.getLogger(Main.class);
		LOG.info(APP_NAME + " " + VERSION_CODE);
//		LOG.debug(Runtime.version().toString() + " / " + System.getProperties().getProperty("java.vendor"));

		final ScannerProperties appProp = ScannerProperties.init(Path.of(System.getProperty("user.home"), ".shellyScanner"));

		CLI cli = new CLI(args);

		boolean fullScan = false;
		IPCollection ipCollection = null;

		int cliIndex;
		// Scan mode
		if(cli.hasEntry("-fullscan", "-full") >= 0) {
			fullScan = true;
		} else if(cli.hasEntry("-localscan", "-local") >= 0) {
			fullScan = false;
		} else if((cliIndex = cli.hasEntry("-ipscan", "-ipscan0", "-ip", "-ip0")) >= 0) {
			try {
				final Pattern ipRangePattern = Pattern.compile(IP_SCAN_PAR_FORMAT);
				Matcher m = ipRangePattern.matcher(cli.getParameter(cliIndex));
				m.find();
				String baseIPPar = m.group(1);
				int firstIP = Integer.parseInt(m.group(2));
				int lastIP = Integer.parseInt(m.group(3));
				ipCollection = new IPCollection();
				ipCollection.add(baseIPPar, firstIP, lastIP);
				for(int i = 1; (cliIndex = cli.hasEntry("-ipscan" + i, "-ip" + i)) >= 0; i++) {
					m = ipRangePattern.matcher(cli.getParameter(cliIndex));
					m.find();
					baseIPPar = m.group(1);
					firstIP = Integer.parseInt(m.group(2));
					lastIP = Integer.parseInt(m.group(3));
					ipCollection.add(baseIPPar, firstIP, lastIP);
				}
			} catch (Exception e) {
				System.err.println("Wrong parameter format; example: -ipscan 192.168.1.1-254");
				System.exit(1);
			}
		} else if((cliIndex = cli.hasEntry("-noscan")) >= 0) { // only archive (it's actually an IP scan with firstIP > lastIP)
			ipCollection = new IPCollection();
		} else {
			final String scanMode = appProp.getProperty(ScannerProperties.PROP_SCAN_MODE/*, ScannerProperties.PROP_SCAN_MODE_DEFAULT*/);
			if(scanMode.equals("IP")) {
				ipCollection = new IPCollection();
				for(int i = 0; i < 10; i++) {
					final String baseIPPar = appProp.getProperty(ScannerProperties.BASE_SCAN_IP + i);
					if(baseIPPar != null && baseIPPar.isEmpty() == false) {
						int firstIP = appProp.getIntProperty(ScannerProperties.FIRST_SCAN_IP + i);
						int lastIP = appProp.getIntProperty(ScannerProperties.LAST_SCAN_IP + i);
						ipCollection.add(baseIPPar, firstIP, lastIP);
					}
				}
			} else if(scanMode.equals("OFFLINE")) {
				ipCollection = new IPCollection();
			} else {
				fullScan = scanMode.equals("FULL");
			}
		}

		// Credentials (from configuration only)
		String lUser = appProp.getProperty(ScannerProperties.PROP_LOGIN_USER);
		if(lUser != null && lUser.length() > 0) {
			try {
				char[] pDecoded = new String(Base64.getDecoder().decode(appProp.getProperty(ScannerProperties.PROP_LOGIN_PWD).substring(1))).toCharArray();
				DevicesFactory.setCredential(lUser, pDecoded);
			} catch(RuntimeException e) {}
		}

		// Non interactive commands
		if((cliIndex = cli.hasEntry("-backup")) >= 0) {
			final String path = cli.getParameter(cliIndex);
			if(path == null) {
				System.err.println("mandatory parameter after -backup (must be an existing path)");
				System.exit(1);
			}
			Path dirPath = Path.of(path);
			if(path == null || Files.exists(dirPath) == false || Files.isDirectory(dirPath) == false) {
				System.err.println("parameter after -backup must be an existing path");
				System.exit(1);
			}
			// look for unused CLI entries
			if(cli.unused().length > 0) {
				System.err.println("Wrong parameter(s): " + String.join("; ", cli.unused()));
				System.exit(10);
			}
			LOG.info("Backup devices in {}", path);
			try (NonInteractiveDevices model = (ipCollection == null) ? new NonInteractiveDevices(fullScan) : new NonInteractiveDevices(ipCollection)) {
				model.execute(d -> {
					try {
						d.backup(Path.of(path, d.getHostname().replaceAll("[^\\w_-]+", "_") + "." + Main.BACKUP_FILE_EXT));
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
				System.err.println("Wrong parameter(s): " + String.join("; ", cli.unused()));
				System.exit(10);
			}
			LOG.info("Retriving list ...");
			try (NonInteractiveDevices model = (ipCollection == null) ? new NonInteractiveDevices(fullScan) : new NonInteractiveDevices(ipCollection)) {
				model.execute(d -> System.out.println(d));
				LOG.info("List end");
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// Activate dynamic model - Go interactive
		try {
			UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NIMBUS);
//			UIManager.getLookAndFeelDefaults().put("Table:\"Table.cellRenderer\".alternateRowColor", TAB_LINE2_COLOR); // genera strani log
		} catch (Exception e) {
			Msg.errorMsg(e);
		}
		//		UIManager.put("Table.background", new ColorUIResource(TAB_LINE1));
		//		UIManager.put("Table.alternateRowColor", TAB_LINE2);

		if(TAB_VERSION.equals(appProp.getProperty("TAB_VER")) == false) {
			appProp.setProperty("TAB_VER", TAB_VERSION);
			appProp.remove(DevicesTable.STORE_PREFIX + ".COL_P");
			appProp.remove(DevicesTable.STORE_EXT_PREFIX + ".COL_P");
		}
		try {
			final Devices model = new Devices();
			DeferrablesContainer.init(model); // first model listener
			final MainView view = new MainView(model, appProp);

			cliIndex = cli.hasEntry("-graphs");
			if(cliIndex >= 0) {
				String gPar = cli.getParameter(cliIndex);
				if(gPar != null) {
					try {
						NonInteractiveMeasuresChart chartW = new NonInteractiveMeasuresChart(model, ChartType.valueOf(gPar), appProp);
						model.addListener(chartW);
						// do not activateGUI
					} catch(IllegalArgumentException e) { // not a valid chart type
						activateGUI(view, model, appProp);
						MeasuresChart.setDoOutStream(true);
						cli.rejectParameter(cliIndex);
					}
				} else {
					activateGUI(view, model, appProp);
					MeasuresChart.setDoOutStream(true);
				}
			} else {
				activateGUI(view, model, appProp);
			}

			// final values for thread
			final boolean fullScanFinal = fullScan;
			final IPCollection ipCollectionFinal = ipCollection;
			SwingUtilities.invokeLater(() -> {
				try {
					view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					boolean useArchive = appProp.getBoolProperty(ScannerProperties.PROP_USE_ARCHIVE);
					if(useArchive) {
						try {
							model.loadFromStore(Path.of(appProp.getProperty(ScannerProperties.PROP_ARCHIVE_FILE, ScannerProperties.PROP_ARCHIVE_FILE_DEFAULT)));
						} catch (/*IO*/Exception e) {
							appProp.setBoolProperty(ScannerProperties.PROP_USE_ARCHIVE, false);
							Msg.errorMsg(view, e);
						}
					}
					final int refreshStatusInterval = appProp.getIntProperty(ScannerProperties.PROP_REFRESH_ITERVAL/*, ScannerProperties.PROP_REFRESH_ITERVAL_DEFAULT*/) * 1000;
					final int refreshConfigTics = appProp.getIntProperty(ScannerProperties.PROP_REFRESH_CONF/*, ScannerProperties.PROP_REFRESH_CONF_DEFAULT*/);
					if(ipCollectionFinal != null) {
						model.scannerInit(ipCollectionFinal, refreshStatusInterval, refreshConfigTics);
					} else {
						model.scannerInit(fullScanFinal, refreshStatusInterval, refreshConfigTics, appProp.getBoolProperty(ScannerProperties.PROP_AUTORELOAD_ARCHIVE) && useArchive);
					}
				} catch (/*IO*/Exception e) {
					Msg.errorMsg(e);
					System.exit(1);
				} finally {
					view.setCursor(Cursor.getDefaultCursor());
				}
			});
			if(cli.unused().length > 0) {
				System.err.println("Ignored parameter(s): " + String.join("; ", cli.unused()));
			}
		} catch (Exception ex) {
			Msg.errorMsg(ex);
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void activateGUI(final MainView view, final Devices model, final ScannerProperties appProp) {
		view.setVisible(true);
		view.requestFocus(); // remove random focus on toolbar button
		model.addListener(view);
		appProp.addListener(view);
		new Thread(() -> ApplicationUpdateCHK.checkForUpdates(view, appProp)).start();
	}
}