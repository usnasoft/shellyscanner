package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.LoginManagerG2;
import it.usna.shellyscan.view.devsettings.DialogDeviceSettings.Gen;
import it.usna.shellyscan.view.util.UtilCollecion;

public class PanelResLogin extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	private JCheckBox chckbxEnabled = new JCheckBox(/*"", true*/);
	private JTextField textFieldUser;
	private JPasswordField textFieldPwd;
	private JCheckBox chckbxShowPwd;
	private char pwdEchoChar;
	private Gen types;
	private List<LoginManager> loginModule = new ArrayList<>();

	public PanelResLogin(List<ShellyAbstractDevice> devices) {
		super(devices);
		types = DialogDeviceSettings.getTypes(devices);
		setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0};
		gridBagLayout.rowHeights = new int[] {0, 0, 0, 0, 30};
		gridBagLayout.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(Main.LABELS.getString("dlgSetEnabled"));
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

		JLabel lblNewLabel_1 = new JLabel(Main.LABELS.getString("labelUser"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		textFieldUser = new JTextField();
		GridBagConstraints gbc_textFieldSSID = new GridBagConstraints();
		gbc_textFieldSSID.gridwidth = 3;
		gbc_textFieldSSID.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldSSID.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldSSID.gridx = 1;
		gbc_textFieldSSID.gridy = 1;
		add(textFieldUser, gbc_textFieldSSID);
		//		textFieldUser.setColumns(10);

		if(types != Gen.G1) {
			textFieldUser.setEnabled(false);
			textFieldUser.setText(LoginManagerG2.LOGIN_USER);
		}

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
		gbc_textFieldPwd.gridwidth = 3;
		gbc_textFieldPwd.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPwd.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPwd.gridx = 1;
		gbc_textFieldPwd.gridy = 2;
		add(textFieldPwd, gbc_textFieldPwd);
		//		textFieldPwd.setColumns(10);

		chckbxShowPwd = new JCheckBox(Main.LABELS.getString("labelShowPwd"));
		chckbxShowPwd.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 3;
		add(chckbxShowPwd, gbc_chckbxNewCheckBox);

		chckbxEnabled.addItemListener(event -> setEnabledLogin(event.getStateChange() == java.awt.event.ItemEvent.SELECTED));
		chckbxShowPwd.addItemListener(e -> textFieldPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar));
	}

	private void setEnabledLogin(boolean enabled) {
		textFieldUser.setEnabled(enabled && types == Gen.G1);
		textFieldPwd.setEnabled(enabled);
		chckbxShowPwd.setEnabled(enabled);
	}

	@Override
	public String showing() throws InterruptedException {
		String exclude = "<html>";
		int excludeCount = 0;
		loginModule.clear();
		ShellyAbstractDevice d = null;
		try {
			chckbxEnabled.setEnabled(false);
			setEnabledLogin(false);
			boolean enabledGlobal = false;
			String userGlobal = "";
			boolean first = true;
			for(int i = 0; i < devices.size(); i++) {
				try {
					d = devices.get(i);
					LoginManager lm = d.getLoginManager();
					if(Thread.interrupted()) {
						throw new InterruptedException();
					}
					boolean enabled = lm.isEnabled();
					String user = lm.getUser();
					if(first) {
						enabledGlobal = enabled;
						userGlobal = user;
						first = false;
					} else {
						if(enabled != enabledGlobal) enabledGlobal = false;
						if(user.equals(userGlobal) == false) userGlobal = "";
					}
					loginModule.add(lm);
				} catch(IOException | RuntimeException e) {
					loginModule.add(null);
					if(excludeCount > 0) {
						exclude += "<br>";
					}
					exclude += UtilCollecion.getFullName(d);
					excludeCount++;
				}
			}
			chckbxEnabled.setSelected(enabledGlobal);
			if(types == Gen.G1) {
				textFieldUser.setText(userGlobal);
			}
//			if(Thread.interrupted()) {
//				throw new InterruptedException();
//			}
			if(excludeCount == devices.size()) {
				return LABELS.getString("msgAllDevicesExcluded");
			} else if (excludeCount > 0) {
				JOptionPane.showMessageDialog(this, exclude, LABELS.getString("dlgExcludedDevicesTitle"), JOptionPane.WARNING_MESSAGE);
			}
			chckbxEnabled.setEnabled(true); // form is active
			setEnabledLogin(enabledGlobal);
			return null;
		} catch (RuntimeException e) {
			e.printStackTrace();
			return getExtendedName(d) + ": " + e.getMessage();
		}
	}

	@Override
	public String apply() {
		// Validation
		final boolean enabled = chckbxEnabled.isSelected();
		final String user = textFieldUser.getText().trim();
		final char[] pwd = textFieldPwd.getPassword();
		if(enabled && (user.length() == 0 || pwd.length == 0)) {
			throw new IllegalArgumentException(Main.LABELS.getString("dlgSetMsgObbUser"));
		}
		CredentialsProvider credsProvider = null;
		if(enabled) {
			credsProvider = LoginManager.getCredentialsProvider(user, pwd);
		}
		String res = "<html>";
		for(int i=0; i < devices.size(); i++) {
			String msg;
			LoginManager lm = loginModule.get(i);
			if(lm != null ) {
				if(enabled) {
					msg = lm.set(user, pwd, credsProvider);
				} else {
					msg = lm.disable();
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
} //194