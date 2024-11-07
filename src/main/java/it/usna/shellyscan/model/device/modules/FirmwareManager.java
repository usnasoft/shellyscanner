package it.usna.shellyscan.model.device.modules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface FirmwareManager {
	final static Pattern VERSION_PATTERN = Pattern.compile(".*/v?([\\.\\d]+(?:(?:-beta.*)|(?:-rc.*))?)(?:-|@).*?(-beta\\d*(-QA)?)?$");
	//e.g.:
	// 20210429-100340/v1.10.4-g3f94cd7 - 20211222-144927/0.9.2-beta2-gc538a83
	// 20231107-162425/v1.14.1-rc1-g0617c15 - 20211223-144928/v2.0.5@3f0fcbbe
	// 20240825-205857/2.2.0-b13b6e07-beta7
	// 20241031-171026/2.3.0-d508f135-beta4-QA
	
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
			if(m.find()) {
				String b = m.group(2);
				return (b == null) ? m.group(1) : m.group(1) + b;
			} else {
				return fw;
			}
		}
	}
	
//	public static void main(String ...strings) {
//		System.out.println(getShortVersion("20240825-205857/2.2.0-b13b6e07-beta7"));
//		System.out.println(getShortVersion("20211222-144927/0.9.2-beta2-gc538a83"));
//		System.out.println(getShortVersion("20211222-144927/0.9.2-beta2-gc538a83"));
//		System.out.println(getShortVersion("20241031-171026/2.3.0-d508f135-beta4-QA"));
//	}
}