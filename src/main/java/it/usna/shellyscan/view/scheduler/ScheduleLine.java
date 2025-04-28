package it.usna.shellyscan.view.scheduler;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ScheduleLine extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTextField hoursTextField;
	private JTextField minutesTextField;
	private JTextField secondsTextField;
	private JTextField daysTextField;
	private JPanel monthsPanel;
	private JPanel daysOfWeekPanel;
	private String daysOfWeek;
	private String months;
	private JTextField expressionField;
	private JRadioButton cronRadio;
	private JRadioButton beforeRiseRadio;
	private JRadioButton afterRiseRadio;
	private JRadioButton beforeSetRadio;
	private JRadioButton afterSetRadio;
	
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
	
	private final static Pattern HOUR_0_23_PATTERN = Pattern.compile(REX_0_23);
	private final static Pattern MINUTE_0_59_PATTERN = Pattern.compile(REX_0_59);

	private final static Pattern HOURS_PATTERN = Pattern.compile(REX_HOURS);
	private final static Pattern MINUTES_PATTERN = Pattern.compile(REX_MINUTES);
	private final static Pattern SECONDS_PATTERN = Pattern.compile(REX_SECONDS);
	private final static Pattern DAYS_PATTERN = Pattern.compile(REX_MONTHDAYS);

	private final static Pattern MONTHS_FIND_PATTERN = Pattern.compile(",?" + REX_MONTH);
	private final static Pattern WEEKDAYS_FIND_PATTERN = Pattern.compile(",?" + REX_WEEKDAY);

	private final static Pattern CRON_PATTERN = Pattern.compile("(" + REX_SECONDS + ") (" + REX_MINUTES + ") (" + REX_SECONDS + ") (" + REX_MONTHDAYS + ") (" + REX_MONTHS + ") (" + REX_WEEKDAYS + ")");
	private final static Pattern SUNSET_PATTERN = Pattern.compile("@(sunset|sunrise)((\\+|-)(?<HOUR>" + REX_0_23 + ")h((?<MINUTE>" + REX_0_59 + ")m)?)?( (?<DAY>" + REX_MONTHDAYS + ") (?<MONTH>" + REX_MONTHS + ") (?<WDAY>" + REX_WEEKDAYS + "))?");

	private final static String[] WEEK_DAYS = new String[] {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
	private final static String[] MONTHS = new String[] {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

	ScheduleLine(String cronLine) {
		GridBagLayout gbl_panel = new GridBagLayout();
//		gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 10, 0, 10};
		// gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_panel.columnWeights = new double[] { 1.0, 1.0, 1.0, 1.0, 0.5, 0.0, 0 };
		gbl_panel.rowWeights = new double[] { 1.0, 1.0, 1.0 };
		setLayout(gbl_panel);

		JLabel lblNewLabel = new JLabel("Hours");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);

		JLabel lblNewLabel_2 = new JLabel("Seconds");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 2;
		gbc_lblNewLabel_2.gridy = 0;
		add(lblNewLabel_2, gbc_lblNewLabel_2);

		JLabel lblNewLabel_3 = new JLabel("Days");
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.gridx = 3;
		gbc_lblNewLabel_3.gridy = 0;
		add(lblNewLabel_3, gbc_lblNewLabel_3);

		hoursTextField = new JTextField();
		GridBagConstraints gbc_hoursTextField = new GridBagConstraints();
		gbc_hoursTextField.insets = new Insets(0, 0, 5, 5);
		gbc_hoursTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_hoursTextField.gridx = 0;
		gbc_hoursTextField.gridy = 1;
		add(hoursTextField, gbc_hoursTextField);
		hoursTextField.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel("Minutes");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 1;
		gbc_lblNewLabel_1.gridy = 0;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		minutesTextField = new JTextField();
		GridBagConstraints gbc_minutesTextField = new GridBagConstraints();
		gbc_minutesTextField.insets = new Insets(0, 0, 5, 5);
		gbc_minutesTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_minutesTextField.gridx = 1;
		gbc_minutesTextField.gridy = 1;
		add(minutesTextField, gbc_minutesTextField);
		minutesTextField.setColumns(10);

		secondsTextField = new JTextField();
		GridBagConstraints gbc_secondsTextField = new GridBagConstraints();
		gbc_secondsTextField.insets = new Insets(0, 0, 5, 5);
		gbc_secondsTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_secondsTextField.gridx = 2;
		gbc_secondsTextField.gridy = 1;
		add(secondsTextField, gbc_secondsTextField);
		secondsTextField.setColumns(10);

		daysTextField = new JTextField();
		GridBagConstraints gbc_daysTextField = new GridBagConstraints();
		gbc_daysTextField.insets = new Insets(0, 0, 5, 5);
		gbc_daysTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_daysTextField.gridx = 3;
		gbc_daysTextField.gridy = 1;
		add(daysTextField, gbc_daysTextField);
		daysTextField.setColumns(10);

		monthsPanel = monthsPanel();
		GridBagConstraints gbc_months = new GridBagConstraints();
		gbc_months.gridheight = 2;
		gbc_months.insets = new Insets(0, 0, 5, 5);
		gbc_months.fill = GridBagConstraints.VERTICAL;
		gbc_months.gridx = 4;
		gbc_months.gridy = 0;
		add(monthsPanel, gbc_months);

		daysOfWeekPanel = daysOfWeekPanel();
		GridBagConstraints gbc_daysOfWeek = new GridBagConstraints();
		gbc_daysOfWeek.gridheight = 2;
		gbc_daysOfWeek.insets = new Insets(0, 0, 5, 5);
		gbc_daysOfWeek.fill = GridBagConstraints.BOTH;
		gbc_daysOfWeek.gridx = 5;
		gbc_daysOfWeek.gridy = 0;
		add(daysOfWeekPanel, gbc_daysOfWeek);

		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 4;
		gbc_panel.insets = new Insets(0, 0, 0, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 2;
		add(panel, gbc_panel);
		
		ActionListener changeType = e -> { // cron <-> sunset/sunrise
			if(((JRadioButton)e.getSource()).isSelected()) {
				if(e.getSource() == cronRadio) {
					secondsTextField.setEnabled(true);
				} else {
					secondsTextField.setEnabled(false);
					secondsTextField.setText("0");
					if(HOUR_0_23_PATTERN.matcher(hoursTextField.getText()).matches() == false) {
						hoursTextField.setText("0");
						hoursTextField.setForeground(null);
					}
					if(MINUTE_0_59_PATTERN.matcher(minutesTextField.getText()).matches() == false) {
						minutesTextField.setText("0");
						minutesTextField.setForeground(null);
					}
				}
				asString();
			}
		};

		cronRadio = new JRadioButton("Time");
		cronRadio.addActionListener(changeType);
		panel.add(cronRadio);

		beforeRiseRadio = new JRadioButton("Before sunrise");
		beforeRiseRadio.addActionListener(changeType);
		panel.add(beforeRiseRadio);

		afterRiseRadio = new JRadioButton("After sunrise");
		afterRiseRadio.addActionListener(changeType);
		panel.add(afterRiseRadio);

		beforeSetRadio = new JRadioButton("Before sunset");
		beforeSetRadio.addActionListener(changeType);
		panel.add(beforeSetRadio);
		
		afterSetRadio = new JRadioButton("After sunset");
		afterSetRadio.addActionListener(changeType);
		panel.add(afterSetRadio);
		
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(cronRadio);
		modeGroup.add(beforeRiseRadio);
		modeGroup.add(afterRiseRadio);
		modeGroup.add(beforeSetRadio);
		modeGroup.add(afterSetRadio);
		
		expressionField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.insets = new Insets(0, 0, 0, 5);
		gbc_textField.gridwidth = 2;
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 4;
		gbc_textField.gridy = 2;
		add(expressionField, gbc_textField);

		expressionField.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				String exp = fragStrToNum(expressionField.getText());
				expressionField.setText(exp);
				if((CRON_PATTERN.matcher(exp).matches() || SUNSET_PATTERN.matcher(exp).matches())) {
					expressionField.setForeground(null);
					setCron(exp);
				} else {
					expressionField.setForeground(Color.red);
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				expressionField.setForeground(null); // default color
			}
		});

		FocusListener fragmentsFocusListener = new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				JTextField f = (JTextField)e.getComponent();
				f.setText(f.getText().replaceAll("\\s", ""));
				if(f.getText().isEmpty()) {
					f.setText("*");
				}
				asString();
			}

			@Override
			public void focusGained(FocusEvent e) {
				e.getComponent().setForeground(null); // default color
			}
		};

		hoursTextField.addFocusListener(fragmentsFocusListener);
		minutesTextField.addFocusListener(fragmentsFocusListener);
		secondsTextField.addFocusListener(fragmentsFocusListener);
		daysTextField.addFocusListener(fragmentsFocusListener);

		setCron(cronLine);
	}

	private JPanel monthsPanel() {
		JPanel panel = new JPanel();
		ActionListener change = e -> {
			computeMonths();
			asString();
		};
		panel.setLayout(new GridLayout(1, 0, 0, 0));
		for (Month m : Month.values()) {
			JCheckBox chckbxNewCheckBox = new JCheckBox(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
			chckbxNewCheckBox.addActionListener(change);
			panel.add(chckbxNewCheckBox);
		}
		return panel;
	}

	private void computeMonths() {
		List<Integer> seq = new ArrayList<>();
		for (int i = 0; i < monthsPanel.getComponentCount(); i++) {
			if (((JCheckBox) monthsPanel.getComponent(i)).isSelected()) {
				seq.add(i + 1);
			}
		}
		months = (seq.size() == 12 || seq.isEmpty()) ? "*" : listAsCronString(seq);
	}

	private JPanel daysOfWeekPanel() {
		JPanel panel = new JPanel();
		ActionListener change = e -> {
			computeDaysOfWeek();
			asString();
		};
		panel.setLayout(new GridLayout(1, 0, 0, 0));
		for (DayOfWeek d : DayOfWeek.values()) {
			JCheckBox chckbxNewCheckBox = new JCheckBox(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
			chckbxNewCheckBox.addActionListener(change);
			panel.add(chckbxNewCheckBox);
		}
		return panel;
	}

	private void computeDaysOfWeek() {
		List<Integer> seq = new ArrayList<>();
		if (((JCheckBox) daysOfWeekPanel.getComponent(6)).isSelected()) { // SUN
			seq.add(0);
		}
		for (int i = 0; i < 6; i++) {
			if (((JCheckBox) daysOfWeekPanel.getComponent(i)).isSelected()) {
				seq.add(i + 1);
			}
		}
		daysOfWeek = (seq.size() == 7 || seq.isEmpty()) ? "*" : listAsCronString(seq);
	}

	public void setCron(String cronLine) {
		// assume cronLine is correct
		cronLine = fragStrToNum(cronLine);
		
		final String[] values;
		Matcher sm = SUNSET_PATTERN.matcher(cronLine);
		if(sm.matches()) {
			secondsTextField.setEnabled(false);
			String hours = sm.group("HOUR");
			String minutes = sm.group("MINUTE");
			String day = sm.group("DAY");
			String month = sm.group("MONTH");
			String wday = sm.group("WDAY");
			values = new String[] {"0", (minutes != null) ? minutes : "0", (hours != null) ? hours : "0", (day != null) ? day : "*", (month != null) ? month : "*", (wday != null) ? wday : "*"};
			if(cronLine.startsWith("@sunrise+")) {
				afterRiseRadio.setSelected(true);
			} else if(cronLine.startsWith("@sunrise-")) {
				beforeRiseRadio.setSelected(true);
			} else if(cronLine.startsWith("@sunrise")) {
				afterRiseRadio.setSelected(true);
			} else if(cronLine.startsWith("@sunset+")) {
				afterSetRadio.setSelected(true);
			} else if(cronLine.startsWith("@sunset-")) {
				beforeSetRadio.setSelected(true);
			} else if(cronLine.startsWith("@sunset")) {
				afterSetRadio.setSelected(true);
			}
		} else {
			cronRadio.setSelected(true);
			secondsTextField.setEnabled(true);
			values = cronLine.split(" ");
		}
		
		secondsTextField.setText(values[0]);
		minutesTextField.setText(values[1]);
		hoursTextField.setText(values[2]);
		daysTextField.setText(values[3]);

		for(int i = 0; i < 12; i++) {
			((JCheckBox) monthsPanel.getComponent(i)).setSelected(false);
			monthsPanel.getComponent(i).setEnabled(true);
		}
		String months = values[4];
		if(months.contains("/")) {
			this.months = months;
			for(int i = 0; i < 12; i++) {
				monthsPanel.getComponent(i).setEnabled(false);
			}
		} else {
			if(months.equals("*")) {
				months ="1-12";
			}
			for(int month: fragmentToInt(MONTHS_FIND_PATTERN.matcher(months))) {
				((JCheckBox) monthsPanel.getComponent(month - 1)).setSelected(true);
			}
			computeMonths();
		}

		for(int i = 0; i < 7; i++) {
			((JCheckBox) daysOfWeekPanel.getComponent(i)).setSelected(false);
			daysOfWeekPanel.getComponent(i).setEnabled(true);
		}
		String daysOfWeek = values[5];
		if(daysOfWeek.contains("/")) {
			this.daysOfWeek = daysOfWeek;
			for(int i = 0; i < 7; i++) {
				daysOfWeekPanel.getComponent(i).setEnabled(false);
			}
		} else {
			if(daysOfWeek.equals("*")) {
				daysOfWeek ="0-6";
			}
			for(int weekDay: fragmentToInt(WEEKDAYS_FIND_PATTERN.matcher(daysOfWeek))) {
				((JCheckBox) daysOfWeekPanel.getComponent((weekDay + 6) % 7)).setSelected(true);
			}
			computeDaysOfWeek();
		}
		asString();
	}
	
	public void asString() {
		String res = "";
		String minutes = minutesTextField.getText();
		String hours = hoursTextField.getText();
		if(cronRadio.isSelected()) {
			String seconds = secondsTextField.getText();
			res = seconds + " " + minutes + " " + hours;
			secondsTextField.setForeground(SECONDS_PATTERN.matcher(seconds).matches() ? null : Color.red);
			minutesTextField.setForeground(MINUTES_PATTERN.matcher(minutes).matches() ? null : Color.red);
			hoursTextField.setForeground(HOURS_PATTERN.matcher(hours).matches() ? null : Color.red);
		} else if(beforeRiseRadio.isSelected()) {
			res = "@sunrise-" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		} else if(afterRiseRadio.isSelected()) {
			res = "@sunrise+" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		} else if(beforeSetRadio.isSelected()) {
			res = "@sunset-" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		} else if(afterSetRadio.isSelected()) {
			res = "@sunset+" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		}

		String days = daysTextField.getText();
		daysTextField.setForeground(DAYS_PATTERN.matcher(days).matches() ? null : Color.red);
		res += " " + days + " " + months + " " + daysOfWeek;

		expressionField.setText(res);
		expressionField.setForeground((CRON_PATTERN.matcher(res).matches() || SUNSET_PATTERN.matcher(res).matches()) ? null : Color.red);
	}
	
	private static List<Integer> fragmentToInt(Matcher m) {
		List<Integer> res = new ArrayList<>();
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
	private static String fragStrToNum(String in) {
		for(int i = 0; i < MONTHS.length; i++) {
			in = in.replaceAll("(?i)" + MONTHS[i], String.valueOf(i + 1));
		}
		for(int i = 0; i < WEEK_DAYS.length; i++) {
			in = in.replaceAll("(?i)([^@])" + WEEK_DAYS[i], "$1" + String.valueOf(i)); // ([^@]) -> @sunrise / @sunset
		}
		return in;
	}

	private static String listAsCronString(List<Integer> list) {
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