package it.usna.shellyscan.view.scheduler.walldisplay;

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
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.view.scheduler.CronUtils;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.NumericTextField;

class ThermJobPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final static String SECONDS = "*";
	private final static String DAYS = "*";
	private final static String MONTHS = "*";
	private float minTarget, maxTarget;
	private NumericTextField<Float> target;
	
	private NumericTextField<Integer> hoursTextField = new NumericTextField<>(0, 0, 23);
	private NumericTextField<Integer> minutesTextField = new NumericTextField<>(0, 0, 59);
	private JPanel daysOfWeekPanel;
	private String daysOfWeek;
	protected JTextField expressionField;
	protected JDialog parent;
	
	private final static String DEF_CRON = "* 0 0 * * 0,1,2,3,4,5,6";

	ThermJobPanel(JDialog parent, float minTarget, float maxTarget, String timespec, Float temp) {
		this.parent = parent;
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2));
		initCronSection();
		
		this.minTarget = minTarget;
		this.maxTarget = maxTarget;
		initTempSection();
		if(timespec == null) {
			setCron(DEF_CRON);
			target.setValue(null);
		} else {
			setCron(timespec);
			target.setValue(temp);
		}
	}

	private void initTempSection() {
		target = new NumericTextField<Float>(minTarget, maxTarget, Locale.ENGLISH);
		target.allowNull(true);
		target.setColumns(4);
		target.setMaximumFractionDigits(1);
		target.setLimits(minTarget, maxTarget);

		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 2;
		add(new JLabel(LABELS.getString("lblTargetTemp")), gbc_lblNewLabel_5);

		GridBagConstraints gbc_target = new GridBagConstraints();
		gbc_target.anchor = GridBagConstraints.WEST;
		gbc_target.gridx = 1;
		gbc_target.gridy = 2;
		gbc_target.gridwidth = 3;
		add(target, gbc_target);
	}

	public void clean() {
		setCron(DEF_CRON);
		target.setValue(null);
	}

	public boolean isNullJob() {
		return expressionField.getText().equals(DEF_CRON) && target.getText().isBlank();
	}

	public boolean validateData() {
		if(validateCronData()) {
			if(target.isEmpty()) {
				target.requestFocus();
				Msg.errorMsg(parent, "schErrorInvalidTarget");
				return false;
			}
			return true;
		}
		return false;
	}
	
	public String getTimespec() {
		return expressionField.getText();
	}
	
	public float getTarget() {
		return target.getFloatValue();
	}
	
	public void setTarget(float temp) {
		target.setValue(temp);
	}

	public ObjectNode getJson() {
		final ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("timespec", expressionField.getText());
		if(target.isEmpty() == false) {
			out.put("target_C", target.getFloatValue());
		}
		return out;
	}
	
	private void initCronSection() {
		GridBagLayout gbl_panel = new GridBagLayout();
		// gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 10, 0, 10};
		// gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
//		gbl_panel.columnWeights = new double[] { 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0/*, 0.0, 0.0, 0.0, 0.0, 0.0 */};
//		gbl_panel.rowWeights = new double[] { 1.0, 1.0, 1.0};
		setLayout(gbl_panel);

		JLabel lblNewLabel = new JLabel(LABELS.getString("lblHours"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 3, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);

		GridBagConstraints gbc_hoursTextField = new GridBagConstraints();
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
		gbc_lblNewLabel_1.gridx = 1;
		gbc_lblNewLabel_1.gridy = 0;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		GridBagConstraints gbc_minutesTextField = new GridBagConstraints();
		gbc_minutesTextField.insets = new Insets(0, 0, 5, 5);
		gbc_minutesTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_minutesTextField.gridx = 1;
		gbc_minutesTextField.gridy = 1;
		add(minutesTextField, gbc_minutesTextField);
		minutesTextField.setColumns(10);

		daysOfWeekPanel = daysOfWeekPanel();
		GridBagConstraints gbc_daysOfWeek = new GridBagConstraints();
		gbc_daysOfWeek.anchor = GridBagConstraints.WEST;
		gbc_daysOfWeek.gridheight = 2;
		gbc_daysOfWeek.insets = new Insets(0, 15, 5, 8);
		gbc_daysOfWeek.fill = GridBagConstraints.HORIZONTAL;
		gbc_daysOfWeek.gridx = 2;
		gbc_daysOfWeek.gridy = 0;
		add(daysOfWeekPanel, gbc_daysOfWeek);
		
		JButton allWDaysButton = new JButton(new UsnaAction(null, "lblAddAll", "/images/Ok14.png", e -> checkBoxSelect(daysOfWeekPanel, true)));
		allWDaysButton.setContentAreaFilled(false);
		allWDaysButton.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_allWDaysButton = new GridBagConstraints();
		gbc_allWDaysButton.insets = new Insets(0, 0, 5, 0);
		gbc_allWDaysButton.anchor = GridBagConstraints.SOUTHWEST;
		gbc_allWDaysButton.gridx = 3;
		gbc_allWDaysButton.gridy = 0;
		add(allWDaysButton, gbc_allWDaysButton);
		
		JButton noWDaysButton = new JButton(new UsnaAction(null, "lblRemoveAll", "/images/PlayerStop14.png", e -> checkBoxSelect(daysOfWeekPanel, false)));
		noWDaysButton.setContentAreaFilled(false);
		noWDaysButton.setBorder(BorderFactory.createEmptyBorder());
		GridBagConstraints gbc_noWDaysButton = new GridBagConstraints();
		gbc_noWDaysButton.anchor = GridBagConstraints.WEST;
		gbc_noWDaysButton.insets = new Insets(0, 0, 5, 0);
		gbc_noWDaysButton.gridx = 3;
		gbc_noWDaysButton.gridy = 1;
		add(noWDaysButton, gbc_noWDaysButton);

		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		radioPanel.setOpaque(false);
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 0, 5);
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.gridwidth = 2;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 2;
		add(radioPanel, gbc_panel);

		expressionField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.weightx = 1.0;
		gbc_textField.gridwidth = 2;
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 2;
		gbc_textField.gridy = 2;
		add(expressionField, gbc_textField);

		expressionField.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				String exp = CronUtils.fragStrToNum(expressionField.getText());
				expressionField.setText(exp);
				if(CronUtils.CRON_PATTERN_TH_WD.matcher(exp).matches()) {
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
			@SuppressWarnings("unchecked")
			@Override
			public void focusLost(FocusEvent e) {
				if(((NumericTextField<Integer>)e.getComponent()).isEditValid()) {
					generateExpression();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				e.getComponent().setForeground(null); // default color
			}
		};

		hoursTextField.addFocusListener(fragmentsFocusListener);
		minutesTextField.addFocusListener(fragmentsFocusListener);
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
		// the app do not undertand intervals here; the web UI do (!!!)
		daysOfWeek = (seq.isEmpty()) ? "0,1,2,3,4,5,6" : seq.stream().map(num -> num + "").collect(Collectors.joining(","));

	}

	// assume cronLine is correct
	public void setCron(String cronLine) {
		cronLine = CronUtils.fragStrToNum(cronLine);
		final String[] values = cronLine.split(" ");

		minutesTextField.setText(values[1]);
		hoursTextField.setText(values[2]);

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
	}
	
	private static void checkBoxSelect(JPanel p, boolean select) {
		for(int i = 0; i < p.getComponentCount(); i++) {
			((JCheckBox) p.getComponent(i)).setSelected(select);
		}
	}
	
	private static void enableEdit(JPanel panel, boolean enable) {
		for(Component c: panel.getComponents()) {
			if(c instanceof JPanel p) {
				enableEdit(p, enable);
			} else {
				c.setEnabled(enable);
			}
		}
	}

	private void generateExpression() {
		String minutes = minutesTextField.getText();
		String hours = hoursTextField.getText();
		String res = SECONDS + " " + minutes + " " + hours + " " + DAYS + " " + MONTHS + " " + daysOfWeek;
		expressionField.setText(res);
		expressionField.setForeground((CronUtils.CRON_PATTERN_TH_WD.matcher(res).matches()) ? null : Color.red);
	}
	
	// probably to be removed for a new WD specific panel
	public String getExtTimespec() {
		String minutes = minutesTextField.getText();
		String hours = hoursTextField.getText();
		return "* " + minutes + " " + hours + " * * " + CronUtils.daysOfWeekAsString(daysOfWeek);
	}
	
	private boolean validateCronData() {
		String exp = expressionField.getText();
		if(CronUtils.CRON_PATTERN_TH_WD.matcher(exp).matches() == false && CronUtils.SUNSET_PATTERN.matcher(exp).matches() == false) {
			expressionField.requestFocus();
			Msg.errorMsg(parent, "schErrorInvalidExpression");
			return false;
		} else {
			return true;
		}
	}
}