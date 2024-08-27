package it.usna.shellyscan.model.device.modules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface FirmwareManager {
//	final static Pattern VERSION_PATTERN = Pattern.compile(".*/v?([\\.\\d]+(-beta.*)?)(-|@).*");
	final static Pattern VERSION_PATTERN = Pattern.compile(".*/v?([\\.\\d]+(?:(-beta.*)|(-rc.*))?)(?:-|@).*");
	//e.g.: 20210429-100340/v1.10.4-g3f94cd7 - 20211222-144927/0.9.2-beta2-gc538a83 - 20211223-144928/v2.0.5@3f0fcbbe
	
	void chech();
	
	String current();
	
	String newBeta();
	
	String newStable();
	
	String update(boolean stable);
	
	boolean upadating();
	
	boolean isValid();
	
	static String getShortVersion(String fw) {
		if(fw == null) {
			return null;
		} else {
			Matcher m = VERSION_PATTERN.matcher(fw);
			return m.find() ? m.group(1) : fw;
		}
	}
}