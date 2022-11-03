package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;
import it.usna.shellyscan.model.device.WIFIManager.Network;
import it.usna.shellyscan.view.DialogDeviceSelection;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.util.UsnaEventListener;

public class PanelWIFI extends AbstractSettingsPanel implements UsnaEventListener<ShellyAbstractDevice, Object> {
	private static final long serialVersionUID = 1L;
	private JCheckBox chckbxEnabled;
	private JTextField textFieldSSID;
	private JPasswordField textFieldPwd;
	private JCheckBox chckbxShowPwd;
	private JTextField textFieldStaticIP;
	private JTextField textFieldNetmask;
	private JTextField textFieldGateway;
	private JTextField textFieldDNS;
	private JRadioButton rdbtnDHCP = new JRadioButton(Main.LABELS.getString("dlgSetEnabled"));
	private JRadioButton rdbtnStaticIP = new JRadioButton(Main.LABELS.getString("dlgSetStatic"));
	private char pwdEchoChar;
	private List<WIFIManager> fwModule = new ArrayList<>();

	private final static String IPV4_REGEX = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	private JButton btnCopy = new JButton(LABELS.getString("btnCopyFrom"));
	private DialogDeviceSelection selDialog = null;

	public PanelWIFI(JDialog owner, List<ShellyAbstractDevice> devices, final Devices model) {
		super(devices);
		setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(Main.LABELS.getString("dlgSetEnabled"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);

		chckbxEnabled = new JCheckBox(/*"", true*/);
		GridBagConstraints gbc_chckbxEnabled = new GridBagConstraints();
		gbc_chckbxEnabled.anchor = GridBagConstraints.WEST;
		gbc_chckbxEnabled.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxEnabled.gridx = 1;
		gbc_chckbxEnabled.gridy = 0;
		add(chckbxEnabled, gbc_chckbxEnabled);

		GridBagConstraints gbc_btnCopy = new GridBagConstraints();
		gbc_btnCopy.anchor = GridBagConstraints.EAST;
		gbc_btnCopy.insets = new Insets(0, 0, 5, 0);
		gbc_btnCopy.gridx = 3;
		gbc_btnCopy.gridy = 0;
		add(btnCopy, gbc_btnCopy);
		btnCopy.addActionListener(e -> selDialog = new DialogDeviceSelection(owner, this, model));

		JLabel lblNewLabel_1 = new JLabel(Main.LABELS.getString("dlgSetSSID"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		textFieldSSID = new JTextField();
		GridBagConstraints gbc_textFieldSSID = new GridBagConstraints();
		gbc_textFieldSSID.weightx = 1.0;
		gbc_textFieldSSID.gridwidth = 3;
		gbc_textFieldSSID.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldSSID.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldSSID.gridx = 1;
		gbc_textFieldSSID.gridy = 1;
		add(textFieldSSID, gbc_textFieldSSID);
		textFieldSSID.setColumns(10);

		JLabel lblNewLabel_2 = new JLabel(Main.LABELS.getString("labelPassword"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 2;
		add(lblNewLabel_2, gbc_lblNewLabel_2);

		textFieldPwd = new JPasswordField();
		pwdEchoChar = textFieldPwd.getEchoChar();
		GridBagConstraints gbc_textFieldPwd = new GridBagConstraints();
		gbc_textFieldPwd.weightx = 1.0;
		gbc_textFieldPwd.gridwidth = 3;
		gbc_textFieldPwd.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPwd.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPwd.gridx = 1;
		gbc_textFieldPwd.gridy = 2;
		add(textFieldPwd, gbc_textFieldPwd);
		textFieldPwd.setColumns(10);

		chckbxShowPwd = new JCheckBox(Main.LABELS.getString("labelShowPwd"));
		chckbxShowPwd.addItemListener(e -> textFieldPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.gridwidth = 2;
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 3;
		add(chckbxShowPwd, gbc_chckbxNewCheckBox);

		JLabel lblNewLabel_3 = new JLabel(Main.LABELS.getString("dlgSetDHCP"));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 4;
		add(lblNewLabel_3, gbc_lblNewLabel_3);

		GridBagConstraints gbc_rdbtnDHCP = new GridBagConstraints();
		gbc_rdbtnDHCP.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDHCP.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnDHCP.gridx = 1;
		gbc_rdbtnDHCP.gridy = 4;
		add(rdbtnDHCP, gbc_rdbtnDHCP);

		GridBagConstraints gbc_rdbtnStaticIP = new GridBagConstraints();
		gbc_rdbtnStaticIP.anchor = GridBagConstraints.EAST;
		gbc_rdbtnStaticIP.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnStaticIP.gridx = 2;
		gbc_rdbtnStaticIP.gridy = 4;
		add(rdbtnStaticIP, gbc_rdbtnStaticIP);

		JLabel lblNewLabel_4 = new JLabel(Main.LABELS.getString("dlgSetIP"));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 5;
		add(lblNewLabel_4, gbc_lblNewLabel_4);

		textFieldStaticIP = new JTextField();
		GridBagConstraints gbc_textFieldStaticIP = new GridBagConstraints();
		gbc_textFieldStaticIP.gridwidth = 2;
		gbc_textFieldStaticIP.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldStaticIP.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldStaticIP.gridx = 1;
		gbc_textFieldStaticIP.gridy = 5;
		add(textFieldStaticIP, gbc_textFieldStaticIP);
		textFieldStaticIP.setColumns(16);

		JLabel lblNewLabel_5 = new JLabel(Main.LABELS.getString("dlgSetNetmask"));
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 6;
		add(lblNewLabel_5, gbc_lblNewLabel_5);

		textFieldNetmask = new JTextField();
		GridBagConstraints gbc_textFieldNetmask = new GridBagConstraints();
		gbc_textFieldNetmask.gridwidth = 2;
		gbc_textFieldNetmask.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldNetmask.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldNetmask.gridx = 1;
		gbc_textFieldNetmask.gridy = 6;
		add(textFieldNetmask, gbc_textFieldNetmask);
		textFieldNetmask.setColumns(16);

		JLabel lblNewLabel_6 = new JLabel(Main.LABELS.getString("dlgSetGateway"));
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 7;
		add(lblNewLabel_6, gbc_lblNewLabel_6);

		textFieldGateway = new JTextField();
		GridBagConstraints gbc_textFieldGateway = new GridBagConstraints();
		gbc_textFieldGateway.gridwidth = 2;
		gbc_textFieldGateway.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldGateway.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldGateway.gridx = 1;
		gbc_textFieldGateway.gridy = 7;
		add(textFieldGateway, gbc_textFieldGateway);
		textFieldGateway.setColumns(16);

		JLabel lblNewLabel_7 = new JLabel(Main.LABELS.getString("dlgSetDNS"));
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 8;
		add(lblNewLabel_7, gbc_lblNewLabel_7);

		textFieldDNS = new JTextField();
		GridBagConstraints gbc_textFieldDNS = new GridBagConstraints();
		gbc_textFieldDNS.gridwidth = 2;
		gbc_textFieldDNS.insets = new Insets(0, 0, 0, 5);
		gbc_textFieldDNS.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldDNS.gridx = 1;
		gbc_textFieldDNS.gridy = 8;
		add(textFieldDNS, gbc_textFieldDNS);
		textFieldDNS.setColumns(16);

		ButtonGroup dhcpStatic = new ButtonGroup();
		dhcpStatic.add(rdbtnDHCP);
		dhcpStatic.add(rdbtnStaticIP);
		rdbtnStaticIP.addItemListener(event -> setStaticIP(event.getStateChange() == java.awt.event.ItemEvent.SELECTED));

		chckbxEnabled.addItemListener(event -> setEnabledWIFI(event.getStateChange() == java.awt.event.ItemEvent.SELECTED, rdbtnStaticIP.isSelected()));
	}

	private void setEnabledWIFI(boolean enabled, boolean staticIP) {
		textFieldSSID.setEnabled(enabled);
		textFieldPwd.setEnabled(enabled);
		chckbxShowPwd.setEnabled(enabled);
		rdbtnDHCP.setEnabled(enabled && devices.size() == 1);
		rdbtnStaticIP.setEnabled(enabled && devices.size() == 1);
		if(enabled == false) {
			//			textFieldSSID.setText("");
			//			textFieldPwd.setText("");
			setStaticIP(false);
		} else {
			setStaticIP(staticIP);
		}
	}

	private void setStaticIP(boolean staticIP) {
		textFieldStaticIP.setEnabled(staticIP && devices.size() == 1);
		textFieldNetmask.setEnabled(staticIP);
		textFieldGateway.setEnabled(staticIP);
		textFieldDNS.setEnabled(staticIP);
		//		if(staticIP == false) {
		//			textFieldStaticIP.setText("");
		//			textFieldNetmask.setText("");
		//			textFieldGateway.setText("");
		//			textFieldDNS.setText("");
		//		}
	}

	@Override
	public String showing() throws InterruptedException {
		String exclude = "<html>" + LABELS.getString("dlgExcludedDevicesMsg");
		int excludeCount = 0;
		ShellyAbstractDevice d = null;
		fwModule.clear();
		try {
			btnCopy.setEnabled(false);
			chckbxEnabled.setEnabled(false);
			setEnabledWIFI(false, false); // disable while checking
			boolean enabledGlobal = false;
			String ssidGlobal = "";
			boolean staticIPGlobal = false;
			String globalIP = "";
			String globalNetmask = "";
			String globalGW = "";
			String globalDNS = "";
			boolean first = true;
			for(int i = 0; i < devices.size(); i++) {
				d = devices.get(i);
				try {
					WIFIManager sta = d.getWIFIManager(WIFIManager.Network.SECONDARY);
					if(Thread.interrupted()) {
						throw new InterruptedException();
					}
					boolean enabled = sta.isEnabled();
					String dSSID = sta.getSSID();
					boolean staticIP = sta.isStaticIP();
					String ip = sta.getIP();
					String netmask = sta.getMask();
					String gw = sta.getGateway();
					String dns = sta.getDNS();
					if(first) {
						enabledGlobal = enabled;
						ssidGlobal = dSSID;
						staticIPGlobal = staticIP;
						globalIP = ip;
						globalNetmask = netmask;
						globalGW = gw;
						globalDNS = dns;
						first = false;
					} else {
						if(enabled != enabledGlobal) enabledGlobal = false;
						if(dSSID.equals(ssidGlobal) == false) ssidGlobal = "";
						if(staticIP != staticIPGlobal) staticIPGlobal = false;
						if(ip.equals(globalIP) == false) globalIP = "";
						if(netmask.equals(globalNetmask) == false) globalNetmask = "";
						if(gw.equals(globalGW) == false) globalGW = "";
						if(dns.equals(globalDNS) == false) globalDNS = "";
					}
					fwModule.add(sta);
				} catch(IOException | RuntimeException e) {
					fwModule.add(null);
					exclude += "<br>" + UtilCollecion.getFullName(d);
					excludeCount++;
				}
			}
//			if(Thread.interrupted()) {
//				throw new InterruptedException();
//			}
			if(excludeCount == devices.size()) {
				return LABELS.getString("msgAllDevicesExcluded");
			} else if (excludeCount > 0) {
				Msg.showHtmlMessageDialog(this, exclude, LABELS.getString("dlgExcludedDevicesTitle"), JOptionPane.WARNING_MESSAGE);
			}
			chckbxEnabled.setEnabled(true); // form is active
			chckbxEnabled.setSelected(enabledGlobal);
			textFieldSSID.setText(ssidGlobal);
			rdbtnDHCP.setSelected(staticIPGlobal == false);
			rdbtnStaticIP.setSelected(staticIPGlobal);
			textFieldStaticIP.setText(globalIP);
			textFieldNetmask.setText(globalNetmask);
			textFieldGateway.setText(globalGW);
			textFieldDNS.setText(globalDNS);			
			setEnabledWIFI(/*chckbxEnabled.isSelected()*/enabledGlobal, /*rdbtnStaticIP.isSelected()*/staticIPGlobal);
			btnCopy.setEnabled(true);
			return null;
		} catch (/*IOException |*/ RuntimeException e) {
			setEnabledWIFI(false, false);
			return getExtendedName(d) + ": " + e.getMessage();
		}
	}
	
	@Override
	public void hiding() {
		if(selDialog != null) selDialog.dispose();
	}

	@Override
	public String apply() {
		final boolean enabled = chckbxEnabled.isSelected();
		final String ssid = textFieldSSID.getText().trim();
		final String pwd = new String(textFieldPwd.getPassword()).trim();
		final boolean dhcp = rdbtnDHCP.isSelected();
		final String ip = textFieldStaticIP.getText().trim();
		final String gw = textFieldGateway.getText().trim();
		final String netmask = textFieldNetmask.getText().trim();
		final String dns = textFieldDNS.getText().trim();
		if(enabled) {
			// Validation
			if(ip.isEmpty() == false && ip.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgWrongIP"));
			}
			if(gw.isEmpty() == false && gw.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgWrongGW"));
			}
			if(netmask.isEmpty() == false && netmask.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgWrongMask"));
			}
			if(dns.isEmpty() == false && dns.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgWrongDNS"));
			}
			if(dhcp == false && devices.size() == 1 && ip.isEmpty()) {
				throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgObbStaticIP"));
			}
			if(dhcp == false && (netmask.isEmpty() || gw.isEmpty())) {
				throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgObbStaticMaskGW"));
			}
			if(ssid.isEmpty() || pwd.isEmpty()) {
				throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgObbSSID"));
			}
		}
		String res = "<html>";
		for(int i = 0; i < devices.size(); i++) {
			WIFIManager wfManager = fwModule.get(i);
			if(wfManager != null) { // wfManager == null excluded device
				String msg = null;
				if(enabled) {
					if(dhcp) {
						msg = wfManager.set(ssid, pwd);
					} else {
						msg = wfManager.set(ssid, pwd, ip, netmask, gw, dns);
					}
				} else {
					msg = wfManager.disable();
				}
				if(msg != null) {
					res += String.format(LABELS.getString("dlgSetMultiMsgFail"), devices.get(i).getHostname()) + " (" + msg + ")<br>";
				} else {
					res += String.format(LABELS.getString("dlgSetMultiMsgOk"), devices.get(i).getHostname()) + "<br>";
				}
			}
		}
		try {
			showing();
		} catch (InterruptedException e) {}
		return res;
	}

	@Override
	public void update(ShellyAbstractDevice device, Object dummy) {
		try {
			WIFIManager m = device.getWIFIManager(Network.SECONDARY);
			chckbxEnabled.setSelected(m.isEnabled());
			textFieldSSID.setText(m.getSSID());
			rdbtnStaticIP.setSelected(m.isStaticIP());
			rdbtnDHCP.setSelected(m.isStaticIP() == false);
			textFieldGateway.setText(m.getGateway());
			textFieldNetmask.setText(m.getMask());
			textFieldDNS.setText(m.getDNS());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}