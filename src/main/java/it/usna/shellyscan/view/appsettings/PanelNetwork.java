package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.DevicesFactory;
import it.usna.shellyscan.view.IntegerTextFieldPanel;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;

public class PanelNetwork extends JPanel {
	private static final long serialVersionUID = 1L;

	private JRadioButton localScanButton;
	private JRadioButton fullScanButton;
	private JRadioButton ipScanButton;
	private JTextField userFieldRL;
	private JPasswordField passwordFieldRL;
	private IntegerTextFieldPanel refreshTextField;
	private IntegerTextFieldPanel confRefreshtextField;
	
	private JPanel panelIP = new JPanel();
	private JButton btnIPEdit = new JButton(LABELS.getString("edit2"));
	private DialogNetworkIPScanSelection dialogIP;

	PanelNetwork(JDialog parent, final AppProperties appProp) {
		dialogIP = new DialogNetworkIPScanSelection(parent);
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, 0.0, 10.0};
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 10};
		setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppSetScanNetworkLabel"));
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 15);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);

		ButtonGroup scanModeGroup = new ButtonGroup();

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 5));
		panel.setBorder(new EmptyBorder(0, -24, 0, 0));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.gridwidth = 3;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 0;
		add(panel, gbc_panel);

		localScanButton = new JRadioButton(LABELS.getString("dlgAppSetLocalScan"));
		panel.add(localScanButton);
		scanModeGroup.add(localScanButton);

		fullScanButton = new JRadioButton(LABELS.getString("dlgAppSetFullScan"));
		panel.add(fullScanButton);
		scanModeGroup.add(fullScanButton);

		ipScanButton = new JRadioButton(LABELS.getString("dlgAppSetIPScan"));
		panel.add(ipScanButton);
		scanModeGroup.add(ipScanButton);
		
		JRadioButton offlineButton = new JRadioButton(LABELS.getString("dlgAppSetOfflineScan"));
		panel.add(offlineButton);
		scanModeGroup.add(offlineButton);

		ipScanButton.addChangeListener(e -> scanByIP(ipScanButton.isSelected()));

		String baseIPProp = appProp.getProperty(ScannerProperties.BASE_SCAN_IP);
		if(baseIPProp == null) {
			try {
				baseIPProp = InetAddress.getLocalHost().getHostAddress();
				baseIPProp = baseIPProp.substring(0, baseIPProp.lastIndexOf('.'));
			} catch (UnknownHostException e) {
				baseIPProp = "";
			}
		}
		
		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(5, 0, 5, 0);
		gbc_scrollPane.gridwidth = 2;
		gbc_scrollPane.fill = GridBagConstraints.HORIZONTAL;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 1;
		add(scrollPane, gbc_scrollPane);

		scrollPane.setViewportView(panelIP);
		panelIP.setLayout(new GridLayout(0, 2, 0, 1));
		fillIPPanel();

		btnIPEdit.addActionListener(e -> {
			dialogIP.setVisible(true);
			fillIPPanel();
		});
		GridBagConstraints gbc_btnIPEdit = new GridBagConstraints();
		gbc_btnIPEdit.insets = new Insets(1, 0, 1, 0);
		gbc_btnIPEdit.gridx = 3;
		gbc_btnIPEdit.gridy = 1;
		add(btnIPEdit, gbc_btnIPEdit);

		String mode = appProp.getProperty(ScannerProperties.PROP_SCAN_MODE);
		if(mode == null) {
			appProp.setProperty(ScannerProperties.PROP_SCAN_MODE, ScannerProperties.PROP_SCAN_MODE_DEFAULT);
			mode = ScannerProperties.PROP_SCAN_MODE_DEFAULT;
		}
		if(mode.equals("LOCAL")) {
			localScanButton.setSelected(true);
			scanByIP(false);
		} else if(mode.equals("FULL")) {
			fullScanButton.setSelected(true);
			scanByIP(false);
		} else if(mode.equals("IP")) {
			ipScanButton.setSelected(true);
			scanByIP(true);
		} else { // "OFFLINE"
			offlineButton.setSelected(true);
			scanByIP(false);
		}

		JSeparator separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.insets = new Insets(0, 0, 5, 0);
		gbc_separator_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_1.gridwidth = 4;
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 2;
		add(separator_1, gbc_separator_1);

		JLabel lblNewLabel_8 = new JLabel(LABELS.getString("dlgAppSetRestrictedLoginUser"));
		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_8.insets = new Insets(0, 2, 5, 5);
		gbc_lblNewLabel_8.gridx = 1;
		gbc_lblNewLabel_8.gridy = 3;
		add(lblNewLabel_8, gbc_lblNewLabel_8);

		JLabel lblNewLabel_9 = new JLabel(LABELS.getString("labelPassword"));
		GridBagConstraints gbc_lblNewLabel_9 = new GridBagConstraints();
		gbc_lblNewLabel_9.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_9.insets = new Insets(0, 2, 5, 5);
		gbc_lblNewLabel_9.gridx = 2;
		gbc_lblNewLabel_9.gridy = 3;
		add(lblNewLabel_9, gbc_lblNewLabel_9);

		JLabel lblNewLabel_6 = new JLabel(LABELS.getString("dlgAppSetRestrictedLogin"));
		lblNewLabel_6.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 4;
		add(lblNewLabel_6, gbc_lblNewLabel_6);

		userFieldRL = new JTextField(appProp.getProperty(ScannerProperties.PROP_LOGIN_USER));
		GridBagConstraints gbc_userFieldRL = new GridBagConstraints();
		gbc_userFieldRL.anchor = GridBagConstraints.WEST;
		gbc_userFieldRL.insets = new Insets(0, 0, 5, 5);
		gbc_userFieldRL.gridx = 1;
		gbc_userFieldRL.gridy = 4;
		add(userFieldRL, gbc_userFieldRL);
		userFieldRL.setColumns(20);

		passwordFieldRL = new JPasswordField();
		try {
			passwordFieldRL.setText(new String(Base64.getDecoder().decode(appProp.getProperty(ScannerProperties.PROP_LOGIN_PWD).substring(1))));
		} catch(RuntimeException e) {}
		passwordFieldRL.setColumns(20);
		GridBagConstraints gbc_passwordFieldRL = new GridBagConstraints();
		gbc_passwordFieldRL.anchor = GridBagConstraints.WEST;
		gbc_passwordFieldRL.gridwidth = 2;
		gbc_passwordFieldRL.insets = new Insets(0, 0, 5, 0);
		gbc_passwordFieldRL.gridx = 2;
		gbc_passwordFieldRL.gridy = 4;
		add(passwordFieldRL, gbc_passwordFieldRL);

		JLabel lblNewLabel_7 = new JLabel(LABELS.getString("dlgAppSetRestrictedLogigMsg"));
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_7.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_7.gridwidth = 2;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 5;
		add(lblNewLabel_7, gbc_lblNewLabel_7);

		JCheckBox chckbxShowPwd = new JCheckBox(LABELS.getString("labelShowPwd"));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxNewCheckBox.gridx = 2;
		gbc_chckbxNewCheckBox.gridy = 5;
		add(chckbxShowPwd, gbc_chckbxNewCheckBox);

		JSeparator separator_2 = new JSeparator();
		GridBagConstraints gbc_separator_2 = new GridBagConstraints();
		gbc_separator_2.insets = new Insets(0, 0, 5, 0);
		gbc_separator_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator_2.gridwidth = 4;
		gbc_separator_2.gridx = 0;
		gbc_separator_2.gridy = 6;
		add(separator_2, gbc_separator_2);
		
		JLabel lblNewLabel_rt = new JLabel(LABELS.getString("dlgAppSetRefreshTime"));
		lblNewLabel_rt.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_rt = new GridBagConstraints();
		gbc_lblNewLabel_rt.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_rt.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_rt.gridx = 0;
		gbc_lblNewLabel_rt.gridy = 7;
		add(lblNewLabel_rt, gbc_lblNewLabel_rt);
		
		refreshTextField = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.PROP_REFRESH_ITERVAL, ScannerProperties.PROP_REFRESH_ITERVAL_DEFAULT), 1, 3600, false);
		GridBagConstraints gbc_refreshtextField = new GridBagConstraints();
		gbc_refreshtextField.anchor = GridBagConstraints.WEST;
		gbc_refreshtextField.insets = new Insets(0, 0, 5, 5);
		gbc_refreshtextField.gridx = 1;
		gbc_refreshtextField.gridy = 7;
		add(refreshTextField, gbc_refreshtextField);
		refreshTextField.setColumns(4);
		
		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("dlgAppSetConfRefreshTic"));
		lblNewLabel_4.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 8;
		add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		confRefreshtextField = new IntegerTextFieldPanel(appProp.getIntProperty(ScannerProperties.PROP_REFRESH_CONF, ScannerProperties.PROP_REFRESH_CONF_DEFAULT), 1, 9999, false);
		GridBagConstraints gbc_confRefreshtextField = new GridBagConstraints();
		gbc_confRefreshtextField.insets = new Insets(0, 0, 5, 5);
		gbc_confRefreshtextField.anchor = GridBagConstraints.WEST;
		gbc_confRefreshtextField.gridx = 1;
		gbc_confRefreshtextField.gridy = 8;
		add(confRefreshtextField, gbc_confRefreshtextField);
		confRefreshtextField.setColumns(4);
		
		JLabel lblNewLabel_5 = new JLabel(LABELS.getString("dlgAppSetRefreshMsg"));
		lblNewLabel_5.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.weighty = 1.0;
		gbc_lblNewLabel_5.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_5.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_5.gridwidth = 4;
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 9;
		add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		final char pwdEchoChar = passwordFieldRL.getEchoChar();
		chckbxShowPwd.addItemListener(e -> {
			passwordFieldRL.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar);
			passwordFieldRL.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar);
		});
	}
	
	private void fillIPPanel() {
		panelIP.removeAll();
		for(int i = 0; i < 10; i++) {
			String ip = dialogIP.baseIP[i].getText();
			if(ip.isEmpty() == false) {
				panelIP.add(new JLabel(ip + " / " + dialogIP.firstIP[i].getText() + "-" +  dialogIP.lastIP[i].getText()));
				/*panelIP.*/revalidate();
			}
		}
	}
	
	private void scanByIP(boolean ip) {
		btnIPEdit.setEnabled(ip);
		for(Component c: panelIP.getComponents()) {
			c.setEnabled(ip);
		}
	}
	
	public void store(AppProperties appProp, Devices model) {
		// Scan mode
		String scanMode;
		if(localScanButton.isSelected()) {
			scanMode = "LOCAL";
		} else if(fullScanButton.isSelected()) {
			scanMode = "FULL";
		} else if(ipScanButton.isSelected()) {
			scanMode = "IP";
		} else { // Offline
			scanMode = "OFFLINE";
		}
		if(appProp.changeProperty(ScannerProperties.PROP_SCAN_MODE, scanMode)) {
			JOptionPane.showMessageDialog(this, LABELS.getString("dlgAppSetScanNetworMsg"), LABELS.getString("dlgAppSetTitle"), JOptionPane.WARNING_MESSAGE);
		}
		
		// Login
		String rlUser = userFieldRL.getText();
		appProp.setProperty(ScannerProperties.PROP_LOGIN_USER, rlUser);
		String encodedRlp = "";
		if(rlUser.length() > 0) {
			char[] rlp = passwordFieldRL.getPassword();
			try {
				encodedRlp = (char)(rlp.hashCode() % ('Z' - 'A') + 'A') + Base64.getEncoder().encodeToString(new String(rlp).getBytes());
			} catch(RuntimeException e) {}
			DevicesFactory.setCredential(rlUser, rlp);
		} else {
			DevicesFactory.setCredential(null, null);
		}
		appProp.setProperty(ScannerProperties.PROP_LOGIN_PWD, encodedRlp);
		
		// Refresh
		boolean r0 = appProp.changeProperty(ScannerProperties.PROP_REFRESH_ITERVAL, refreshTextField.getText());
		boolean r1 = appProp.changeProperty(ScannerProperties.PROP_REFRESH_CONF, confRefreshtextField.getText());
		if(r0 || r1) {
			model.setRefreshTime(appProp.getIntProperty(ScannerProperties.PROP_REFRESH_ITERVAL) * 1000, appProp.getIntProperty(ScannerProperties.PROP_REFRESH_CONF));
			for(int i = 0; i < model.size(); i++) {
				model.refresh(i, true);
			}
		}

		dialogIP.store(model);
	}
}