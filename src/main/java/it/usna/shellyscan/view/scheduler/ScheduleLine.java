package it.usna.shellyscan.view.scheduler;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import it.usna.swing.texteditor.TextDocumentListener;

public class ScheduleLine extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTextField hoursTextField;
	private JTextField minutesTextField;
	private JTextField secondsTextField;
	private JTextField daysTextField;
	private JPanel monthsPanel;
	private JPanel daysOfWeekPanel;
	private JTextField expressionField;
	private final static String REX_0_59 = "(\\d|[1-5]\\d)";
	private final static String REX_0_23 = "(\\d|1\\d|2[0-3])";
	private final static String REX_1_31 = "([1-9]|[12]\\d|3[01])";
	private final static String REX_1_9999 = "([1-9]\\d{0,3})";
	private Pattern HOURS_PATTERN = Pattern.compile("\\*|" + REX_0_23 + "|\\*/" + REX_1_9999 + "|" + REX_0_23 + "/" + REX_1_9999); // todo verificate 0/5 e 1/5
	private Pattern MINUTES_PATTERN = Pattern.compile("\\*|" + REX_0_59 + "|\\*/" + REX_1_9999 + "|" + REX_0_59 + "/" + REX_1_9999);
	private Pattern SECONDS_PATTERN = Pattern.compile("\\*|" + REX_0_59 + "|\\*/" + REX_1_9999 + "|" + REX_0_59 + "/" + REX_1_9999);
	private Pattern DAYS_PATTERN = Pattern.compile("\\*|" + REX_1_31 + "|\\*/" + REX_1_9999 + "|" + REX_1_31 + "/" + REX_1_9999);

	ScheduleLine(String cronLine) {
		GridBagLayout gbl_panel = new GridBagLayout();
		// gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 1, 10, 10};
		// gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_panel.columnWeights = new double[] { 1.0, 1.0, 1.0, 1.0, 0.5, 1.0 };
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
		hoursTextField.getDocument().addDocumentListener( (TextDocumentListener)event -> asString());

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
		minutesTextField.getDocument().addDocumentListener( (TextDocumentListener)event -> asString());

		secondsTextField = new JTextField();
		GridBagConstraints gbc_secondsTextField = new GridBagConstraints();
		gbc_secondsTextField.insets = new Insets(0, 0, 5, 5);
		gbc_secondsTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_secondsTextField.gridx = 2;
		gbc_secondsTextField.gridy = 1;
		add(secondsTextField, gbc_secondsTextField);
		secondsTextField.setColumns(10);
		secondsTextField.getDocument().addDocumentListener( (TextDocumentListener)event -> asString());

		daysTextField = new JTextField();
		GridBagConstraints gbc_daysTextField = new GridBagConstraints();
		gbc_daysTextField.insets = new Insets(0, 0, 5, 5);
		gbc_daysTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_daysTextField.gridx = 3;
		gbc_daysTextField.gridy = 1;
		add(daysTextField, gbc_daysTextField);
		daysTextField.setColumns(10);
		daysTextField.getDocument().addDocumentListener( (TextDocumentListener)event -> asString());

		daysOfWeekPanel = daysOfWeekPanel();
		GridBagConstraints gbc_daysOfWeek = new GridBagConstraints();
		gbc_daysOfWeek.gridheight = 2;
		gbc_daysOfWeek.insets = new Insets(0, 0, 5, 5);
		gbc_daysOfWeek.fill = GridBagConstraints.BOTH;
		gbc_daysOfWeek.gridx = 4;
		gbc_daysOfWeek.gridy = 0;
		add(daysOfWeekPanel, gbc_daysOfWeek);

		monthsPanel = monthsPanel();
		GridBagConstraints gbc_months = new GridBagConstraints();
		gbc_months.gridheight = 2;
		gbc_months.insets = new Insets(0, 0, 5, 0);
		gbc_months.fill = GridBagConstraints.VERTICAL;
		gbc_months.gridx = 5;
		gbc_months.gridy = 0;
		add(monthsPanel, gbc_months);

		expressionField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 4;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 0;
		gbc_textField.gridy = 2;
		add(expressionField, gbc_textField);
		expressionField.setColumns(10);
		
		String[] values = cronLine.split(" ");
		expressionField.setText(cronLine);
		secondsTextField.setText(values[0]);
		minutesTextField.setText(values[1]);
		hoursTextField.setText(values[2]);
		daysTextField.setText(values[3]);
	}

	private JPanel monthsPanel() {
		JPanel months = new JPanel();
		ActionListener change = e -> asString();
		months.setLayout(new GridLayout(1, 0, 0, 0));
		for (Month m : Month.values()) {
			JCheckBox chckbxNewCheckBox = new JCheckBox(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
			chckbxNewCheckBox.addActionListener(change);
			months.add(chckbxNewCheckBox);
		}
		return months;
	}

	private JPanel daysOfWeekPanel() {
		JPanel days = new JPanel();
		ActionListener change = e -> asString();
		days.setLayout(new GridLayout(1, 0, 0, 0));
		for (DayOfWeek m : DayOfWeek.values()) {
			JCheckBox chckbxNewCheckBox = new JCheckBox(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
			chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
			chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
			chckbxNewCheckBox.addActionListener(change);
			days.add(chckbxNewCheckBox);
		}
		return days;
	}

	public void asString() {
		String seconds = secondsTextField.getText();
		secondsTextField.setForeground(SECONDS_PATTERN.matcher(seconds).matches() ? null : Color.red);
		String res = seconds.isEmpty() ? "*" : seconds;
		
		String minutes = minutesTextField.getText();
		minutesTextField.setForeground(MINUTES_PATTERN.matcher(minutes).matches() ? null : Color.red);
		res += " " + (minutes.isEmpty() ? "*" : minutes);
		
		String hours = hoursTextField.getText();
		hoursTextField.setForeground(HOURS_PATTERN.matcher(hours).matches() ? null : Color.red);
		res += " " + (hours.isEmpty() ? "*" : hours);
		
		String days = daysTextField.getText();
		daysTextField.setForeground(DAYS_PATTERN.matcher(days).matches() ? null : Color.red);
		res += " " + (days.isEmpty() ? "*" : days);
		
		List<Integer> seq = new ArrayList<>();
		for (int i = 0; i < monthsPanel.getComponentCount(); i++) {
			if (((JCheckBox) monthsPanel.getComponent(i)).isSelected()) {
				seq.add(i + 1);
			}
		}
		res += " " + ((seq.size() == monthsPanel.getComponentCount()) ? "*" : listAsCronString(seq));

		seq.clear();
		if (((JCheckBox) daysOfWeekPanel.getComponent(6)).isSelected()) { // SUN
			seq.add(0);
		}
		for (int i = 0; i < 6; i++) {
			if (((JCheckBox) daysOfWeekPanel.getComponent(i)).isSelected()) {
				seq.add(i + 1);
			}
		}
		res += " " + ((seq.size() == daysOfWeekPanel.getComponentCount()) ? "*" : listAsCronString(seq));

		expressionField.setText(res);
//		System.out.println(res);
//		return res;
	}

	private static String listAsCronString(List<Integer> list) {
		if (list.isEmpty()) {
			return "*";
		}
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