package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class SchedulerDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	

	public SchedulerDialog(Window owner) {
		super(owner, "sch", Dialog.ModalityType.APPLICATION_MODAL);
		JPanel mainPanel = new JPanel(new GridLayout(0, 1));
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		JButton remove = new JButton("del");
		panel.add(new ScheduleLine());
		panel.add(remove);
		
		mainPanel.add(panel);
		
		getContentPane().add(mainPanel, BorderLayout.NORTH);
//		GridBagLayout gbl_panel = new GridBagLayout();
////		gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 1, 10, 10};
////		gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
//		gbl_panel.columnWeights = new double[]{1.0, 1.0, 1.0, 1.0, 0.5, 1.0, Double.MIN_VALUE};
//		gbl_panel.rowWeights = new double[]{1.0, 1.0, 0.0, Double.MIN_VALUE};
//		panel.setLayout(gbl_panel);
//		
//		JLabel lblNewLabel = new JLabel("Hours");
//		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
//		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
//		gbc_lblNewLabel.gridx = 0;
//		gbc_lblNewLabel.gridy = 0;
//		panel.add(lblNewLabel, gbc_lblNewLabel);
//		
//		JLabel lblNewLabel_2 = new JLabel("Seconds");
//		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
//		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
//		gbc_lblNewLabel_2.gridx = 2;
//		gbc_lblNewLabel_2.gridy = 0;
//		panel.add(lblNewLabel_2, gbc_lblNewLabel_2);
//		
//		JLabel lblNewLabel_3 = new JLabel("Days");
//		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
//		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
//		gbc_lblNewLabel_3.gridx = 3;
//		gbc_lblNewLabel_3.gridy = 0;
//		panel.add(lblNewLabel_3, gbc_lblNewLabel_3);
//		
//		hoursTextField = new JTextField();
//		GridBagConstraints gbc_hoursTextField = new GridBagConstraints();
//		gbc_hoursTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_hoursTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_hoursTextField.gridx = 0;
//		gbc_hoursTextField.gridy = 1;
//		panel.add(hoursTextField, gbc_hoursTextField);
//		hoursTextField.setColumns(10);
//		
//		JLabel lblNewLabel_1 = new JLabel("Minutes");
//		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
//		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
//		gbc_lblNewLabel_1.gridx = 1;
//		gbc_lblNewLabel_1.gridy = 0;
//		panel.add(lblNewLabel_1, gbc_lblNewLabel_1);
//		
//		minutesTextField = new JTextField();
//		GridBagConstraints gbc_minutesTextField = new GridBagConstraints();
//		gbc_minutesTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_minutesTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_minutesTextField.gridx = 1;
//		gbc_minutesTextField.gridy = 1;
//		panel.add(minutesTextField, gbc_minutesTextField);
//		minutesTextField.setColumns(10);
//		
//		secondsTextField = new JTextField();
//		GridBagConstraints gbc_secondsTextField = new GridBagConstraints();
//		gbc_secondsTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_secondsTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_secondsTextField.gridx = 2;
//		gbc_secondsTextField.gridy = 1;
//		panel.add(secondsTextField, gbc_secondsTextField);
//		secondsTextField.setColumns(10);
//		
//		daysTextField = new JTextField();
//		GridBagConstraints gbc_daysTextField = new GridBagConstraints();
//		gbc_daysTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_daysTextField.fill = GridBagConstraints.HORIZONTAL;
//		gbc_daysTextField.gridx = 3;
//		gbc_daysTextField.gridy = 1;
//		panel.add(daysTextField, gbc_daysTextField);
//		daysTextField.setColumns(10);
//		
//		GridBagConstraints gbc_daysOfWeek = new GridBagConstraints();
//		gbc_daysOfWeek.gridheight = 2;
//		gbc_daysOfWeek.insets = new Insets(0, 0, 5, 5);
//		gbc_daysOfWeek.fill = GridBagConstraints.BOTH;
//		gbc_daysOfWeek.gridx = 4;
//		gbc_daysOfWeek.gridy = 0;
//		panel.add(daysOfWeekPanel(), gbc_daysOfWeek);
//		
//		GridBagConstraints gbc_months = new GridBagConstraints();
//		gbc_months.gridheight = 2;
//		gbc_months.insets = new Insets(0, 0, 5, 0);
//		gbc_months.fill = GridBagConstraints.VERTICAL;
//		gbc_months.gridx = 5;
//		gbc_months.gridy = 0;
//		panel.add(monthsPanel(), gbc_months);
	}


	
	private class ScheduleLine extends JPanel {
		private static final long serialVersionUID = 1L;
		private JTextField hoursTextField;
		private JTextField minutesTextField;
		private JTextField secondsTextField;
		private JTextField daysTextField;
		private JPanel monthsPanel;
		private JPanel daysOfWeekPanel;
		
		private ScheduleLine() {
			GridBagLayout gbl_panel = new GridBagLayout();
			//		gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 1, 10, 10};
			//		gbl_panel.rowHeights = new int[]{0, 0, 0, 0};
			gbl_panel.columnWeights = new double[]{1.0, 1.0, 1.0, 1.0, 0.5, 1.0, Double.MIN_VALUE};
			gbl_panel.rowWeights = new double[]{1.0, 1.0, 0.0, Double.MIN_VALUE};
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
		}

		private JPanel monthsPanel() {
			JPanel months = new JPanel();
			ActionListener change = e -> asString();
			months.setLayout(new GridLayout(1, 0, 0, 0));
			for(Month m: Month.values()) {
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
			for(DayOfWeek m: DayOfWeek.values()) {
				JCheckBox chckbxNewCheckBox = new JCheckBox(m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
				chckbxNewCheckBox.setVerticalTextPosition(SwingConstants.TOP);
				chckbxNewCheckBox.setHorizontalTextPosition(SwingConstants.CENTER);
				chckbxNewCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
				chckbxNewCheckBox.addActionListener(change);
				days.add(chckbxNewCheckBox);
			}
			return days;
		}
		
		public String asString() {
			String res = "";
			System.out.println(checkboxAsString(monthsPanel) + " " + checkboxAsString(daysOfWeekPanel));
			return res;
		}
		
		// todo sun -sat / day of week 0-6 or SUN-SAT
		private String checkboxAsString(JPanel panel) {
			String res = "";
			int init = -1;
			int countChk = panel.getComponentCount();
			int selectedCount = 0;
			for(int i = 0; i <= countChk; i++) {
				boolean selected = i < countChk && ((JCheckBox) panel.getComponent(i)).isSelected();
				if(selected) {
					selectedCount++;
				}
				if(selected && init < 0) {
					init = i;
				} else if(selected == false && init >= 0 && i - init == 1) {
					res += res.isEmpty() ? (init + 1) : "," + (init + 1);
					init = -1;
				} else if(selected == false && init >= 0 && i - init == 2) {
					res += res.isEmpty() ? (init + 1) + "," + i : "," + (init + 1) + "," + i;
					init = -1;
				} else if(selected == false && init >= 0 && i - init > 2) {
					res += res.isEmpty() ? (init + 1) + "-" + i : "," + (init + 1) + "-" + i;
					init = -1;
				}
			}
			if(res.isEmpty() || selectedCount == countChk) {
				res = "*";
			}
			return res;
		}
	}
	
	public static void main(final String ... args) {
		SchedulerDialog s = new SchedulerDialog(null);
		s.setVisible(true);
	}
}
