package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import it.usna.shellyscan.model.Devices;
import it.usna.util.AppProperties;

public class DialogAppSettings extends JDialog {
	private static final long serialVersionUID = 1L;
	
	public final static String PROP_CSV_SEPARATOR = "CSV_SEPARATOR";
	public final static String PROP_CSV_SEPARATOR_DEFAULT = ",";
	public final static String PROP_SCAN_MODE = "SCAN_MODE";
	public final static String PROP_SCAN_MODE_DEFAULT = "LOCAL";
	public final static String PROP_DCLICK_ACTION = "DCLICK_ACTION";
	public final static String PROP_DCLICK_ACTION_DEFAULT = "DET";
	public final static String PROP_REFRESH_ITERVAL = "REFRESH_INTERVAL";
	public final static int PROP_REFRESH_ITERVAL_DEFAULT = 2;
	public final static String PROP_REFRESH_CONF = "REFRESH_SETTINGS";
	public final static int PROP_REFRESH_CONF_DEFAULT = 5;
	
	public DialogAppSettings(final MainView owner, Devices model, final AppProperties appProp) {
		super(owner, LABELS.getString("dlgAppSetTitle"), true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setVgap(5);
		getContentPane().setLayout(borderLayout);
		((JComponent)getContentPane()).setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
		
		GridBagLayout gridBagLayout = new GridBagLayout();
//		gridBagLayout.columnWidths = new int[]{0, 0, 0};
//		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
//		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0, 0.0};
		JPanel optionsPanel = new JPanel(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppSetScanNetworkLabel"));
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		optionsPanel.add(lblNewLabel, gbc_lblNewLabel);
		
		JRadioButton localScanButton = new JRadioButton(LABELS.getString("dlgAppSetLocalScan"));
		GridBagConstraints gbc_rdbtnNewRadioButton = new GridBagConstraints();
		gbc_rdbtnNewRadioButton.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton.gridx = 1;
		gbc_rdbtnNewRadioButton.gridy = 0;
		optionsPanel.add(localScanButton, gbc_rdbtnNewRadioButton);
		
		JRadioButton fullScanButton = new JRadioButton(LABELS.getString("dlgAppSetFullScan"));
		GridBagConstraints gbc_rdbtnNewRadioButton_1 = new GridBagConstraints();
		gbc_rdbtnNewRadioButton_1.weightx = 4.0;
		gbc_rdbtnNewRadioButton_1.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton_1.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnNewRadioButton_1.gridx = 2;
		gbc_rdbtnNewRadioButton_1.gridy = 0;
		optionsPanel.add(fullScanButton, gbc_rdbtnNewRadioButton_1);
		
		ButtonGroup scanModeGroup = new ButtonGroup();
		scanModeGroup.add(localScanButton);
		scanModeGroup.add(fullScanButton);
		String mode = appProp.getProperty(PROP_SCAN_MODE);
		if(mode == null) {
			appProp.setProperty(PROP_SCAN_MODE, PROP_SCAN_MODE_DEFAULT);
			mode = PROP_SCAN_MODE_DEFAULT;
		}
		if(mode.equals("LOCAL")) {
			localScanButton.setSelected(true);
		} else  {
			fullScanButton.setSelected(true);
		}

		JSeparator separator = new JSeparator();
		separator.setForeground(Color.BLACK);
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.insets = new Insets(0, 0, 5, 0);
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 3;
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 1;
		optionsPanel.add(separator, gbc_separator);
		
		Label label = new Label(LABELS.getString("dlgAppSetDoubleClickLabel"));
		label.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.anchor = GridBagConstraints.WEST;
		gbc_label.insets = new Insets(0, 0, 5, 5);
		gbc_label.gridx = 0;
		gbc_label.gridy = 2;
		optionsPanel.add(label, gbc_label);
		
		JRadioButton detailsButton = new JRadioButton(LABELS.getString("dlgAppSetDetails"));
		GridBagConstraints gbc_detailsButton = new GridBagConstraints();
		gbc_detailsButton.anchor = GridBagConstraints.WEST;
		gbc_detailsButton.insets = new Insets(0, 0, 5, 5);
		gbc_detailsButton.gridx = 1;
		gbc_detailsButton.gridy = 2;
		optionsPanel.add(detailsButton, gbc_detailsButton);
		
		JRadioButton webUIButton = new JRadioButton(LABELS.getString("dlgAppSetWEBUI"));
		GridBagConstraints gbc_webUIButton = new GridBagConstraints();
		gbc_webUIButton.anchor = GridBagConstraints.WEST;
		gbc_webUIButton.insets = new Insets(0, 0, 5, 0);
		gbc_webUIButton.gridx = 2;
		gbc_webUIButton.gridy = 2;
		optionsPanel.add(webUIButton, gbc_webUIButton);
		
		ButtonGroup dClockGroup = new ButtonGroup();
		dClockGroup.add(detailsButton);
		dClockGroup.add(webUIButton);
		if(appProp.getProperty(PROP_DCLICK_ACTION, PROP_DCLICK_ACTION_DEFAULT).equals("DET")) {
			detailsButton.setSelected(true);
		} else  {
			webUIButton.setSelected(true);
		}
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setForeground(Color.BLACK);
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.insets = new Insets(0, 0, 5, 0);
		gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1.gridwidth = 3;
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 3;
		optionsPanel.add(separator_1, gbc_separator_1);
		
		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgAppSetCSV"));
		lblNewLabel_1.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 4;
		optionsPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		JTextField csvTextField = new JTextField(appProp.getProperty(PROP_CSV_SEPARATOR, PROP_CSV_SEPARATOR_DEFAULT));
		GridBagConstraints gbc_csvTextField = new GridBagConstraints();
		gbc_csvTextField.gridwidth = 2;
		gbc_csvTextField.anchor = GridBagConstraints.WEST;
		gbc_csvTextField.insets = new Insets(0, 0, 5, 0);
		gbc_csvTextField.gridx = 1;
		gbc_csvTextField.gridy = 4;
		optionsPanel.add(csvTextField, gbc_csvTextField);
		csvTextField.setColumns(2);
		
		JSeparator separator_2 = new JSeparator();
		separator_1.setForeground(Color.BLACK);
		GridBagConstraints gbc_separator_2 = new GridBagConstraints();
		gbc_separator_2.insets = new Insets(0, 0, 5, 0);
		gbc_separator_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_2.gridwidth = 3;
		gbc_separator_2.gridx = 0;
		gbc_separator_2.gridy = 5;
		optionsPanel.add(separator_2, gbc_separator_2);
		
		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgAppSetRefreshTime"));
		lblNewLabel_3.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 6;
		optionsPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		IntegerTextFieldPanel refreshTextField = new IntegerTextFieldPanel(appProp.getIntProperty(PROP_REFRESH_ITERVAL, PROP_REFRESH_ITERVAL_DEFAULT), 1, 3600, false);
		GridBagConstraints gbc_refreshtextField = new GridBagConstraints();
		gbc_refreshtextField.gridwidth = 2;
		gbc_refreshtextField.anchor = GridBagConstraints.WEST;
		gbc_refreshtextField.insets = new Insets(0, 0, 5, 0);
		gbc_refreshtextField.gridx = 1;
		gbc_refreshtextField.gridy = 6;
		optionsPanel.add(refreshTextField, gbc_refreshtextField);
		refreshTextField.setColumns(4);
		
		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("dlgAppSetConfRefreshTic"));
		lblNewLabel_4.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 7;
		optionsPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		IntegerTextFieldPanel confRefreshtextField = new IntegerTextFieldPanel(appProp.getIntProperty(PROP_REFRESH_CONF, PROP_REFRESH_CONF_DEFAULT), 1, 9999, false);
		GridBagConstraints gbc_confRefreshtextField = new GridBagConstraints();
		gbc_confRefreshtextField.gridwidth = 2;
		gbc_confRefreshtextField.insets = new Insets(0, 0, 5, 0);
		gbc_confRefreshtextField.anchor = GridBagConstraints.WEST;
		gbc_confRefreshtextField.gridx = 1;
		gbc_confRefreshtextField.gridy = 7;
		optionsPanel.add(confRefreshtextField, gbc_confRefreshtextField);
		confRefreshtextField.setColumns(4);
		
		JLabel lblNewLabel_5 = new JLabel(LABELS.getString("dlgAppSetRefreshMsg"));
		lblNewLabel_5.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_5.weighty = 1.0;
		gbc_lblNewLabel_5.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_5.gridwidth = 3;
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 8;
		optionsPanel.add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		getContentPane().add(optionsPanel, BorderLayout.CENTER);
		
		JPanel panelCommands = new JPanel();
		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		JButton btnOKButton = new JButton(LABELS.getString("dlgApply"));
		panelCommands.add(btnOKButton);
		panelCommands.add(btnClose);
		btnClose.addActionListener(event -> dispose());
		btnOKButton.addActionListener(event -> {
			appProp.setProperty(PROP_CSV_SEPARATOR, csvTextField.getText());
			if(appProp.changeProperty(PROP_SCAN_MODE, localScanButton.isSelected() ? "LOCAL" : "FULL")) {
				JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworMsg"), LABELS.getString("dlgAppSetTitle"), JOptionPane.WARNING_MESSAGE);
			}
			appProp.setProperty(PROP_DCLICK_ACTION, detailsButton.isSelected() ? "DET" : "WEB");
			boolean r0 = appProp.changeProperty(PROP_REFRESH_ITERVAL, refreshTextField.getText());
			boolean r1 = appProp.changeProperty(PROP_REFRESH_CONF, confRefreshtextField.getText());
			if(r0 || r1) {
				model.setRefreshTime(appProp.getIntProperty(PROP_REFRESH_ITERVAL) * 1000, appProp.getIntProperty(PROP_REFRESH_CONF));
				for(int i = 0; i < model.size(); i++) {
					model.refresh(i, true);
				}
			}
			dispose();
		});
		getContentPane().add(panelCommands, BorderLayout.SOUTH);
		
		optionsPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
}