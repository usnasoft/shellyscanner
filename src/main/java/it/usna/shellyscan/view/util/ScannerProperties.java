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
	public final static String PROP_TOOLBAR_CAPTIONS = "T_CAPTIONS";
	public final static String PROP_CSV_SEPARATOR = "CSV_SEPARATOR";
	private final static String PROP_CSV_SEPARATOR_DEFAULT = ",";
	public final static String PROP_SCAN_MODE = "SCAN_MODE";
	private final static String PROP_SCAN_MODE_DEFAULT = "FULL";
	public final static String PROP_DCLICK_ACTION = "DCLICK_ACTION";
	private final static String PROP_DCLICK_ACTION_DEFAULT = "DET";
	public final static String PROP_DEFAULT_FILTER_IDX = "DEFAULT_FILTER";
	
	public final static String PROP_UPTIME_MODE = "UPTIME_MODE";
	private final static String PROP_UPTIME_MODE_DEFAULT = "SEC";
	
	public final static String PROP_TEMP_UNIT = "TEMP_UNIT";
	private final static String PROP_TEMP_UNIT_DEFAULT = "C";
	
	public final static String PROP_UPDATECHK_ACTION = "UPDATE_CHK";
	public final static String PROP_UPDATECHK_NEVER = "NEVER";
	public final static String PROP_UPDATECHK_STABLE = "STABLE";
	public final static String PROP_UPDATECHK_DEV = "BETA";
	
	public final static String PROP_CHARTS_START = "CHART_DEF";
	public final static String PROP_CHARTS_EXPORT = "CHART_EXPORT";
	
	public final static String PROP_DETAILED_VIEW_SCREEN = "DETAIL_SCREEN";
	public final static String PROP_DETAILED_VIEW_SCREEN_FULL = "FULL";
	public final static String PROP_DETAILED_VIEW_SCREEN_AS_IS = "ASIS";
	public final static String PROP_DETAILED_VIEW_SCREEN_HORIZONTAL = "HOR";
	public final static String PROP_DETAILED_VIEW_SCREEN_ESTIMATE = "COMP";
	
	public final static String PROP_LOGIN_USER = "RLUSER";
	public final static String PROP_LOGIN_PWD = "RLPWD";
	
	public final static String PROP_REFRESH_ITERVAL = "REFRESH_INTERVAL";
	private final static int PROP_REFRESH_ITERVAL_DEFAULT = 2;
	public final static String PROP_REFRESH_CONF = "REFRESH_SETTINGS";
	private final static int PROP_REFRESH_CONF_DEFAULT = 5;
	
	public final static String PROP_USE_ARCHIVE = "USE_ARCHIVE";
	public final static String PROP_ARCHIVE_FILE = "USE_ARCHIVE_FILENAME";
	public final static String PROP_ARCHIVE_FILE_DEFAULT = Path.of(System.getProperty("user.home"), "ShellyStore.arc").toString();
	public final static String PROP_AUTORELOAD_ARCHIVE = "AUTORELOAD";
	
	public final static String BASE_SCAN_IP = "BASE_SCAN";
	public final static String FIRST_SCAN_IP = "FIRST_SCAN";
	public final static int FIST_SCAN_IP_DEFAULT = 1;
	public final static String LAST_SCAN_IP = "LAST_SCAN";
	public final static int LAST_SCAN_IP_DEFAULT = 254;
	
	public final static String PROP_IDE_TAB_SIZE = "IDE_TAB_SIZE";
	public final static int IDE_TAB_SIZE_DEFAULT = 4;
	public final static String PROP_IDE_FONT_SIZE = "IDE_FONT_SIZE";
	public final static int IDE_FONT_SIZE_DEFAULT = 12;
	public final static String IDE_AUTOINDENT = "IDE_INDENT";
	public final static String IDE_AUTOCLOSE_CURLY = "CL_CURLY";
	public final static String IDE_AUTOCLOSE_BRACKET = "CL_BRACK";
	public final static String IDE_AUTOCLOSE_SQUARE = "CL_SQUSARE";
	public final static String IDE_AUTOCLOSE_STRING = "CL_STRING";
	public final static String PROP_IDE_DARK = "IDE_DARK";
	
	public final static String VERSION_IGNORE = "IGNORE_VERION_DOWNLOAD";
	
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