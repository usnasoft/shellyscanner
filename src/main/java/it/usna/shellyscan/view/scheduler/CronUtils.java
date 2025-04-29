package it.usna.shellyscan.view.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CronUtils {
	private final static String[] WEEK_DAYS = new String[] {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
	private final static String[] MONTHS = new String[] {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
	
	private final static Pattern FIND_PATTERN = Pattern.compile("(\\d+-\\d+|\\*/\\d+|\\d+/\\d+|\\d+)");
//	private final static Pattern MONTHS_FIND_PATTERN = Pattern.compile(",?" + REX_MONTH);
//	private final static Pattern WEEKDAYS_FIND_PATTERN = Pattern.compile(",?" + REX_WEEKDAY);
	
	private CronUtils() {}
	
//	public static List<Integer> fragmentToInt(Matcher m) {
//		List<Integer> res = new ArrayList<>();
//		while(m.find()) {
//			String frag = m.group(1);
//			if(frag.contains("-")) {
//				String[] split = frag.split("-");
//				for(int val = Integer.parseInt(split[0]); val <= Integer.parseInt(split[1]); val++) {
//					res.add(val);
//				}
//			} else {
//				res.add(Integer.parseInt(frag));
//			}
//		}
//		return res;
//	}
	
	// currently ignore */x and x/y
	public static List<Integer> fragmentToInt(String f) {
		List<Integer> res = new ArrayList<>();
		Matcher m = FIND_PATTERN.matcher(f);
		while(m.find()) {
			String frag = m.group(1);
			if(frag.contains("-")) {
				String[] split = frag.split("-");
				for(int val = Integer.parseInt(split[0]); val <= Integer.parseInt(split[1]); val++) {
					res.add(val);
				}
			} else {
				res.add(Integer.parseInt(frag));
			}
		}
		return res;
	}

	// MON -> 1, DEC -> 12, ...
	public static String fragStrToNum(String in) {
		for(int i = 0; i < MONTHS.length; i++) {
			in = in.replaceAll("(?i)" + MONTHS[i], String.valueOf(i + 1));
		}
		for(int i = 0; i < WEEK_DAYS.length; i++) {
			in = in.replaceAll("(?i)([^@])" + WEEK_DAYS[i], "$1" + String.valueOf(i)); // ([^@]) -> @sunrise / @sunset
		}
		return in;
	}

	public static String listAsCronString(List<Integer> list) {
		list.add(Integer.MAX_VALUE); // tail
		String res = "";
		int init = list.get(0);
		int last = init;
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i) > last + 1) {
				if (init == last) {
					res += res.isEmpty() ? init : "," + init;
					init = last = list.get(i);
				} else if (init == last - 1) {
					res += res.isEmpty() ? init + "," + last : "," + init + "," + last;
					init = last = list.get(i);
				} else if (init < last - 1) {
					res += res.isEmpty() ? init + "-" + last : "," + init + "-" + last;
					init = last = list.get(i);
				}
			} else {
				last = list.get(i);
			}
		}
		return res;
	}
}
