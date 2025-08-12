package it.usna.shellyscan.view.scheduler;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemListener;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.view.util.Msg;

/**
 * complete CRON panel; job details will be added by implementing classes
 */
public abstract class AbstractCronPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final ImageIcon EDIT_IMG = new ImageIcon(AbstractCronPanel.class.getResource("/images/Write16.png"));
	private JTextField hoursTextField;
	private JTextField minutesTextField;
	private JTextField secondsTextField;
	private JTextField daysTextField;
	private JPanel monthsPanel;
	private JPanel daysOfWeekPanel;
	private String daysOfWeek;
	private String months;
	private JRadioButton cronRadio;
	private JRadioButton beforeRiseRadio;
	private JRadioButton afterRiseRadio;
	private JRadioButton beforeSetRadio;
	private JRadioButton afterSetRadio;
	protected JTextField expressionField;
	protected JDialog parentDlg;

	protected static final String DEF_CRON = "0 0 * * * *";
	
	/**
	 * @wbp.parser.constructor
	 * used for design
	 */
	protected AbstractCronPanel(JDialog parent) {
		this.parentDlg = parent;
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
		initCronSection();
	}

	protected void initCronSection() {
		GridBagLayout gbl_panel = new GridBagLayout();
		//		gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 10, 0, 10};
		// gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_panel.columnWeights = new double[] { 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
//		gbl_panel.rowWeights = new double[] { 1.0, 1.0, 1.0};
		setLayout(gbl_panel);

		JLabel lblNewLabel = new JLabel(LABELS.getString("lblHours"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 3, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);

		JButton btnHoursSelec = new JButton(EDIT_IMG);
		btnHoursSelec.setContentAreaFilled(false);
		btnHoursSelec.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_btnHoursSelec = new GridBagConstraints();
		gbc_btnHoursSelec.insets = new Insets(1, 0, 5, 8);
		gbc_btnHoursSelec.gridx = 1;
		gbc_btnHoursSelec.gridy = 0;
		add(btnHoursSelec, gbc_btnHoursSelec);
		btnHoursSelec.addActionListener(e -> new CronValuesDialog(parentDlg, hoursTextField, 0, 24));

		JButton btnMinutesSelec = new JButton(EDIT_IMG);
		btnMinutesSelec.setContentAreaFilled(false);
		btnMinutesSelec.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_btnMinutesSelec = new GridBagConstraints();
		gbc_btnMinutesSelec.insets = new Insets(1, 0, 5, 8);
		gbc_btnMinutesSelec.gridx = 3;
		gbc_btnMinutesSelec.gridy = 0;
		add(btnMinutesSelec, gbc_btnMinutesSelec);
		btnMinutesSelec.addActionListener(e -> new CronValuesDialog(parentDlg, minutesTextField, 0, 60));

		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("lblSeconds"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 3, 5, 5);
		gbc_lblNewLabel_2.gridx = 4;
		gbc_lblNewLabel_2.gridy = 0;
		add(lblNewLabel_2, gbc_lblNewLabel_2);

		JButton btnSecondsSelec = new JButton(EDIT_IMG);
		btnSecondsSelec.setContentAreaFilled(false);
		btnSecondsSelec.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_btnSecondsSelec = new GridBagConstraints();
		gbc_btnSecondsSelec.insets = new Insets(1, 0, 5, 8);
		gbc_btnSecondsSelec.gridx = 5;
		gbc_btnSecondsSelec.gridy = 0;
		add(btnSecondsSelec, gbc_btnSecondsSelec);
		btnSecondsSelec.addActionListener(e -> new CronValuesDialog(parentDlg, secondsTextField, 0, 60));

		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("lblDays"));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 3, 5, 5);
		gbc_lblNewLabel_3.gridx = 6;
		gbc_lblNewLabel_3.gridy = 0;
		add(lblNewLabel_3, gbc_lblNewLabel_3);

		JButton btnDaysSelec = new JButton(EDIT_IMG);
		btnDaysSelec.setContentAreaFilled(false);
		btnDaysSelec.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_btnDaysSelec = new GridBagConstraints();
		gbc_btnDaysSelec.insets = new Insets(1, 0, 5, 8);
		gbc_btnDaysSelec.gridx = 7;
		gbc_btnDaysSelec.gridy = 0;
		add(btnDaysSelec, gbc_btnDaysSelec);
		btnDaysSelec.addActionListener(e -> new CronValuesDialog(parentDlg, daysTextField, 1, 32));

		hoursTextField = new JTextField();
		GridBagConstraints gbc_hoursTextField = new GridBagConstraints();
		gbc_hoursTextField.gridwidth = 2;
		gbc_hoursTextField.insets = new Insets(0, 0, 5, 5);
		gbc_hoursTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_hoursTextField.gridx = 0;
		gbc_hoursTextField.gridy = 1;
		add(hoursTextField, gbc_hoursTextField);
		hoursTextField.setColumns(10);

		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("lblMinutes"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 3, 5, 5);
		gbc_lblNewLabel_1.gridx = 2;
		gbc_lblNewLabel_1.gridy = 0;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		minutesTextField = new JTextField();
		GridBagConstraints gbc_minutesTextField = new GridBagConstraints();
		gbc_minutesTextField.gridwidth = 2;
		gbc_minutesTextField.insets = new Insets(0, 0, 5, 5);
		gbc_minutesTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_minutesTextField.gridx = 2;
		gbc_minutesTextField.gridy = 1;
		add(minutesTextField, gbc_minutesTextField);
		minutesTextField.setColumns(10);

		secondsTextField = new JTextField();
		GridBagConstraints gbc_secondsTextField = new GridBagConstraints();
		gbc_secondsTextField.gridwidth = 2;
		gbc_secondsTextField.insets = new Insets(0, 0, 5, 5);
		gbc_secondsTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_secondsTextField.gridx = 4;
		gbc_secondsTextField.gridy = 1;
		add(secondsTextField, gbc_secondsTextField);
		secondsTextField.setColumns(10);

		daysTextField = new JTextField();
		GridBagConstraints gbc_daysTextField = new GridBagConstraints();
		gbc_daysTextField.gridwidth = 2;
		gbc_daysTextField.insets = new Insets(0, 0, 5, 5);
		gbc_daysTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_daysTextField.gridx = 6;
		gbc_daysTextField.gridy = 1;
		add(daysTextField, gbc_daysTextField);
		daysTextField.setColumns(10);

		daysOfWeekPanel = daysOfWeekPanel();
		GridBagConstraints gbc_daysOfWeek = new GridBagConstraints();
		gbc_daysOfWeek.anchor = GridBagConstraints.WEST;
		gbc_daysOfWeek.gridheight = 2;
		gbc_daysOfWeek.insets = new Insets(0, 5, 5, 0);
		gbc_daysOfWeek.fill = GridBagConstraints.HORIZONTAL;
		gbc_daysOfWeek.gridx = 8;
		gbc_daysOfWeek.gridy = 0;
		add(daysOfWeekPanel, gbc_daysOfWeek);
		
		JButton allWDaysButton = new JButton(new UsnaAction(null, "lblAddAll", "/images/Ok14.png", e -> checkBoxSelect(daysOfWeekPanel, true)));
		allWDaysButton.setContentAreaFilled(false);
		allWDaysButton.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_allWDaysButton = new GridBagConstraints();
		gbc_allWDaysButton.anchor = GridBagConstraints.SOUTH;
		gbc_allWDaysButton.gridx = 9;
		gbc_allWDaysButton.gridy = 0;
		add(allWDaysButton, gbc_allWDaysButton);
		
		JButton noWDaysButton = new JButton(new UsnaAction(null, "lblRemoveAll", "/images/PlayerStop14.png", e -> checkBoxSelect(daysOfWeekPanel, false)));
		noWDaysButton.setContentAreaFilled(false);
		noWDaysButton.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_noWDaysButton = new GridBagConstraints();
		gbc_noWDaysButton.insets = new Insets(0, 0, 5, 0);
		gbc_noWDaysButton.gridx = 9;
		gbc_noWDaysButton.gridy = 1;
		add(noWDaysButton, gbc_noWDaysButton);
		
		monthsPanel = monthsPanel();
		GridBagConstraints gbc_months = new GridBagConstraints();
		gbc_months.anchor = GridBagConstraints.WEST;
		gbc_months.gridheight = 2;
		gbc_months.insets = new Insets(0, 12, 5, 5);
		gbc_months.fill = GridBagConstraints.HORIZONTAL;
		gbc_months.gridx = 10;
		gbc_months.gridy = 0;
		add(monthsPanel, gbc_months);
		
		JButton allMonthsButton = new JButton(new UsnaAction(null, "lblAddAll", "/images/Ok14.png", e -> checkBoxSelect(monthsPanel, true)));
		allMonthsButton.setContentAreaFilled(false);
		allMonthsButton.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.anchor = GridBagConstraints.SOUTH;
		gbc_btnNewButton.gridx = 11;
		gbc_btnNewButton.gridy = 0;
		add(allMonthsButton, gbc_btnNewButton);
		
		JButton noMonthsButton = new JButton(new UsnaAction(null, "lblRemoveAll", "/images/PlayerStop14.png", e -> checkBoxSelect(monthsPanel, false)));
		noMonthsButton.setContentAreaFilled(false);
		noMonthsButton.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_noMonthsButton = new GridBagConstraints();
		gbc_noMonthsButton.insets = new Insets(0, 0, 5, 0);
		gbc_noMonthsButton.gridx = 11;
		gbc_noMonthsButton.gridy = 1;
		add(noMonthsButton, gbc_noMonthsButton);

		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		radioPanel.setOpaque(false);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 0, 5);
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.gridwidth = 7;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 2;
		add(radioPanel, gbc_panel);
		
		ItemListener changeType = e -> {
			if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
				if(e.getSource() == cronRadio) {
					secondsTextField.setEnabled(true);
					btnSecondsSelec.setEnabled(true);
				} else {
					secondsTextField.setEnabled(false);
					btnSecondsSelec.setEnabled(false);
					secondsTextField.setText("0");
					if(CronUtils.HOUR_0_23_PATTERN.matcher(hoursTextField.getText()).matches() == false) {
						hoursTextField.setText("0");
						hoursTextField.setForeground(null);
					}
					if(CronUtils.MINUTE_0_59_PATTERN.matcher(minutesTextField.getText()).matches() == false) {
						minutesTextField.setText("0");
						minutesTextField.setForeground(null);
					}
				}
				generateExpression();
			}
		};

		cronRadio = new JRadioButton(LABELS.getString("lblTime"));
		cronRadio.setOpaque(false);
		cronRadio.addItemListener(changeType);
		radioPanel.add(cronRadio);

		beforeRiseRadio = new JRadioButton(LABELS.getString("lblBeforeSunrise"));
		beforeRiseRadio.setOpaque(false);
		beforeRiseRadio.addItemListener(changeType);
		radioPanel.add(beforeRiseRadio);

		afterRiseRadio = new JRadioButton(LABELS.getString("lblAfterSunrise"));
		afterRiseRadio.setOpaque(false);
		afterRiseRadio.addItemListener(changeType);
		radioPanel.add(afterRiseRadio);

		beforeSetRadio = new JRadioButton(LABELS.getString("lblBeforeSunset"));
		beforeSetRadio.setOpaque(false);
		beforeSetRadio.addItemListener(changeType);
		radioPanel.add(beforeSetRadio);

		afterSetRadio = new JRadioButton(LABELS.getString("lblAfterSunset"));
		afterSetRadio.setOpaque(false);
		afterSetRadio.addItemListener(changeType);
		radioPanel.add(afterSetRadio);

		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(cronRadio);
		modeGroup.add(beforeRiseRadio);
		modeGroup.add(afterRiseRadio);
		modeGroup.add(beforeSetRadio);
		modeGroup.add(afterSetRadio);

		expressionField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 3;
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 8;
		gbc_textField.gridy = 2;
		add(expressionField, gbc_textField);

		expressionField.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				String exp = CronUtils.fragStrToNum(expressionField.getText());
				expressionField.setText(exp);
				if(hoursTextField.isEnabled() == false) {
					enableEdit(AbstractCronPanel.this, true);
				}
				if((CronUtils.CRON_PATTERN.matcher(exp).matches() || CronUtils.SUNSET_PATTERN.matcher(exp).matches())) {
					expressionField.setForeground(null);
					setCron(exp);
//				} else if(CronUtils.RANDOM_PATTERN.matcher(exp).matches()) {
//					enableEdit(AbstractCronPanel.this, false);
//					expressionField.setEnabled(true);
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
				generateExpression();
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
	}

	private JPanel monthsPanel() {
		JPanel mPanel = new JPanel();
		mPanel.setOpaque(false);
		ItemListener change = e -> {
			computeMonths();
			generateExpression();
		};
		mPanel.setLayout(new GridLayout(1, 0, 0, 0));
		for (Month m : Month.values()) {
			JCheckBox chckbxNewCheckBox = new JCheckBox(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
			chckbxNewCheckBox.setOpaque(false);
			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
			chckbxNewCheckBox.addItemListener(change);
			mPanel.add(chckbxNewCheckBox);
		}
		return mPanel;
	}

	private void computeMonths() {
		List<Integer> seq = new ArrayList<>();
		for (int i = 0; i < monthsPanel.getComponentCount(); i++) {
			if (((JCheckBox) monthsPanel.getComponent(i)).isSelected()) {
				seq.add(i + 1);
			}
		}
		months = (seq.size() == 12 || seq.isEmpty()) ? "*" : CronUtils.listAsCronString(seq);
	}

	private JPanel daysOfWeekPanel() {
		JPanel wPanel = new JPanel();
		wPanel.setOpaque(false);
		ItemListener change = e -> {
			computeDaysOfWeek();
			generateExpression();
		};
		wPanel.setLayout(new GridLayout(1, 0, 0, 0));
		for (DayOfWeek d : DayOfWeek.values()) {
			JCheckBox chckbxNewCheckBox = new JCheckBox(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
			chckbxNewCheckBox.setOpaque(false);
			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
			chckbxNewCheckBox.addItemListener(change);
			wPanel.add(chckbxNewCheckBox);
		}
		return wPanel;
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
//		daysOfWeek = (seq.size() == 7 || seq.isEmpty()) ? "*" : CronUtils.listAsCronString(seq);
		// the app do not understand intervals here; the web UI do (!!!)
		daysOfWeek = (seq.size() == 7 || seq.isEmpty()) ? "*" : seq.stream().map(num -> num + "").collect(Collectors.joining(","));

	}

	// assume cronLine is correct
	public void setCron(String cronLine) {
		cronLine = CronUtils.fragStrToNum(cronLine);
//		if(cronLine.startsWith("@random") == false) {
			final String[] values;
			Matcher sm = CronUtils.SUNSET_PATTERN.matcher(cronLine);
			if(sm.matches()) {
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
				enableEdit(monthsPanel, false);
			} else {
				if(months.equals("*")) {
					months ="1-12";
				}
				for(int month: CronUtils.fragmentToInt(months)) {
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
				enableEdit(daysOfWeekPanel, false);
			} else {
				if(daysOfWeek.equals("*")) {
					daysOfWeek ="0-6";
				}
				for(int weekDay: CronUtils.fragmentToInt(daysOfWeek)) {
					((JCheckBox) daysOfWeekPanel.getComponent((weekDay + 6) % 7)).setSelected(true);
				}
				computeDaysOfWeek();
			}
			generateExpression();
//		} else {
//			expressionField.setText(cronLine);
//		}
	}
	
	private static void checkBoxSelect(JPanel p, boolean select) {
		for(int i = 0; i < p.getComponentCount(); i++) {
			((JCheckBox) p.getComponent(i)).setSelected(select);
		}
	}
	
	protected static void enableEdit(JPanel panel, boolean enable) {
		for(Component c: panel.getComponents()) {
			if(c instanceof JPanel p) {
				enableEdit(p, enable);
			} else {
				c.setEnabled(enable);
			}
		}
	}

	private void generateExpression() {
		String res = "";
		String minutes = minutesTextField.getText();
		String hours = hoursTextField.getText();
		if(cronRadio.isSelected()) {
			String seconds = secondsTextField.getText();
			res = seconds + " " + minutes + " " + hours;
			secondsTextField.setForeground(CronUtils.SECONDS_PATTERN.matcher(seconds).matches() ? null : Color.red);
			minutesTextField.setForeground(CronUtils.MINUTES_PATTERN.matcher(minutes).matches() ? null : Color.red);
			hoursTextField.setForeground(CronUtils.HOURS_PATTERN.matcher(hours).matches() ? null : Color.red);
		} else if(beforeRiseRadio.isSelected()) {
			res = "@sunrise-" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		} else if(afterRiseRadio.isSelected()) {
			res = "@sunrise+" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		} else if(beforeSetRadio.isSelected()) {
			res = "@sunset-" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		} else if(afterSetRadio.isSelected()) {
			res = "@sunset+" + hours + "h" + minutes + "m";
			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
		}

		String days = daysTextField.getText();
		daysTextField.setForeground(CronUtils.DAYS_PATTERN.matcher(days).matches() ? null : Color.red);
		res += " " + days + " " + months + " " + daysOfWeek;

		expressionField.setText(res);
		expressionField.setForeground((CronUtils.CRON_PATTERN.matcher(res).matches() || CronUtils.SUNSET_PATTERN.matcher(res).matches()) ? null : Color.red);
	}
	
	protected boolean validateData() {
		String exp = expressionField.getText();
		if(CronUtils.CRON_PATTERN.matcher(exp).matches() == false && CronUtils.SUNSET_PATTERN.matcher(exp).matches() == false /*&& CronUtils.RANDOM_PATTERN.matcher(exp).matches() == false*/) {
			expressionField.requestFocus();
			Msg.errorMsg(parentDlg, "schErrorInvalidExpression");
			return false;
		} else {
			return true;
		}
	}
}