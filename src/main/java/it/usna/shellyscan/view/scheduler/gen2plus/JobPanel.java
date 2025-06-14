package it.usna.shellyscan.view.scheduler.gen2plus;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.controller.UsnaDropdownAction;
import it.usna.shellyscan.view.scheduler.AbstractCronPanel;
import it.usna.shellyscan.view.scheduler.CronUtils;
import it.usna.shellyscan.view.util.Msg;

public class JobPanel extends AbstractCronPanel {
	private static final long serialVersionUID = 1L;
//	private final static ImageIcon EDIT_IMG = new ImageIcon(JobPanel.class.getResource("/images/Write16.png"));
//	private JTextField hoursTextField;
//	private JTextField minutesTextField;
//	private JTextField secondsTextField;
//	private JTextField daysTextField;
//	private JPanel monthsPanel;
//	private JPanel daysOfWeekPanel;
//	private String daysOfWeek;
//	private String months;
//	private JTextField expressionField;
//	private JRadioButton cronRadio;
//	private JRadioButton beforeRiseRadio;
//	private JRadioButton afterRiseRadio;
//	private JRadioButton beforeSetRadio;
//	private JRadioButton afterSetRadio;
	private JPanel callsPanel;
	private JPanel callsParameterPanel;
	private JPanel callsOperationsPanel;
//	private JDialog parent;
	
	private boolean systemJob = false;

	private final MethodHints mHints;// = new MethodHints();
//	private final static String DEF_CRON = "0 0 * * * *";
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

	/**
	 * @wbp.parser.constructor
	 */
	JobPanel(JDialog parent, JsonNode scheduleNode, MethodHints mHints) {
		super(parent);
		initCallSection();
//		this.parent = parent;
		this.mHints = mHints;
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
//		initCronSection();
		if(scheduleNode == null) {
			setCron(DEF_CRON);
			addCall("", "", 0);
		} else {
			setCron(scheduleNode.path("timespec").asText());
			setCalls(scheduleNode.path("calls"));
		}
	}

	public void setCalls(JsonNode calls) {
		int iniIdx = callsPanel.getComponentCount();
		if(iniIdx > 0 && calls.size() > 0 && ((JTextField)callsPanel.getComponent(iniIdx - 1)).getText().isEmpty() && ((JTextField)callsParameterPanel.getComponent(iniIdx - 1)).getText().isEmpty()) {
			iniIdx--;
			callsPanel.remove(iniIdx);
			callsParameterPanel.remove(iniIdx);
			callsOperationsPanel.remove(iniIdx);
		}
		Iterator<JsonNode> callsIt = calls.iterator();
		for(int i = iniIdx; callsIt.hasNext(); i++) {
			JsonNode call = callsIt.next();
			String params = call.path("params").toString();
			if(call.hasNonNull("origin")) {
				systemJob = true;
			}
			addCall(call.path("method").asText(), params.isEmpty() ? "" :  params.substring(1, params.length() - 1), i);
		}
		if(systemJob) {
			enableEdit(callsPanel, false);
			enableEdit(callsParameterPanel, false);
			enableEdit(callsOperationsPanel, false);
		}
	}

	private void addCall(String method, String params/*, String origin*/, int index) {
		JTextField methodTF = new JTextField(method);
		JTextField paramsTF = new JTextField(params);
		callsPanel.add(methodTF, index);
		callsParameterPanel.add(paramsTF, index);

		JPanel callOpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		callOpPanel.setOpaque(false);
		JButton addB = new JButton(new ImageIcon(getClass().getResource("/images/plus_transp16.png")));
		addB.setContentAreaFilled(false);
		addB.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		addB.addActionListener(e ->  {
			Component[] list = callsOperationsPanel.getComponents();
			int i;
			for(i = 0; list[i] != callOpPanel; i++);
			addCall("", "", i + 1);
			callsOperationsPanel.revalidate();
		});
		callOpPanel.add(addB);
		JButton minusB = new JButton(new ImageIcon(getClass().getResource("/images/erase-9-16.png")));
		minusB.setContentAreaFilled(false);
		minusB.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		minusB.addActionListener(e ->  {
			Component[] list = callsOperationsPanel.getComponents();
			if(list.length > 1) {
				int i;
				for(i = 0; list[i] != callOpPanel; i++);
				callsPanel.remove(i);
				callsParameterPanel.remove(i);
				callsOperationsPanel.remove(i);
				callsOperationsPanel.revalidate();
			}
		});
		callOpPanel.add(minusB);

		JButton btnSelectCombo = new JButton();
		btnSelectCombo.setAction(new UsnaDropdownAction(btnSelectCombo, "/images/expand-more.png", "lblMethodSelect", () -> {
			try {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				return mHints.get(methodTF, paramsTF);
			} finally {
				this.setCursor(Cursor.getDefaultCursor());
			}
		}));
		btnSelectCombo.setContentAreaFilled(false);
		btnSelectCombo.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 3));
		callOpPanel.add(btnSelectCombo);

		callsOperationsPanel.add(callOpPanel, index);
	}

	public void clean() {
		callsPanel.removeAll();
		callsParameterPanel.removeAll();
		callsOperationsPanel.removeAll();
		setCron(DEF_CRON);
		addCall("", "", 0);
	}

//	private void initCronSection() {
//		GridBagLayout gbl_panel = new GridBagLayout();
//		//		gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 10, 0, 10};
//		// gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
//		gbl_panel.columnWeights = new double[] { 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.5, 1.0, 0.0 };
//		gbl_panel.rowWeights = new double[] { 1.0, 1.0, 1.0, 0.0, 1.0};
//		setLayout(gbl_panel);
//
//		JLabel lblNewLabel = new JLabel(LABELS.getString("lblHours"));
//		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
//		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
//		gbc_lblNewLabel.insets = new Insets(0, 3, 5, 5);
//		gbc_lblNewLabel.gridx = 0;
//		gbc_lblNewLabel.gridy = 0;
//		add(lblNewLabel, gbc_lblNewLabel);
//
//		JButton btnHoursSelec = new JButton(EDIT_IMG);
//		btnHoursSelec.setContentAreaFilled(false);
//		btnHoursSelec.setBorder(BorderFactory.createEmptyBorder());
//		GridBagConstraints gbc_btnHoursSelec = new GridBagConstraints();
//		gbc_btnHoursSelec.insets = new Insets(1, 0, 1, 8);
//		gbc_btnHoursSelec.gridx = 1;
//		gbc_btnHoursSelec.gridy = 0;
//		add(btnHoursSelec, gbc_btnHoursSelec);
//		btnHoursSelec.addActionListener(e -> new CronValuesDialog(parent, hoursTextField, 0, 24));
//
//		JButton btnMinutesSelec = new JButton(EDIT_IMG);
//		btnMinutesSelec.setContentAreaFilled(false);
//		btnMinutesSelec.setBorder(BorderFactory.createEmptyBorder());
//		GridBagConstraints gbc_btnMinutesSelec = new GridBagConstraints();
//		gbc_btnMinutesSelec.insets = new Insets(1, 0, 1, 8);
//		gbc_btnMinutesSelec.gridx = 3;
//		gbc_btnMinutesSelec.gridy = 0;
//		add(btnMinutesSelec, gbc_btnMinutesSelec);
//		btnMinutesSelec.addActionListener(e -> new CronValuesDialog(parent, minutesTextField, 0, 60));
//
//		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("lblSeconds"));
//		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
//		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
//		gbc_lblNewLabel_2.insets = new Insets(0, 3, 5, 5);
//		gbc_lblNewLabel_2.gridx = 4;
//		gbc_lblNewLabel_2.gridy = 0;
//		add(lblNewLabel_2, gbc_lblNewLabel_2);
//
//		JButton btnSecondsSelec = new JButton(EDIT_IMG);
//		btnSecondsSelec.setContentAreaFilled(false);
//		btnSecondsSelec.setBorder(BorderFactory.createEmptyBorder());
//		GridBagConstraints gbc_btnSecondsSelec = new GridBagConstraints();
//		gbc_btnSecondsSelec.insets = new Insets(1, 0, 1, 8);
//		gbc_btnSecondsSelec.gridx = 5;
//		gbc_btnSecondsSelec.gridy = 0;
//		add(btnSecondsSelec, gbc_btnSecondsSelec);
//		btnSecondsSelec.addActionListener(e -> new CronValuesDialog(parent, secondsTextField, 0, 60));
//
//		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("lblDays"));
//		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
//		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
//		gbc_lblNewLabel_3.insets = new Insets(0, 3, 5, 5);
//		gbc_lblNewLabel_3.gridx = 6;
//		gbc_lblNewLabel_3.gridy = 0;
//		add(lblNewLabel_3, gbc_lblNewLabel_3);
//
//		JButton btnDaysSelec = new JButton(EDIT_IMG);
//		btnDaysSelec.setContentAreaFilled(false);
//		btnDaysSelec.setBorder(BorderFactory.createEmptyBorder());
//		GridBagConstraints gbc_btnDaysSelec = new GridBagConstraints();
//		gbc_btnDaysSelec.insets = new Insets(1, 0, 1, 8);
//		gbc_btnDaysSelec.gridx = 7;
//		gbc_btnDaysSelec.gridy = 0;
//		add(btnDaysSelec, gbc_btnDaysSelec);
//		btnDaysSelec.addActionListener(e -> new CronValuesDialog(parent, daysTextField, 1, 32));
//
//		hoursTextField = new JTextField();
//		GridBagConstraints gbc_hoursTextField = new GridBagConstraints();
//		gbc_hoursTextField.gridwidth = 2;
//		gbc_hoursTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_hoursTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_hoursTextField.gridx = 0;
//		gbc_hoursTextField.gridy = 1;
//		add(hoursTextField, gbc_hoursTextField);
//		hoursTextField.setColumns(10);
//
//		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("lblMinutes"));
//		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
//		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
//		gbc_lblNewLabel_1.insets = new Insets(0, 3, 5, 5);
//		gbc_lblNewLabel_1.gridx = 2;
//		gbc_lblNewLabel_1.gridy = 0;
//		add(lblNewLabel_1, gbc_lblNewLabel_1);
//
//		minutesTextField = new JTextField();
//		GridBagConstraints gbc_minutesTextField = new GridBagConstraints();
//		gbc_minutesTextField.gridwidth = 2;
//		gbc_minutesTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_minutesTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_minutesTextField.gridx = 2;
//		gbc_minutesTextField.gridy = 1;
//		add(minutesTextField, gbc_minutesTextField);
//		minutesTextField.setColumns(10);
//
//		secondsTextField = new JTextField();
//		GridBagConstraints gbc_secondsTextField = new GridBagConstraints();
//		gbc_secondsTextField.gridwidth = 2;
//		gbc_secondsTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_secondsTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_secondsTextField.gridx = 4;
//		gbc_secondsTextField.gridy = 1;
//		add(secondsTextField, gbc_secondsTextField);
//		secondsTextField.setColumns(10);
//
//		daysTextField = new JTextField();
//		GridBagConstraints gbc_daysTextField = new GridBagConstraints();
//		gbc_daysTextField.gridwidth = 2;
//		gbc_daysTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_daysTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_daysTextField.gridx = 6;
//		gbc_daysTextField.gridy = 1;
//		add(daysTextField, gbc_daysTextField);
//		daysTextField.setColumns(10);
//
//		monthsPanel = monthsPanel();
//		GridBagConstraints gbc_months = new GridBagConstraints();
//		gbc_months.anchor = GridBagConstraints.WEST;
//		gbc_months.gridheight = 2;
//		//		gbc_months.insets = new Insets(0, 0, 5, 5);
//		gbc_months.fill = GridBagConstraints.HORIZONTAL;
//		gbc_months.gridx = 9;
//		gbc_months.gridy = 0;
//		add(monthsPanel, gbc_months);
//
//		daysOfWeekPanel = daysOfWeekPanel();
//		GridBagConstraints gbc_daysOfWeek = new GridBagConstraints();
//		gbc_daysOfWeek.anchor = GridBagConstraints.WEST;
//		gbc_daysOfWeek.gridheight = 2;
//		gbc_daysOfWeek.insets = new Insets(0, 5, 5, 15);
//		gbc_daysOfWeek.fill = GridBagConstraints.HORIZONTAL;
//		gbc_daysOfWeek.gridx = 8;
//		gbc_daysOfWeek.gridy = 0;
//		add(daysOfWeekPanel, gbc_daysOfWeek);
//
//		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//		radioPanel.setOpaque(false);
//		GridBagConstraints gbc_panel = new GridBagConstraints();
//		gbc_panel.gridwidth = 7;
//		//		gbc_panel.insets = new Insets(0, 0, 5, 5);
//		gbc_panel.gridx = 0;
//		gbc_panel.gridy = 2;
//		add(radioPanel, gbc_panel);
//		
//		ItemListener changeType = e -> {
//			if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
//				if(e.getSource() == cronRadio) {
//					secondsTextField.setEnabled(true);
//					btnSecondsSelec.setEnabled(true);
//				} else {
//					secondsTextField.setEnabled(false);
//					btnSecondsSelec.setEnabled(false);
//					secondsTextField.setText("0");
//					if(CronUtils.HOUR_0_23_PATTERN.matcher(hoursTextField.getText()).matches() == false) {
//						hoursTextField.setText("0");
//						hoursTextField.setForeground(null);
//					}
//					if(CronUtils.MINUTE_0_59_PATTERN.matcher(minutesTextField.getText()).matches() == false) {
//						minutesTextField.setText("0");
//						minutesTextField.setForeground(null);
//					}
//				}
//				generateExpression();
//			}
//		};
//
//		cronRadio = new JRadioButton(LABELS.getString("lblTime"));
//		cronRadio.setOpaque(false);
//		cronRadio.addItemListener(changeType);
//		radioPanel.add(cronRadio);
//
//		beforeRiseRadio = new JRadioButton(LABELS.getString("lblBeforeSunrise"));
//		beforeRiseRadio.setOpaque(false);
//		beforeRiseRadio.addItemListener(changeType);
//		radioPanel.add(beforeRiseRadio);
//
//		afterRiseRadio = new JRadioButton(LABELS.getString("lblAfterSunrise"));
//		afterRiseRadio.setOpaque(false);
//		afterRiseRadio.addItemListener(changeType);
//		radioPanel.add(afterRiseRadio);
//
//		beforeSetRadio = new JRadioButton(LABELS.getString("lblBeforeSunset"));
//		beforeSetRadio.setOpaque(false);
//		beforeSetRadio.addItemListener(changeType);
//		radioPanel.add(beforeSetRadio);
//
//		afterSetRadio = new JRadioButton(LABELS.getString("lblAfterSunset"));
//		afterSetRadio.setOpaque(false);
//		afterSetRadio.addItemListener(changeType);
//		radioPanel.add(afterSetRadio);
//
//		ButtonGroup modeGroup = new ButtonGroup();
//		modeGroup.add(cronRadio);
//		modeGroup.add(beforeRiseRadio);
//		modeGroup.add(afterRiseRadio);
//		modeGroup.add(beforeSetRadio);
//		modeGroup.add(afterSetRadio);
//
//		expressionField = new JTextField();
//		GridBagConstraints gbc_textField = new GridBagConstraints();
//		gbc_textField.insets = new Insets(0, 0, 5, 0);
//		gbc_textField.gridwidth = 3;
//		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_textField.gridx = 8;
//		gbc_textField.gridy = 2;
//		add(expressionField, gbc_textField);
//
//		expressionField.addFocusListener(new FocusListener() {
//			@Override
//			public void focusLost(FocusEvent e) {
//				String exp = CronUtils.fragStrToNum(expressionField.getText());
//				expressionField.setText(exp);
//				if((CronUtils.CRON_PATTERN.matcher(exp).matches() || CronUtils.SUNSET_PATTERN.matcher(exp).matches())) {
//					expressionField.setForeground(null);
//					setCron(exp);
//				} else {
//					expressionField.setForeground(Color.red);
//				}
//			}
//
//			@Override
//			public void focusGained(FocusEvent e) {
//				expressionField.setForeground(null); // default color
//			}
//		});
//
//		FocusListener fragmentsFocusListener = new FocusListener() {
//			@Override
//			public void focusLost(FocusEvent e) {
//				JTextField f = (JTextField)e.getComponent();
//				f.setText(f.getText().replaceAll("\\s", ""));
//				if(f.getText().isEmpty()) {
//					f.setText("*");
//				}
//				generateExpression();
//			}
//
//			@Override
//			public void focusGained(FocusEvent e) {
//				e.getComponent().setForeground(null); // default color
//			}
//		};
//
//		hoursTextField.addFocusListener(fragmentsFocusListener);
//		minutesTextField.addFocusListener(fragmentsFocusListener);
//		secondsTextField.addFocusListener(fragmentsFocusListener);
//		daysTextField.addFocusListener(fragmentsFocusListener);
//	}
	
	private void initCallSection() {
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 3;
		add(new JLabel(LABELS.getString("lblMethod")), gbc_lblNewLabel_5);

		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.gridwidth = 2;
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 3;
		gbc_lblNewLabel_6.gridy = 3;
		add(new JLabel(LABELS.getString("lblParameters")), gbc_lblNewLabel_6);
		
		callsPanel = new JPanel();
		GridBagConstraints gbc_callsPanel = new GridBagConstraints();
		gbc_callsPanel.gridwidth = 3;
		gbc_callsPanel.insets = new Insets(0, 0, 0, 5);
		gbc_callsPanel.fill = GridBagConstraints.BOTH;
		gbc_callsPanel.gridx = 0;
		gbc_callsPanel.gridy = 4;
		add(callsPanel, gbc_callsPanel);
		callsPanel.setLayout(new BoxLayout(callsPanel, BoxLayout.Y_AXIS));
		callsPanel.setOpaque(true);
		
		callsParameterPanel = new JPanel();
		GridBagConstraints gbc_callsParameterPanel = new GridBagConstraints();
		gbc_callsParameterPanel.gridwidth = 6;
		gbc_callsParameterPanel.insets = new Insets(0, 0, 0, 5);
		gbc_callsParameterPanel.fill = GridBagConstraints.BOTH;
		gbc_callsParameterPanel.gridx = 3;
		gbc_callsParameterPanel.gridy = 4;
		add(callsParameterPanel, gbc_callsParameterPanel);
		callsParameterPanel.setLayout(new BoxLayout(callsParameterPanel, BoxLayout.Y_AXIS));

		callsOperationsPanel = new JPanel();
		GridBagConstraints gbc_callsOperations = new GridBagConstraints();
		gbc_callsOperations.fill = GridBagConstraints.VERTICAL;
		gbc_callsOperations.anchor = GridBagConstraints.WEST;
		gbc_callsOperations.insets = new Insets(0, 0, 0, 5);
		gbc_callsOperations.gridx = 9;
		gbc_callsOperations.gridy = 4;
		add(callsOperationsPanel, gbc_callsOperations);
		callsOperationsPanel.setOpaque(false);
		//		callsOperationsPanel.setBackground(Color.red);
		callsOperationsPanel.setLayout(new BoxLayout(callsOperationsPanel, BoxLayout.Y_AXIS));
	}

//	private JPanel monthsPanel() {
//		JPanel panel = new JPanel();
//		panel.setOpaque(false);
//		ActionListener change = e -> {
//			computeMonths();
//			generateExpression();
//		};
//		panel.setLayout(new GridLayout(1, 0, 0, 0));
//		for (Month m : Month.values()) {
//			JCheckBox chckbxNewCheckBox = new JCheckBox(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
//			chckbxNewCheckBox.setOpaque(false);
//			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
//			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
//			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
//			chckbxNewCheckBox.addActionListener(change);
//			panel.add(chckbxNewCheckBox);
//		}
//		return panel;
//	}
//
//	private void computeMonths() {
//		List<Integer> seq = new ArrayList<>();
//		for (int i = 0; i < monthsPanel.getComponentCount(); i++) {
//			if (((JCheckBox) monthsPanel.getComponent(i)).isSelected()) {
//				seq.add(i + 1);
//			}
//		}
//		months = (seq.size() == 12 || seq.isEmpty()) ? "*" : CronUtils.listAsCronString(seq);
//	}
//
//	private JPanel daysOfWeekPanel() {
//		JPanel panel = new JPanel();
//		panel.setOpaque(false);
//		ActionListener change = e -> {
//			computeDaysOfWeek();
//			generateExpression();
//		};
//		panel.setLayout(new GridLayout(1, 0, 0, 0));
//		for (DayOfWeek d : DayOfWeek.values()) {
//			JCheckBox chckbxNewCheckBox = new JCheckBox(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
//			chckbxNewCheckBox.setOpaque(false);
//			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
//			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
//			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
//			chckbxNewCheckBox.addActionListener(change);
//			panel.add(chckbxNewCheckBox);
//		}
//		return panel;
//	}
//
//	private void computeDaysOfWeek() {
//		List<Integer> seq = new ArrayList<>();
//		if (((JCheckBox) daysOfWeekPanel.getComponent(6)).isSelected()) { // SUN
//			seq.add(0);
//		}
//		for (int i = 0; i < 6; i++) {
//			if (((JCheckBox) daysOfWeekPanel.getComponent(i)).isSelected()) {
//				seq.add(i + 1);
//			}
//		}
//		daysOfWeek = (seq.size() == 7 || seq.isEmpty()) ? "*" : CronUtils.listAsCronString(seq);
//	}

	// assume cronLine is correct
//	public void setCron(String cronLine) {
//		cronLine = CronUtils.fragStrToNum(cronLine);
//
//		final String[] values;
//		Matcher sm = CronUtils.SUNSET_PATTERN.matcher(cronLine);
//		if(sm.matches()) {
//			String hours = sm.group("HOUR");
//			String minutes = sm.group("MINUTE");
//			String day = sm.group("DAY");
//			String month = sm.group("MONTH");
//			String wday = sm.group("WDAY");
//			values = new String[] {"0", (minutes != null) ? minutes : "0", (hours != null) ? hours : "0", (day != null) ? day : "*", (month != null) ? month : "*", (wday != null) ? wday : "*"};
//			if(cronLine.startsWith("@sunrise+")) {
//				afterRiseRadio.setSelected(true);
//			} else if(cronLine.startsWith("@sunrise-")) {
//				beforeRiseRadio.setSelected(true);
//			} else if(cronLine.startsWith("@sunrise")) {
//				afterRiseRadio.setSelected(true);
//			} else if(cronLine.startsWith("@sunset+")) {
//				afterSetRadio.setSelected(true);
//			} else if(cronLine.startsWith("@sunset-")) {
//				beforeSetRadio.setSelected(true);
//			} else if(cronLine.startsWith("@sunset")) {
//				afterSetRadio.setSelected(true);
//			}
//		} else {
//			cronRadio.setSelected(true);
//			values = cronLine.split(" ");
//		}
//
//		secondsTextField.setText(values[0]);
//		minutesTextField.setText(values[1]);
//		hoursTextField.setText(values[2]);
//		daysTextField.setText(values[3]);
//
//		for(int i = 0; i < 12; i++) {
//			((JCheckBox) monthsPanel.getComponent(i)).setSelected(false);
//			monthsPanel.getComponent(i).setEnabled(true);
//		}
//		String months = values[4];
//		if(months.contains("/")) {
//			this.months = months;
//			enableEdit(monthsPanel, false);
//		} else {
//			if(months.equals("*")) {
//				months ="1-12";
//			}
//			for(int month: CronUtils.fragmentToInt(months)) {
//				((JCheckBox) monthsPanel.getComponent(month - 1)).setSelected(true);
//			}
//			computeMonths();
//		}
//
//		for(int i = 0; i < 7; i++) {
//			((JCheckBox) daysOfWeekPanel.getComponent(i)).setSelected(false);
//			daysOfWeekPanel.getComponent(i).setEnabled(true);
//		}
//		String daysOfWeek = values[5];
//		if(daysOfWeek.contains("/")) {
//			this.daysOfWeek = daysOfWeek;
//			enableEdit(daysOfWeekPanel, false);
//		} else {
//			if(daysOfWeek.equals("*")) {
//				daysOfWeek ="0-6";
//			}
//			for(int weekDay: CronUtils.fragmentToInt(daysOfWeek)) {
//				((JCheckBox) daysOfWeekPanel.getComponent((weekDay + 6) % 7)).setSelected(true);
//			}
//			computeDaysOfWeek();
//		}
//		generateExpression();
//	}
//	
//	private static void enableEdit(JPanel panel, boolean enable) {
//		for(Component c: panel.getComponents()) {
//			if(c instanceof JPanel p) {
//				enableEdit(p, enable);
//			} else {
//				c.setEnabled(enable);
//			}
//		}
//	}

//	private void generateExpression() {
//		String res = "";
//		String minutes = minutesTextField.getText();
//		String hours = hoursTextField.getText();
//		if(cronRadio.isSelected()) {
//			String seconds = secondsTextField.getText();
//			res = seconds + " " + minutes + " " + hours;
//			secondsTextField.setForeground(CronUtils.SECONDS_PATTERN.matcher(seconds).matches() ? null : Color.red);
//			minutesTextField.setForeground(CronUtils.MINUTES_PATTERN.matcher(minutes).matches() ? null : Color.red);
//			hoursTextField.setForeground(CronUtils.HOURS_PATTERN.matcher(hours).matches() ? null : Color.red);
//		} else if(beforeRiseRadio.isSelected()) {
//			res = "@sunrise-" + hours + "h" + minutes + "m";
//			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
//			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
//		} else if(afterRiseRadio.isSelected()) {
//			res = "@sunrise+" + hours + "h" + minutes + "m";
//			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
//			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
//		} else if(beforeSetRadio.isSelected()) {
//			res = "@sunset-" + hours + "h" + minutes + "m";
//			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
//			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
//		} else if(afterSetRadio.isSelected()) {
//			res = "@sunset+" + hours + "h" + minutes + "m";
//			hoursTextField.setForeground(CronUtils.HOUR_0_23_PATTERN.matcher(hours).matches() ? null : Color.red);
//			minutesTextField.setForeground(CronUtils.MINUTE_0_59_PATTERN.matcher(minutes).matches() ? null : Color.red);
//		}
//
//		String days = daysTextField.getText();
//		daysTextField.setForeground(CronUtils.DAYS_PATTERN.matcher(days).matches() ? null : Color.red);
//		res += " " + days + " " + months + " " + daysOfWeek;
//
//		expressionField.setText(res);
//		expressionField.setForeground((CronUtils.CRON_PATTERN.matcher(res).matches() || CronUtils.SUNSET_PATTERN.matcher(res).matches()) ? null : Color.red);
//	}

	public boolean isNullJob() {
		return expressionField.getText().equals(DEF_CRON) &&
				callsPanel.getComponentCount() == 1 &&
				((JTextField)callsPanel.getComponent(0)).getText().isBlank() &&
				((JTextField)callsParameterPanel.getComponent(0)).getText().isBlank();
	}

	public boolean validateData() {
		String exp = expressionField.getText();
		if(CronUtils.CRON_PATTERN.matcher(exp).matches() == false && CronUtils.SUNSET_PATTERN.matcher(exp).matches() == false) {
			expressionField.requestFocus();
			Msg.errorMsg(parent, "schErrorInvalidExpression");
			return false;
		}
		for(int i = 0; i < callsPanel.getComponentCount(); i++) {
			if(((JTextField)callsPanel.getComponent(i)).getText().isBlank()) {
				callsPanel.getComponent(i).requestFocus();
				Msg.errorMsg(parent, "schErrorInvalidMethod");
				return false;
			}
			String parameters = ((JTextField)callsParameterPanel.getComponent(i)).getText();
			if(parameters.isBlank() == false) {
				try {
					JSON_MAPPER.readTree("{" + parameters + "}");
				} catch (JsonProcessingException e) {
					callsParameterPanel.getComponent(i).requestFocus();
					Msg.errorMsg(parent, "schErrorInvalidParameters");
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean hasSystemCalls() {
		return systemJob;
	}

	public ObjectNode getJson() {
		final ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("timespec", expressionField.getText());
		final ArrayNode calls = JsonNodeFactory.instance.arrayNode();
		for(int i = 0; i < callsPanel.getComponentCount(); i++) {
			final ObjectNode call = JsonNodeFactory.instance.objectNode();
			call.put("method", ((JTextField)callsPanel.getComponent(i)).getText());
			String parameters = ((JTextField)callsParameterPanel.getComponent(i)).getText();
			if(parameters.isBlank() == false) {
				try {
					call.set("params", JSON_MAPPER.readTree("{" + parameters + "}"));
				} catch (JsonProcessingException e) {
					Msg.errorMsg(parent, "schErrorInvalidParameters");
					callsParameterPanel.getComponent(i).requestFocus();
					return null;
				}
			}
			calls.add(call);
		}
		out.set("calls", calls);
		return out;
	}
}