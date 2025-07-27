package it.usna.shellyscan.view.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CronUtils {
	private final static String[] WEEK_DAYS = new String[] {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
	private final static String[] MONTHS = new String[] {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
	
	private final static String REX_0_59 = "([1-5]?\\d)";
	private final static String REX_00_59 = "([0-5]?\\d)"; // 00 is valid
	private final static String REX_0_23 = "(1\\d|2[0-3]|\\d)";
	private final static String REX_1_31 = "([12]\\d|3[01]|[1-9])";
	private final static String REX_1_9999 = "([1-9]\\d{0,3})";
	private final static String REX_1_12 = "(1[0-2]|[1-9])";
	private final static String REX_0_6 = "([0-6])";

	private final static String REX_HOUR = "(" + REX_0_23 + "-" + REX_0_23 + "|\\*/" + REX_1_9999 + "|" + REX_0_23 + "/" + REX_1_9999 + "|" + REX_0_23 + ")";
	private final static String REX_MINUTE = "(" + REX_0_59 + "-" + REX_0_59 +  "|\\*/" + REX_1_9999 + "|" + REX_0_59 + "/" + REX_1_9999 + "|" + REX_0_59  + ")";
//	private final static String REX_SECOND = "(" + REX_0_59 + "-" + REX_0_59 +  "|\\*/" + REX_1_9999 + "|" + REX_0_59 + "/" + REX_1_9999 + "|" + REX_0_59  + ")";
	private final static String REX_MONTHDAY = "(" + REX_1_31 + "-" + REX_1_31 +  "|\\*/" + REX_1_9999 + "|" + REX_1_31 + "/" + REX_1_9999 + "|" + REX_1_31 + ")";
	private final static String REX_MONTH = "(" + REX_1_12 + "-" + REX_1_12 +  "|\\*/" + REX_1_9999 + "|" + REX_1_12 + "/" + REX_1_9999 + "|" + REX_1_12 + ")";
	private final static String REX_WEEKDAY = "(" + REX_0_6 + "-" + REX_0_6 +  "|\\*/" + REX_1_9999 + "|" + REX_0_6 + "/" + REX_1_9999 + "|" + REX_0_6 + ")";

	private final static String REX_HOURS = "\\*|" + REX_HOUR + "(," + REX_HOUR + ")*";
	private final static String REX_MINUTES = "\\*|" + REX_MINUTE + "(," + REX_MINUTE + ")*";
	private final static String REX_SECONDS = REX_MINUTES; //"\\*|" + REX_SECOND + "(," + REX_SECOND + ")*";
	private final static String REX_MONTHDAYS = "\\*|" + REX_MONTHDAY + "(," + REX_MONTHDAY + ")*";
	private final static String REX_MONTHS = "\\*|" + REX_MONTH + "(," + REX_MONTH + ")*";
	private final static String REX_WEEKDAYS = "\\*|" + REX_WEEKDAY + "(," + REX_WEEKDAY + ")*";
	
	public final static Pattern HOUR_0_23_PATTERN = Pattern.compile(REX_0_23);
	public final static Pattern MINUTE_0_59_PATTERN = Pattern.compile(REX_0_59);

	public final static Pattern HOURS_PATTERN = Pattern.compile(REX_HOURS);
	public final static Pattern MINUTES_PATTERN = Pattern.compile(REX_MINUTES);
	public final static Pattern SECONDS_PATTERN = MINUTES_PATTERN; //Pattern.compile(REX_SECONDS);
	public final static Pattern DAYS_PATTERN = Pattern.compile(REX_MONTHDAYS);

	public final static Pattern CRON_PATTERN = Pattern.compile("(" + REX_SECONDS + ") (" + REX_MINUTES + ") (" + REX_HOURS + ") (" + REX_MONTHDAYS + ") (" + REX_MONTHS + ") (" + REX_WEEKDAYS + ")");
	public final static Pattern SUNSET_PATTERN = Pattern.compile("@(sunset|sunrise)((\\+|-)(?<HOUR>" + REX_0_23 + ")h((?<MINUTE>" + REX_00_59 + ")m)?)?( (?<DAY>" + REX_MONTHDAYS + ") (?<MONTH>" + REX_MONTHS + ") (?<WDAY>" + REX_WEEKDAYS + "))?");
	
//	private final static String REX_WEEKDAYS_S = "(SUN|MON|TUE|WED|TH|FRI|SAT)(,(SUN|MON|TUE|WED|THU|FRI|SAT))*";
	private final static String REX_WEEKDAYS_S = "([0-6])(,[0-6])*";
	public final static Pattern CRON_PATTERN_TH_WD = Pattern.compile("\\* (" + REX_0_59 + ") (" + REX_0_23 + ") \\* \\* (" + REX_WEEKDAYS_S + ")");

//	public final static Pattern RANDOM_PATTERN =  Pattern.compile("@random:\\{\"from\":\"(" + REX_SECONDS + ") (" + REX_MINUTES + ") (" + REX_HOURS + ") (" + REX_MONTHDAYS + ") (" + REX_MONTHS + ") (" + REX_WEEKDAYS + ")\", ?\"to\":\"(" +
//			REX_SECONDS + ") (" + REX_MINUTES + ") (" + REX_HOURS + ") (" + REX_MONTHDAYS + ") (" + REX_MONTHS + ") (" + REX_WEEKDAYS + ")\", ?\"number\":\\d+\\}");

	private final static Pattern FIND_PATTERN = Pattern.compile("(\\d+-\\d+|\\d+)");
	
	private CronUtils() {}
	
	// currently ignore */x and x/y
	public static List<Integer> fragmentToInt(String f) {
		List<Integer> res = new ArrayList<>();
		Matcher m = FIND_PATTERN.matcher(f);
		while(m.find()) {
			String frag = m.group(1);
			if(frag.contains("-")) {
				String[] split = frag.split("-");
				int max = Integer.parseInt(split[1]);
				for(int val = Integer.parseInt(split[0]); val <= max; val++) {
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
	
	// this is the only way Wall Display thermostat understand it
	public static String daysOfWeekAsString(String in) {
		if(in.equals("*")) {
			in ="0-6";
		}
		return fragmentToInt(in).stream().map(num -> WEEK_DAYS[num]).collect(Collectors.joining(","));
	}
	
	/**
	 * Shelly implementation wants single values before groups (5,1-3 instead of 1-3,5)
	 * @param list list of single valued (Integer)
	 * @return a string in cron format
	 */
	public static String listAsCronString(List<Integer> list) {
		List<String> single = new ArrayList<>();
		List<String> group = new ArrayList<>();
		list.add(Integer.MAX_VALUE); // tail
		int init = list.get(0);
		int last = init;
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i) > last + 1) {
				if (init == last) {
					single.add(init + "");
					init = last = list.get(i);
				} else if (init == last - 1) {
					single.add(init + "," + last);
					init = last = list.get(i);
				} else if (init < last - 1) {
					group.add(init + "-" + last);
					init = last = list.get(i);
				}
			} else {
				last = list.get(i);
			}
		}
		single.addAll(group);
		return single.stream().collect(Collectors.joining(","));
	}
	
	/**
	 * timespec comparator by hour and minutes; only numbers allowed
	 * @param timespec1
	 * @param timespec2
	 * @return
	 */
	public static int hmCompare(String timespec1, String timespec2) {
		String t1[] = timespec1.split(" ");
		String t2[] = timespec2.split(" ");
		try {
			return String.format("%02d%02d", Integer.parseInt(t1[2]), Integer.parseInt(t1[1])).compareTo(String.format("%02d%02d", Integer.parseInt(t2[2]), Integer.parseInt(t2[1])));
		} catch (java.util.IllegalFormatException e) { // not a simple number
			return 0;
		}
	}
}


//https://next-api-docs.shelly.cloud/gen2/ComponentsAndServices/Schedule
//https://github.com/mongoose-os-libs/cron
//https://crontab.guru/
//https://regex101.com/
//https://www.freeformatter.com/regex-tester.html

//http://<ip>/rpc/Schedule.DeleteAll
//http://<ip>/rpc/Schedule.Create?timespec="0 0 22 * * FRI"&calls=[{"method":"Shelly.GetDeviceInfo"}]
//http://<ip>/rpc/Schedule.Create?timespec="10/100 * * * * *"&calls=[{"method":"light.toggle?id=0"}]

//notes: 10 not working (do 0); 100 not working (do 60)