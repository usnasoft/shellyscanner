package it.usna.shellyscan.view.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import it.usna.util.AppProperties;

/**
 * Not a perfect singleton
 * @author a.flaccomio
 */
public class ScannerProperties extends AppProperties { // cannot also extend UsnaObservable
	private static final long serialVersionUID = 1L;
	public static final String PROP_TOOLBAR_CAPTIONS = "T_CAPTIONS";
	public static final String PROP_CSV_SEPARATOR = "CSV_SEPARATOR";
	private static final String PROP_CSV_SEPARATOR_DEFAULT = ",";
	public static final String PROP_SCAN_MODE = "SCAN_MODE";
	private static final String PROP_SCAN_MODE_DEFAULT = "FULL";
	public static final String PROP_DCLICK_ACTION = "DCLICK_ACTION";
	private static final String PROP_DCLICK_ACTION_DEFAULT = "DET";
	public static final String PROP_DEFAULT_FILTER_IDX = "DEFAULT_FILTER";
	
	public static final String PROP_UPTIME_MODE = "UPTIME_MODE";
	private static final String PROP_UPTIME_MODE_DEFAULT = "SEC";
	
	public static final String PROP_TEMP_UNIT = "TEMP_UNIT";
	private static final String PROP_TEMP_UNIT_DEFAULT = "C";
	
	public static final String PROP_UPDATECHK_ACTION = "UPDATE_CHK";
	public static final String PROP_UPDATECHK_NEVER = "NEVER";
	public static final String PROP_UPDATECHK_STABLE = "STABLE";
	public static final String PROP_UPDATECHK_DEV = "BETA";
	
	public static final String PROP_CHARTS_START = "CHART_DEF";
	public static final String PROP_CHARTS_EXPORT = "CHART_EXPORT";
	
	public static final String PROP_DETAILED_VIEW_SCREEN = "DETAIL_SCREEN";
	public static final String PROP_DETAILED_VIEW_SCREEN_FULL = "FULL";
	public static final String PROP_DETAILED_VIEW_SCREEN_AS_IS = "ASIS";
	public static final String PROP_DETAILED_VIEW_SCREEN_HORIZONTAL = "HOR";
	public static final String PROP_DETAILED_VIEW_SCREEN_ESTIMATE = "COMP";
	
	public static final String PROP_LOGIN_USER = "RLUSER";
	public static final String PROP_LOGIN_PWD = "RLPWD";
	
	public static final String PROP_REFRESH_ITERVAL = "REFRESH_INTERVAL";
	private static final int PROP_REFRESH_ITERVAL_DEFAULT = 2;
	public static final String PROP_REFRESH_CONF = "REFRESH_SETTINGS";
	private static final int PROP_REFRESH_CONF_DEFAULT = 5;
	
	public static final String PROP_USE_ARCHIVE = "USE_ARCHIVE";
	public static final String PROP_ARCHIVE_FILE = "USE_ARCHIVE_FILENAME";
	public static final String PROP_ARCHIVE_FILE_DEFAULT = Path.of(System.getProperty("user.home"), "ShellyStore.arc").toString();
	public static final String PROP_AUTORELOAD_ARCHIVE = "AUTORELOAD";
	
	public static final String BASE_SCAN_IP = "BASE_SCAN";
	public static final String FIRST_SCAN_IP = "FIRST_SCAN";
	public static final int FIST_SCAN_IP_DEFAULT = 1;
	public static final String LAST_SCAN_IP = "LAST_SCAN";
	public static final int LAST_SCAN_IP_DEFAULT = 254;
	
	public static final String PROP_IDE_TAB_SIZE = "IDE_TAB_SIZE";
	public static final int IDE_TAB_SIZE_DEFAULT = 4;
	public static final String PROP_IDE_FONT_SIZE = "IDE_FONT_SIZE";
	public static final int IDE_FONT_SIZE_DEFAULT = 12;
	public static final String IDE_AUTOINDENT = "IDE_INDENT";
	public static final String IDE_AUTOCLOSE_CURLY = "CL_CURLY";
	public static final String IDE_AUTOCLOSE_BRACKET = "CL_BRACK";
	public static final String IDE_AUTOCLOSE_SQUARE = "CL_SQUSARE";
	public static final String IDE_AUTOCLOSE_STRING = "CL_STRING";
	public static final String PROP_IDE_DARK = "IDE_DARK";
	
	public static final String VERSION_IGNORE = "IGNORE_VERION_DOWNLOAD";
	
	public enum PropertyEvent {CHANGE};
	
	private static ArrayList<AppPropertyListener> listeners = new ArrayList<>();
	private static ScannerProperties ap;
	
	private ScannerProperties(Path file) {
		super(file);
		try { // in case of error or no file (true) use default configuration
			load(true);
		} catch (IOException e) {
			Msg.errorMsg(e);
		}
		defaultBoolProperty(PROP_TOOLBAR_CAPTIONS, true);
		defaultProperty(PROP_CSV_SEPARATOR, PROP_CSV_SEPARATOR_DEFAULT);
		defaultProperty(PROP_SCAN_MODE, PROP_SCAN_MODE_DEFAULT);
		defaultProperty(PROP_DCLICK_ACTION, PROP_DCLICK_ACTION_DEFAULT);
		defaultProperty(PROP_TEMP_UNIT, PROP_TEMP_UNIT_DEFAULT);
		defaultProperty(PROP_UPTIME_MODE, PROP_UPTIME_MODE_DEFAULT);
		defaultProperty(PROP_UPDATECHK_ACTION, PROP_UPDATECHK_DEV);
		defaultProperty(PROP_DETAILED_VIEW_SCREEN, PROP_DETAILED_VIEW_SCREEN_FULL);
		defaultIntProperty(PROP_REFRESH_ITERVAL, PROP_REFRESH_ITERVAL_DEFAULT);
		defaultIntProperty(PROP_REFRESH_CONF, PROP_REFRESH_CONF_DEFAULT);
		defaultBoolProperty(PROP_USE_ARCHIVE, true);
		defaultBoolProperty(PROP_AUTORELOAD_ARCHIVE, false);
	}
	
	public static ScannerProperties init(Path file) {
		ap = new ScannerProperties(file);
		return ap;
	}
	
	public static ScannerProperties instance() {
		return ap;
	}
	
	// Observable ability
	
	public synchronized void addListener(AppPropertyListener l) {
		listeners.add(l);
	}
	
	public synchronized void removeListeners() {
		listeners.clear();
	}
	
	public synchronized void removeListener(AppPropertyListener l) {
		listeners.remove(l);
	}
	
	/**
	 * @throws NullPointerException if value is null
	 */
	@Override
	public Object setProperty(String key, String value) {
		Object ret = super.setProperty(key, value);
		if(value.equals(ret) == false) {
			listeners.forEach(l -> l.update(PropertyEvent.CHANGE, key));
		}
        return ret;
    }
	
	public interface AppPropertyListener {
		void update(PropertyEvent e, String propKey);
	}
}