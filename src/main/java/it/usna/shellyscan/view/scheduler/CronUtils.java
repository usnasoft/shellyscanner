package it.usna.shellyscan.view.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CronUtils {
	private final static String[] WEEK_DAYS = new String[] {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
	private final static String[] MONTHS = new String[] {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
	
	private final static String REX_0_59 = "([1-5]?\\d)";
	private final static String REX_0_23 = "(1\\d|2[0-3]|\\d)";
	private final static String REX_1_31 = "([12]\\d|3[01]|[1-9])";
	private final static String REX_1_9999 = "([1-9]\\d{0,3})";
	private final static String REX_1_12 = "(1[0-2]|[1-9])";
	private final static String REX_0_6 = "([0-6])";

	private final static String REX_HOUR = "(" + REX_0_23 + "-" + REX_0_23 + "|\\*/" + REX_1_9999 + "|" + REX_0_23 + "/" + REX_1_9999 + "|" + REX_0_23 + ")";
	private final static String REX_MINUTE = "(" + REX_0_59 + "-" + REX_0_59 +  "|\\*/" + REX_1_9999 + "|" + REX_0_59 + "/" + REX_1_9999 + "|" + REX_0_59  + ")";
	private final static String REX_SECOND = "(" + REX_0_59 + "-" + REX_0_59 +  "|\\*/" + REX_1_9999 + "|" + REX_0_59 + "/" + REX_1_9999 + "|" + REX_0_59  + ")";
	private final static String REX_MONTHDAY = "(" + REX_1_31 + "-" + REX_1_31 +  "|\\*/" + REX_1_9999 + "|" + REX_1_31 + "/" + REX_1_9999 + "|" + REX_1_31 + ")";
	private final static String REX_MONTH = "(" + REX_1_12 + "-" + REX_1_12 +  "|\\*/" + REX_1_9999 + "|" + REX_1_12 + "/" + REX_1_9999 + "|" + REX_1_12 + ")";
	private final static String REX_WEEKDAY = "(" + REX_0_6 + "-" + REX_0_6 +  "|\\*/" + REX_1_9999 + "|" + REX_0_6 + "/" + REX_1_9999 + "|" + REX_0_6 + ")";

	private final static String REX_HOURS = "\\*|" + REX_HOUR + "(," + REX_HOUR + ")*";
	private final static String REX_MINUTES = "\\*|" + REX_MINUTE + "(," + REX_MINUTE + ")*";
	private final static String REX_SECONDS = "\\*|" + REX_SECOND + "(," + REX_SECOND + ")*";
	private final static String REX_MONTHDAYS = "\\*|" + REX_MONTHDAY + "(," + REX_MONTHDAY + ")*";
	private final static String REX_MONTHS = "\\*|" + REX_MONTH + "(," + REX_MONTH + ")*";
	private final static String REX_WEEKDAYS = "\\*|" + REX_WEEKDAY + "(," + REX_WEEKDAY + ")*";
	
	public final static Pattern HOUR_0_23_PATTERN = Pattern.compile(REX_0_23);
	public final static Pattern MINUTE_0_59_PATTERN = Pattern.compile(REX_0_59);

	public final static Pattern HOURS_PATTERN = Pattern.compile(REX_HOURS);
	public final static Pattern MINUTES_PATTERN = Pattern.compile(REX_MINUTES);
	public final static Pattern SECONDS_PATTERN = Pattern.compile(REX_SECONDS);
	public final static Pattern DAYS_PATTERN = Pattern.compile(REX_MONTHDAYS);

	public final static Pattern CRON_PATTERN = Pattern.compile("(" + REX_SECONDS + ") (" + REX_MINUTES + ") (" + REX_SECONDS + ") (" + REX_MONTHDAYS + ") (" + REX_MONTHS + ") (" + REX_WEEKDAYS + ")");
	public final static Pattern SUNSET_PATTERN = Pattern.compile("@(sunset|sunrise)((\\+|-)(?<HOUR>" + REX_0_23 + ")h((?<MINUTE>" + REX_0_59 + ")m)?)?( (?<DAY>" + REX_MONTHDAYS + ") (?<MONTH>" + REX_MONTHS + ") (?<WDAY>" + REX_WEEKDAYS + "))?");
	
//	private final static Pattern FIND_PATTERN = Pattern.compile("(\\d+-\\d+|\\*/\\d+|\\d+/\\d+|\\d+)");
	private final static Pattern FIND_PATTERN = Pattern.compile("(\\d+-\\d+|\\d+)");
//	private final static Pattern MONTHS_FIND_PATTERN = Pattern.compile(",?" + REX_MONTH);
//	private final static Pattern WEEKDAYS_FIND_PATTERN = Pattern.compile(",?" + REX_WEEKDAY);
	
	private CronUtils() {}
	
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
			in = in.replaceAll("(?i)([^@])" + WEEK_DAYS[i], "$1" + String.valueOf(i)); // ([^@]) -> @sunrise / @sunset ... sun !
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