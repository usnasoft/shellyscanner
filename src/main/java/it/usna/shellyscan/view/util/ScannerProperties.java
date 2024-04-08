package it.usna.shellyscan.view.util;

import java.nio.file.Paths;

import it.usna.util.AppProperties;

/**
 * Not a real singleton
 * @author a.flaccomio
 */
public class ScannerProperties {
	public final static String PROP_TOOLBAR_CAPTIONS = "T_CAPTIONS";
	public final static String PROP_CSV_SEPARATOR = "CSV_SEPARATOR";
	public final static String PROP_CSV_SEPARATOR_DEFAULT = ",";
	public final static String PROP_SCAN_MODE = "SCAN_MODE";
	public final static String PROP_SCAN_MODE_DEFAULT = "FULL";
	public final static String PROP_DCLICK_ACTION = "DCLICK_ACTION";
	public final static String PROP_DCLICK_ACTION_DEFAULT = "DET";
	
	public final static String PROP_UPDATECHK_ACTION = "UPDATE_CHK";
	public final static String PROP_UPDATECHK_ACTION_DEFAULT = "STABLE";
	
	public final static String PROP_CHARTS_START = "CHART_DEF";
	public final static String PROP_CHARTS_EXPORT = "CHART_EXPORT";
	
	public final static String PROP_DETAILED_VIEW_SCREEN = "DETAIL_SCREEN";
	public final static String PROP_DETAILED_VIEW_SCREEN_FULL = "FULL";
	public final static String PROP_DETAILED_VIEW_SCREEN_AS_IS = "ASIS";
	public final static String PROP_DETAILED_VIEW_SCREEN_HORIZONTAL = "HOR";
	public final static String PROP_DETAILED_VIEW_SCREEN_ESTIMATE = "COMP";
	public final static String PROP_DETAILED_VIEW_SCREEN_DEFAULT = PROP_DETAILED_VIEW_SCREEN_FULL;
	
	public final static String PROP_LOGIN_USER = "RLUSER";
	public final static String PROP_LOGIN_PWD = "RLPWD";
	
	public final static String PROP_REFRESH_ITERVAL = "REFRESH_INTERVAL";
	public final static int PROP_REFRESH_ITERVAL_DEFAULT = 2;
	public final static String PROP_REFRESH_CONF = "REFRESH_SETTINGS";
	public final static int PROP_REFRESH_CONF_DEFAULT = 5;
	
	public final static String PROP_USE_ARCHIVE = "USE_ARCHIVE";
	public final static String PROP_ARCHIVE_FILE = "USE_ARCHIVE_FILENAME";
	public final static String PROP_ARCHIVE_FILE_DEFAULT = Paths.get(System.getProperty("user.home"), "ShellyStore.arc").toString();
	public final static String PROP_AUTORELOAD_ARCHIVE = "AUTORELOAD";
	
	public final static String BASE_SCAN_IP = "BASE_SCAN";
	public final static String FIRST_SCAN_IP = "FIRST_SCAN";
	public final static int FIST_SCAN_IP_DEFAULT = 1;
	public final static String LAST_SCAN_IP = "LAST_SCAN";
	public final static int LAST_SCAN_IP_DEFAULT = 254;
	
	public final static String PROP_IDE_TAB_SIZE = "IDE_TAB_SIZE";
	public final static int IDE_TAB_SIZE_DEFAULT = 4;
	
	public final static String PROP_IDE_DARK = "IDE_DARK";
	
	private static AppProperties ap;
	
	private ScannerProperties() {}
	
	public static void init(String file) {
		ap = new AppProperties(file);
	}
	
	public static AppProperties get() {
		return ap;
	}
}
