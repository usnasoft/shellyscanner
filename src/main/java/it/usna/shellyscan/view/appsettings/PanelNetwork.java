package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import it.usna.shellyscan.view.IntegerTextFieldPanel;
import it.usna.util.AppProperties;

public class PanelNetwork extends JPanel {
	private static final long serialVersionUID = 1L;

	JRadioButton localScanButton;
	JRadioButton fullScanButton;
	JTextField baseIP;
	IntegerTextFieldPanel firstIP;
	IntegerTextFieldPanel lastIP;
	JTextField userFieldRL;
	JPasswordField passwordFieldRL;
	IntegerTextFieldPanel refreshTextField;
	IntegerTextFieldPanel confRefreshtextField;

	PanelNetwork(final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
//		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
		//		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
		gridBagLayout.columnWeights = new double[]{1.0, 0.0, 0.0, 10.0};
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 10};
//		gridBagLayout.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
		//		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		//		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0, 0.0};
		setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppSetScanNetworkLabel"));
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 10, 15);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);

		ButtonGroup scanModeGroup = new ButtonGroup();

		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 10, 5);
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.gridwidth = 2;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 0;
		add(panel, gbc_panel);
		panel.setLayout(new GridLayout(0, 3, 18, 0));

		localScanButton = new JRadioButton(LABELS.getString("dlgAppSetLocalScan"));
		panel.add(localScanButton);
		scanModeGroup.add(localScanButton);

		fullScanButton = new JRadioButton(LABELS.getString("dlgAppSetFullScan"));
		panel.add(fullScanButton);
		scanModeGroup.add(fullScanButton);

		JRadioButton ipScanButton = new JRadioButton(LABELS.getString("dlgAppSetIPScan"));
		panel.add(ipScanButton);
		scanModeGroup.add(ipScanButton);

		ipScanButton.addChangeListener(e -> scanByIP(ipScanButton.isSelected()));

		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgAppSetIPBase"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(0, 2, 3, 5);
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.gridx = 1;
		gbc_lblNewLabel_1.gridy = 1;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		String baseIPProp = appProp.getProperty(DialogAppSettings.BASE_SCAN_IP);
		if(baseIPProp == null) {
			try {
				baseIPProp = InetAddress.getLocalHost().getHostAddress();
				baseIPProp = baseIPProp.substring(0, baseIPProp.lastIndexOf('.'));
			} catch (UnknownHostException e) {
				baseIPProp = "";
			}
		}

		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("dlgAppSetIPFirst"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 2, 3, 5);
		gbc_lblNewLabel_2.gridx = 2;
		gbc_lblNewLabel_2.gridy = 1;
		add(lblNewLabel_2, gbc_lblNewLabel_2);

		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgAppSetIPLast"));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.insets = new Insets(0, 2, 3, 0);
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.gridx = 3;
		gbc_lblNewLabel_3.gridy = 1;
		add(lblNewLabel_3, gbc_lblNewLabel_3);
		baseIP = new JTextField(baseIPProp);
		GridBagConstraints gbc_baseIP = new GridBagConstraints();
		gbc_baseIP.gridwidth = 3;
		gbc_baseIP.insets = new Insets(0, 0, 5, 0);
		gbc_baseIP.anchor = GridBagConstraints.WEST;
		gbc_baseIP.gridx = 1;
		gbc_baseIP.gridy = 2;
		add(baseIP, gbc_baseIP);
		baseIP.setColumns(11);

		firstIP = new IntegerTextFieldPanel(appProp.getIntProperty(DialogAppSettings.FIRST_SCAN_IP, DialogAppSettings.FIST_SCAN_IP_DEFAULT), 0, 255, false);
		GridBagConstraints gbc_firstIP = new GridBagConstraints();
		gbc_firstIP.anchor = GridBagConstraints.WEST;
		gbc_firstIP.insets = new Insets(0, 0, 5, 5);
		gbc_firstIP.gridx = 2;
		gbc_firstIP.gridy = 2;
		add(firstIP, gbc_firstIP);
		firstIP.setColumns(3);

		lastIP = new IntegerTextFieldPanel(appProp.getIntProperty(DialogAppSettings.LAST_SCAN_IP, DialogAppSettings.LAST_SCAN_IP_DEFAULT), 0, 255, false);
		GridBagConstraints gbc_lastIP = new GridBagConstraints();
		gbc_lastIP.insets = new Insets(0, 0, 5, 5);
		gbc_lastIP.anchor = GridBagConstraints.WEST;
		gbc_lastIP.gridx = 3;
		gbc_lastIP.gridy = 2;
		add(lastIP, gbc_lastIP);
		lastIP.setColumns(3);

		String mode = appProp.getProperty(DialogAppSettings.PROP_SCAN_MODE);
		if(mode == null) {
			appProp.setProperty(DialogAppSettings.PROP_SCAN_MODE, DialogAppSettings.PROP_SCAN_MODE_DEFAULT);
			mode = DialogAppSettings.PROP_SCAN_MODE_DEFAULT;
		}
		if(mode.equals("LOCAL")) {
			localScanButton.setSelected(true);
			scanByIP(false);
		} else if(mode.equals("FULL")) {
			fullScanButton.setSelected(true);
			scanByIP(false);
		} else{
			ipScanButton.setSelected(true);
			scanByIP(true);
		}

		JSeparator separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.insets = new Insets(0, 0, 5, 0);
		gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1.gridwidth = 4;
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 3;
		add(separator_1, gbc_separator_1);

		JLabel lblNewLabel_8 = new JLabel(LABELS.getString("dlgAppSetRestrictedLoginUser"));
		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_8.insets = new Insets(0, 2, 3, 5);
		gbc_lblNewLabel_8.gridx = 1;
		gbc_lblNewLabel_8.gridy = 4;
		add(lblNewLabel_8, gbc_lblNewLabel_8);

		JLabel lblNewLabel_9 = new JLabel(LABELS.getString("labelPassword"));
		GridBagConstraints gbc_lblNewLabel_9 = new GridBagConstraints();
		gbc_lblNewLabel_9.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_9.insets = new Insets(0, 2, 3, 5);
		gbc_lblNewLabel_9.gridx = 2;
		gbc_lblNewLabel_9.gridy = 4;
		add(lblNewLabel_9, gbc_lblNewLabel_9);

		JLabel lblNewLabel_6 = new JLabel(LABELS.getString("dlgAppSetRestrictedLogin"));
		lblNewLabel_6.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 5;
		add(lblNewLabel_6, gbc_lblNewLabel_6);

		userFieldRL = new JTextField(appProp.getProperty(DialogAppSettings.PROP_LOGIN_USER));
		GridBagConstraints gbc_userFieldRL = new GridBagConstraints();
		gbc_userFieldRL.anchor = GridBagConstraints.WEST;
		gbc_userFieldRL.insets = new Insets(0, 0, 5, 5);
		gbc_userFieldRL.gridx = 1;
		gbc_userFieldRL.gridy = 5;
		add(userFieldRL, gbc_userFieldRL);
		userFieldRL.setColumns(20);

		passwordFieldRL = new JPasswordField();
		try {
			passwordFieldRL.setText(new String(Base64.getDecoder().decode(appProp.getProperty(DialogAppSettings.PROP_LOGIN_PWD).substring(1))));
		} catch(RuntimeException e) {}
		passwordFieldRL.setColumns(20);
		GridBagConstraints gbc_passwordFieldRL = new GridBagConstraints();
		gbc_passwordFieldRL.anchor = GridBagConstraints.WEST;
		gbc_passwordFieldRL.gridwidth = 2;
		gbc_passwordFieldRL.insets = new Insets(0, 0, 5, 0);
		gbc_passwordFieldRL.gridx = 2;
		gbc_passwordFieldRL.gridy = 5;
		add(passwordFieldRL, gbc_passwordFieldRL);

		JLabel lblNewLabel_7 = new JLabel(LABELS.getString("dlgAppSetRestrictedLogigMsg"));
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_7.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_7.gridwidth = 2;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 6;
		add(lblNewLabel_7, gbc_lblNewLabel_7);

		JCheckBox chckbxShowPwd = new JCheckBox(LABELS.getString("labelShowPwd"));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxNewCheckBox.gridx = 2;
		gbc_chckbxNewCheckBox.gridy = 6;
		add(chckbxShowPwd, gbc_chckbxNewCheckBox);

		JSeparator separator_2 = new JSeparator();
		GridBagConstraints gbc_separator_2 = new GridBagConstraints();
		gbc_separator_2.insets = new Insets(0, 0, 5, 0);
		gbc_separator_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_2.gridwidth = 4;
		gbc_separator_2.gridx = 0;
		gbc_separator_2.gridy = 7;
		add(separator_2, gbc_separator_2);
		
		JLabel lblNewLabel_rt = new JLabel(LABELS.getString("dlgAppSetRefreshTime"));
		lblNewLabel_rt.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_rt = new GridBagConstraints();
		gbc_lblNewLabel_rt.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_rt.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_rt.gridx = 0;
		gbc_lblNewLabel_rt.gridy = 8;
		add(lblNewLabel_rt, gbc_lblNewLabel_rt);
		
		refreshTextField = new IntegerTextFieldPanel(appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_ITERVAL, DialogAppSettings.PROP_REFRESH_ITERVAL_DEFAULT), 1, 3600, false);
		GridBagConstraints gbc_refreshtextField = new GridBagConstraints();
		gbc_refreshtextField.anchor = GridBagConstraints.WEST;
		gbc_refreshtextField.insets = new Insets(0, 0, 5, 5);
		gbc_refreshtextField.gridx = 1;
		gbc_refreshtextField.gridy = 8;
		add(refreshTextField, gbc_refreshtextField);
		refreshTextField.setColumns(4);
		
		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("dlgAppSetConfRefreshTic"));
		lblNewLabel_4.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 9;
		add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		confRefreshtextField = new IntegerTextFieldPanel(appProp.getIntProperty(DialogAppSettings.PROP_REFRESH_CONF, DialogAppSettings.PROP_REFRESH_CONF_DEFAULT), 1, 9999, false);
		GridBagConstraints gbc_confRefreshtextField = new GridBagConstraints();
		gbc_confRefreshtextField.insets = new Insets(0, 0, 5, 5);
		gbc_confRefreshtextField.anchor = GridBagConstraints.WEST;
		gbc_confRefreshtextField.gridx = 1;
		gbc_confRefreshtextField.gridy = 9;
		add(confRefreshtextField, gbc_confRefreshtextField);
		confRefreshtextField.setColumns(4);
		
		JLabel lblNewLabel_5 = new JLabel(LABELS.getString("dlgAppSetRefreshMsg"));
		lblNewLabel_5.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.weighty = 1.0;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_5.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_5.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_5.gridwidth = 4;
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 10;
		add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		final char pwdEchoChar = passwordFieldRL.getEchoChar();
		chckbxShowPwd.addItemListener(e -> {
			passwordFieldRL.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar);
			passwordFieldRL.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar);
		});
	}

	private void scanByIP(boolean ip) {
		baseIP.setEnabled(ip);
		firstIP.setEnabled(ip);
		lastIP.setEnabled(ip);
	}
}