package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.WIFIManager;
import it.usna.shellyscan.view.DialogDeviceSelection;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.UsnaEventListener;

public class PanelWIFI extends AbstractSettingsPanel implements UsnaEventListener<ShellyAbstractDevice, Future<?>> {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(PanelWIFI.class);
	
	private final WIFIManager.Network netModule;
	private final JCheckBox chckbxEnabled = new JCheckBox();
	private final JTextField textFieldSSID = new JTextField();
	private final JPasswordField textFieldPwd = new JPasswordField();
	private final JCheckBox chckbxShowPwd = new JCheckBox(LABELS.getString("labelShowPwd"));;
	private final JTextField textFieldStaticIP = new JTextField();
	private final JTextField textFieldNetmask = new JTextField();
	private final JTextField textFieldGateway = new JTextField();
	private final JTextField textFieldDNS = new JTextField();;
	private final JRadioButton rdbtnDHCP = new JRadioButton(LABELS.getString("dlgSetEnabled"));
	private final JRadioButton rdbtnStaticIP = new JRadioButton(LABELS.getString("dlgSetStatic"));
	private final JRadioButton rdbtnDhcpNoChange = new JRadioButton(LABELS.getString("dlgSetDoNotChange"));
	private final char pwdEchoChar;
	private final List<WIFIManager> fwModule = new ArrayList<>();

	private final static String IPV4_REGEX = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
	private JButton btnCopy = new JButton(LABELS.getString("btnCopyFrom"));
//	private JButton btnCopyTo;
	private DialogDeviceSelection selDialog = null;

	public PanelWIFI(DialogDeviceSettings owner, WIFIManager.Network net) {
		super(owner);
		this.netModule = net;
		setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgSetEnabled"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);

		GridBagConstraints gbc_chckbxEnabled = new GridBagConstraints();
		gbc_chckbxEnabled.anchor = GridBagConstraints.WEST;
		gbc_chckbxEnabled.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxEnabled.gridx = 1;
		gbc_chckbxEnabled.gridy = 0;
		add(chckbxEnabled, gbc_chckbxEnabled);
		
//		btnCopyTo = new JButton(LABELS.getString(net == WIFIManager.Network.PRIMARY ? "dlgSetWIFI1to2" : "dlgSetWIFI2to1"));
//		GridBagConstraints gbc_btnCopyTo = new GridBagConstraints();
//		gbc_btnCopyTo.anchor = GridBagConstraints.EAST;
//		gbc_btnCopyTo.weightx = 1.0;
//		gbc_btnCopyTo.insets = new Insets(0, 0, 5, 5);
//		gbc_btnCopyTo.gridx = 3;
//		gbc_btnCopyTo.gridy = 0;
//		add(btnCopyTo, gbc_btnCopyTo);
//		btnCopyTo.addActionListener(e -> copyTo(net == WIFIManager.Network.PRIMARY ? WIFIManager.Network.SECONDARY : WIFIManager.Network.PRIMARY));

		GridBagConstraints gbc_btnCopy = new GridBagConstraints();
		gbc_btnCopy.anchor = GridBagConstraints.EAST;
		gbc_btnCopy.insets = new Insets(0, 0, 5, 0);
		gbc_btnCopy.gridx = 4;
		gbc_btnCopy.gridy = 0;
		add(btnCopy, gbc_btnCopy);
		btnCopy.addActionListener(e -> selDialog = new DialogDeviceSelection(owner, this, parent.getModel()));

		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgSetSSID"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		GridBagConstraints gbc_textFieldSSID = new GridBagConstraints();
		gbc_textFieldSSID.weightx = 1.0;
		gbc_textFieldSSID.gridwidth = 4;
		gbc_textFieldSSID.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldSSID.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldSSID.gridx = 1;
		gbc_textFieldSSID.gridy = 1;
		add(textFieldSSID, gbc_textFieldSSID);
		textFieldSSID.setColumns(10);

		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("labelPassword"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 2;
		add(lblNewLabel_2, gbc_lblNewLabel_2);

		pwdEchoChar = textFieldPwd.getEchoChar();
		GridBagConstraints gbc_textFieldPwd = new GridBagConstraints();
		gbc_textFieldPwd.weightx = 1.0;
		gbc_textFieldPwd.gridwidth = 4;
		gbc_textFieldPwd.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPwd.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPwd.gridx = 1;
		gbc_textFieldPwd.gridy = 2;
		add(textFieldPwd, gbc_textFieldPwd);
		textFieldPwd.setColumns(10);

		chckbxShowPwd.addItemListener(e -> textFieldPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.gridwidth = 2;
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 3;
		add(chckbxShowPwd, gbc_chckbxNewCheckBox);

		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgSetDHCP"));
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
		gbc_rdbtnStaticIP.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnStaticIP.gridx = 2;
		gbc_rdbtnStaticIP.gridy = 4;
		add(rdbtnStaticIP, gbc_rdbtnStaticIP);
		
		GridBagConstraints gbc_rdbtnDhcpNoChange = new GridBagConstraints();
		gbc_rdbtnDhcpNoChange.anchor = GridBagConstraints.WEST;
		gbc_rdbtnDhcpNoChange.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnDhcpNoChange.gridx = 3;
		gbc_rdbtnDhcpNoChange.gridy = 4;
		add(rdbtnDhcpNoChange, gbc_rdbtnDhcpNoChange);

		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("dlgSetIP"));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 5;
		add(lblNewLabel_4, gbc_lblNewLabel_4);

		GridBagConstraints gbc_textFieldStaticIP = new GridBagConstraints();
		gbc_textFieldStaticIP.gridwidth = 2;
		gbc_textFieldStaticIP.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldStaticIP.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldStaticIP.gridx = 1;
		gbc_textFieldStaticIP.gridy = 5;
		add(textFieldStaticIP, gbc_textFieldStaticIP);
		textFieldStaticIP.setColumns(16);

		JLabel lblNewLabel_5 = new JLabel(LABELS.getString("dlgSetNetmask"));
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 6;
		add(lblNewLabel_5, gbc_lblNewLabel_5);

		GridBagConstraints gbc_textFieldNetmask = new GridBagConstraints();
		gbc_textFieldNetmask.gridwidth = 2;
		gbc_textFieldNetmask.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldNetmask.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldNetmask.gridx = 1;
		gbc_textFieldNetmask.gridy = 6;
		add(textFieldNetmask, gbc_textFieldNetmask);
		textFieldNetmask.setColumns(16);

		JLabel lblNewLabel_6 = new JLabel(LABELS.getString("dlgSetGateway"));
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 7;
		add(lblNewLabel_6, gbc_lblNewLabel_6);

		GridBagConstraints gbc_textFieldGateway = new GridBagConstraints();
		gbc_textFieldGateway.gridwidth = 2;
		gbc_textFieldGateway.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldGateway.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldGateway.gridx = 1;
		gbc_textFieldGateway.gridy = 7;
		add(textFieldGateway, gbc_textFieldGateway);
		textFieldGateway.setColumns(16);

		JLabel lblNewLabel_7 = new JLabel(LABELS.getString("dlgSetDNS"));
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 8;
		add(lblNewLabel_7, gbc_lblNewLabel_7);

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
		dhcpStatic.add(rdbtnDhcpNoChange);
		rdbtnDHCP.addItemListener(event -> {
			if(event.getStateChange() == java.awt.event.ItemEvent.SELECTED) setEnabledStaticIP(false);
		});
		rdbtnStaticIP.addItemListener(event -> {
			if(event.getStateChange() == java.awt.event.ItemEvent.SELECTED) setEnabledStaticIP(true);
		});
		rdbtnDhcpNoChange.addItemListener(event -> {
			if(event.getStateChange() == java.awt.event.ItemEvent.SELECTED) setEnabledStaticIP(null);
		});

		chckbxEnabled.addItemListener(event -> setEnabledWIFI(event.getStateChange() == java.awt.event.ItemEvent.SELECTED, rdbtnDhcpNoChange.isSelected() ? null : rdbtnStaticIP.isSelected()));
	}

	private void setEnabledWIFI(boolean enabled, Boolean staticIP) {
		textFieldSSID.setEnabled(enabled);
		textFieldPwd.setEnabled(enabled);
		chckbxShowPwd.setEnabled(enabled);
		rdbtnDHCP.setEnabled(enabled /*&& devices.size() == 1*/);
		rdbtnStaticIP.setEnabled(enabled && (staticIP == Boolean.TRUE || parent.getLocalSize() == 1));
		rdbtnDhcpNoChange.setEnabled(enabled);
		setEnabledStaticIP((staticIP == null || staticIP) && enabled);
	}

	private void setEnabledStaticIP(Boolean staticIP) {
		textFieldStaticIP.setEnabled(staticIP == Boolean.TRUE && parent.getLocalSize() == 1);
		textFieldNetmask.setEnabled(staticIP == null || staticIP == Boolean.TRUE);
		textFieldGateway.setEnabled(staticIP == null || staticIP == Boolean.TRUE);
		textFieldDNS.setEnabled(staticIP == null || staticIP == Boolean.TRUE);
	}
	
//	private void copyTo(WIFIManager.Network netFrom) {
//		if(JOptionPane.showConfirmDialog(this, LABELS.getString("dlgSetConfirmWIFI"), LABELS.getString("dlgSetWIFI"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
//			String res = "<html>";
//			ShellyAbstractDevice d = null;
//			for(int i = 0; i < devices.size(); i++) {
//				try {
//					String msg = null;
//					d = devices.get(i);
//					WIFIManager wfManagerTo = fwModule.get(i);
//					WIFIManager wfManagerFrom = d.getWIFIManager(netFrom);
//					wfManagerFrom.getSSID();
//					
////					DialogAuthentication credentials = new DialogAuthentication(this,
////							LABELS.getString("dlgSetWIFI"), LABELS.getString("dlgSetSSID"), LABELS.getString("labelPassword"), LABELS.getString("labelConfPassword"));
////					credentials.setUser(test.get(ShellyAbstractDevice.Restore.RESTORE_WI_FI1));
////					credentials.setMessage(LABELS.getString("msgRestoreEnterWIFI1"));
////					credentials.editableUser(false);
////					credentials.setVisible(true);
//////					if(credentials.getUser() != null) {
//////						resData.put(ShellyAbstractDevice.Restore.RESTORE_WI_FI1, new String(credentials.getPassword()));
//////					}
////					credentials.dispose();
//					
//					if(wfManagerTo != null) {
//						msg = wfManagerTo.copyFrom(wfManagerFrom, null); // todo
//					}
//					if(msg != null) {
//						res += String.format(LABELS.getString("dlgSetMultiMsgFail"), d.getHostname()) + " (" + msg + ")<br>";
//					} else {
//						res += String.format(LABELS.getString("dlgSetMultiMsgOk"), d.getHostname()) + "<br>";
//					}
//				} catch (IOException e) {
//					res += String.format(LABELS.getString("dlgSetMultiMsgFail"), d.getHostname()) + "<br>";
////					e.printStackTrace();
//				}
//			}
//		}
//	}
	
	@Override
	public String showing() throws InterruptedException {
		return fill(true);
	}

	private String fill(boolean showExcluded) throws InterruptedException {
		String exclude = "<html>" + LABELS.getString("dlgExcludedDevicesMsg");
		int excludeCount = 0;
		ShellyAbstractDevice d = null;
		fwModule.clear();
		try {
			rdbtnDhcpNoChange.setVisible(false);
			btnCopy.setEnabled(false);
//			btnCopyTo.setEnabled(false);
			chckbxEnabled.setEnabled(false);
			setEnabledWIFI(false, false); // disable while checking
			boolean enabledGlobal = false;
			String ssidGlobal = "";
			Boolean staticIPGlobal = false;
			String globalIP = "";
			String globalNetmask = "";
			String globalGW = "";
			String globalDNS = "";
			boolean first = true;
			for(int i = 0; i < parent.getLocalSize(); i++) {
				d = parent.getLocalDevice(i);
				try {
					WIFIManager sta = d.getWIFIManager(netModule);
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
						if(staticIPGlobal != null && staticIP != staticIPGlobal) staticIPGlobal = null;
						if(ip.equals(globalIP) == false) globalIP = "";
						if(netmask.equals(globalNetmask) == false) globalNetmask = "";
						if(gw.equals(globalGW) == false) globalGW = "";
						if(dns.equals(globalDNS) == false) globalDNS = "";
					}
					fwModule.add(sta);
				} catch(IOException | RuntimeException e) { // UnsupportedOperationException (RuntimeException) for GhostDevice
					fwModule.add(null);
					exclude += "<br>" + UtilMiscellaneous.getFullName(d);
					excludeCount++;
//					e.printStackTrace();
				}
			}
			if(showExcluded) {
				if(excludeCount == parent.getLocalSize() && isShowing()) {
					return LABELS.getString("msgAllDevicesExcluded");
				} else if (excludeCount > 0 && isShowing()) {
					Msg.showHtmlMessageDialog(this, exclude, LABELS.getString("dlgExcludedDevicesTitle"), JOptionPane.WARNING_MESSAGE);
				}
			}
			chckbxEnabled.setEnabled(true); // form is active
			chckbxEnabled.setSelected(enabledGlobal);
			textFieldSSID.setText(ssidGlobal);
			rdbtnDhcpNoChange.setSelected(staticIPGlobal == null);
			rdbtnDhcpNoChange.setVisible(staticIPGlobal == null);
			rdbtnDHCP.setSelected(staticIPGlobal == Boolean.FALSE);
			rdbtnStaticIP.setSelected(staticIPGlobal == Boolean.TRUE);
			textFieldStaticIP.setText(globalIP);
			textFieldNetmask.setText(globalNetmask);
			textFieldGateway.setText(globalGW);
			textFieldDNS.setText(globalDNS);			
			setEnabledWIFI(/*chckbxEnabled.isSelected()*/enabledGlobal, /*rdbtnStaticIP.isSelected()*/staticIPGlobal);
			btnCopy.setEnabled(true);
			return null;
		} catch (/*IOException |*/ RuntimeException e) {
			setEnabledWIFI(false, false);
			LOG.error("WI-FI showing", e);
			return UtilMiscellaneous.getFullName(d) + ": " + e.getMessage();
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
		boolean dhcp = rdbtnDHCP.isSelected();
		boolean noChange = rdbtnDhcpNoChange.isSelected();
		String ip = textFieldStaticIP.getText().trim();
		final String gw = textFieldGateway.getText().trim();
		final String netmask = textFieldNetmask.getText().trim();
		final String dns = textFieldDNS.getText().trim();
		if(enabled) {
			// Validation
			if(ip.isEmpty() == false && ip.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgWrongIP"));
			}
			if(gw.isEmpty() == false && gw.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgWrongGW"));
			}
			if(netmask.isEmpty() == false && netmask.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgWrongMask"));
			}
			if(dns.isEmpty() == false && dns.matches(IPV4_REGEX) == false) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgWrongDNS"));
			}
			if(dhcp == false && parent.getLocalSize() == 1 && ip.isEmpty()) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgObbStaticIP"));
			}
			if((dhcp == false || noChange) && (netmask.isEmpty() || gw.isEmpty())) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgObbStaticMaskGW"));
			}
			if(ssid.isEmpty() || pwd.isEmpty()) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgObbSSID"));
			}
		}
		if(JOptionPane.showConfirmDialog(this, LABELS.getString("dlgSetConfirmWIFI"), LABELS.getString("dlgSetWIFI"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
			String res = "<html>";
			for(int i = 0; i < parent.getLocalSize(); i++) {
				WIFIManager wfManager = fwModule.get(i);
				if(wfManager != null) { // wfManager == null excluded device
					String msg = null;
					if(enabled) {
						if(rdbtnDhcpNoChange.isSelected()) {
							dhcp = wfManager.isStaticIP() == false;
						}
						if(dhcp) {
							msg = wfManager.set(ssid, pwd);
						} else {
							if(textFieldStaticIP.isEnabled() == false) { // more than 1 device selected -> keep IP
								ip = wfManager.getIP();
							}
							msg = wfManager.set(ssid, pwd, ip, netmask, gw, dns);
						}
					} else {
						msg = wfManager.disable();
					}
					if(msg != null) {
						res += String.format(LABELS.getString("dlgSetMultiMsgFail"), parent.getLocalDevice(i).getHostname()) + " (" + msg + ")<br>";
					} else {
						res += String.format(LABELS.getString("dlgSetMultiMsgOk"), parent.getLocalDevice(i).getHostname()) + "<br>";
					}
				}
			}
			try {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				fill(false);
			} catch (InterruptedException e) {}
			return res;
		}
		return null;
	}

	@Override
	public void update(ShellyAbstractDevice device, Future<?> future) {
		if(future.isCancelled() == false) {
			try {
				WIFIManager m = device.getWIFIManager(netModule);
				chckbxEnabled.setSelected(m.isEnabled());
				textFieldSSID.setText(m.getSSID());
				if(m.isStaticIP()) {
					if(parent.getLocalSize() == 1) {
						rdbtnStaticIP.setSelected(true);
					} else if(rdbtnDhcpNoChange.isVisible()) {
						rdbtnDhcpNoChange.setSelected(true);
					} else {
						rdbtnDHCP.setSelected(true);
					}
				} else {
					rdbtnDHCP.setSelected(true);
				}
				textFieldGateway.setText(m.getGateway());
				textFieldNetmask.setText(m.getMask());
				textFieldDNS.setText(m.getDNS());
			} catch (IOException e) {
				LOG.warn("copy", e.toString());
			} catch (UnsupportedOperationException e) {
				LOG.debug("copy", e);
			}
		}
	}
}