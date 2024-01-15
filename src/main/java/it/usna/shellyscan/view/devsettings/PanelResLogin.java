package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.LoginManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g2.LoginManagerG2;
import it.usna.shellyscan.view.devsettings.DialogDeviceSettings.Gen;

public class PanelResLogin extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	private JCheckBox chckbxEnabled = new JCheckBox();
	private JTextField textFieldUser = new JTextField();
	private JPasswordField textFieldPwd = new JPasswordField();;
	private JCheckBox chckbxShowPwd;
	private char pwdEchoChar;
	private final Gen types;
	private ArrayList<LoginManager> loginModule = new ArrayList<>();

	public PanelResLogin(DialogDeviceSettings parent, Gen types) {
		super(parent);
		this.types = types;
		setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0};
		gridBagLayout.rowHeights = new int[] {0, 0, 0, 0, 30};
		gridBagLayout.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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

		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("labelUser"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		add(lblNewLabel_1, gbc_lblNewLabel_1);

		GridBagConstraints gbc_textFieldSSID = new GridBagConstraints();
		gbc_textFieldSSID.gridwidth = 3;
		gbc_textFieldSSID.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldSSID.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldSSID.gridx = 1;
		gbc_textFieldSSID.gridy = 1;
		add(textFieldUser, gbc_textFieldSSID);

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

		pwdEchoChar = textFieldPwd.getEchoChar();
		GridBagConstraints gbc_textFieldPwd = new GridBagConstraints();
		gbc_textFieldPwd.gridwidth = 3;
		gbc_textFieldPwd.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPwd.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPwd.gridx = 1;
		gbc_textFieldPwd.gridy = 2;
		add(textFieldPwd, gbc_textFieldPwd);

		chckbxShowPwd = new JCheckBox(LABELS.getString("labelShowPwd"));
		chckbxShowPwd.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 3;
		add(chckbxShowPwd, gbc_chckbxNewCheckBox);

		chckbxEnabled.addItemListener(event -> setEnabledLogin(event.getStateChange() == ItemEvent.SELECTED));
		chckbxShowPwd.addItemListener(event -> textFieldPwd.setEchoChar((event.getStateChange() == ItemEvent.SELECTED) ? '\0' : pwdEchoChar));
	}

	private void setEnabledLogin(boolean enabled) {
		textFieldUser.setEnabled(enabled && types == Gen.G1);
		textFieldPwd.setEnabled(enabled);
		chckbxShowPwd.setEnabled(enabled);
	}

	@Override
	public String showing() throws InterruptedException {
		loginModule.clear();
		chckbxEnabled.setEnabled(false);
		setEnabledLogin(false);
		boolean enabledGlobal = false;
		String userGlobal = "";
		boolean first = true;
		for(int i = 0; i < parent.getLocalSize(); i++) {
			try {
				ShellyAbstractDevice d = parent.getLocalDevice(i);
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
			} catch(IOException | RuntimeException e) { // UnsupportedOperationException (RuntimeException) for GhostDevice
				loginModule.add(null);
			}
		}
		chckbxEnabled.setSelected(enabledGlobal);
		if(types == Gen.G1) {
			textFieldUser.setText(userGlobal);
		}
		chckbxEnabled.setEnabled(true); // form is active
		setEnabledLogin(enabledGlobal);
		return null;
	}

	@Override
	public String apply() {
		// Validation
		final boolean enabled = chckbxEnabled.isSelected();
		final String user = textFieldUser.getText().trim();
		final char[] pwd = textFieldPwd.getPassword();
		if(enabled && (user.length() == 0 || pwd.length == 0)) {
			throw new IllegalArgumentException(LABELS.getString("dlgSetMsgObbUser"));
		}
		String res = "<html>";
		for(int i = 0; i < parent.getLocalSize(); i++) {
			final ShellyAbstractDevice device = parent.getLocalDevice(i);
			final LoginManager lm = loginModule.get(i);
			if(lm != null) {
				String msg;
				if(enabled) {
					msg = lm.set(user, pwd);
				} else {
					msg = lm.disable();
				}
				if(msg != null) {
					if(LABELS.containsKey(msg)) {
						msg = LABELS.getString(msg);
					}
					res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + " (" + msg + ")<br>";
				} else {
					res += String.format(LABELS.getString("dlgSetMultiMsgOk"), device.getHostname()) + "<br>";
				}
			} else if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
				res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
				DeferrablesContainer dc = DeferrablesContainer.getInstance();
				String taskDescription = LABELS.getString("dlgSetRestrictedLogin");
				int existingIndex = dc.indexOf(parent.getModelIndex(i), taskDescription);
				if(existingIndex >= 0) {
					dc.cancel(existingIndex);
				}
				dc.add(parent.getModelIndex(i), taskDescription, (def, dev) -> {
					final LoginManager loginManager = dev.getLoginManager();
					if(enabled) {
						return loginManager.set(user, pwd);
					} else {
						return loginManager.disable();
					}
				});
			} else {
				res += String.format(LABELS.getString("dlgSetMultiMsgExclude"), device.getHostname()) + "<br>";
			}
		}
		try {
			showing();
		} catch (InterruptedException e) {}
		return res;
	}
} // 213