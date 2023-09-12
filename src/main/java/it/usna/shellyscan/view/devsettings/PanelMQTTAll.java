package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.device.MQTTManager;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.DialogDeviceSelection;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.UsnaEventListener;

//https://shelly-api-docs.shelly.cloud/gen2/Components/SystemComponents/Mqtt
//https://shelly-api-docs.shelly.cloud/gen1/#settings
public class PanelMQTTAll extends AbstractSettingsPanel implements UsnaEventListener<ShellyAbstractDevice, Future<?>> {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(PanelMQTTAll.class);
	
	private char pwdEchoChar;
	private JCheckBox chckbxEnabled = new JCheckBox();
	private JTextField textFieldServer;
	private JPasswordField textFieldPwd;
	private JCheckBox chckbxShowPwd;
	private JTextField textFieldUser;
	private JTextField textFieldID;
	private JCheckBox chckbxNoPWD;
	private JCheckBox chckbxDefaultPrefix;
	private List<MQTTManager> mqttModule = new ArrayList<>();
	
	private JButton btnCopy = new JButton(LABELS.getString("btnCopyFrom"));
	private DialogDeviceSelection selDialog = null;

	public PanelMQTTAll(DialogDeviceSettings owner) {
		super(owner);
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {10, 0, 30, 0, 0};
		contentPanel.setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgSetEnabled"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		contentPanel.add(lblNewLabel, gbc_lblNewLabel);

		GridBagConstraints gbc_chckbxEnabled = new GridBagConstraints();
		gbc_chckbxEnabled.weightx = 1.0;
		gbc_chckbxEnabled.gridwidth = 3;
		gbc_chckbxEnabled.anchor = GridBagConstraints.WEST;
		gbc_chckbxEnabled.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxEnabled.gridx = 1;
		gbc_chckbxEnabled.gridy = 0;
		chckbxEnabled.setHorizontalAlignment(SwingConstants.LEFT);
		contentPanel.add(chckbxEnabled, gbc_chckbxEnabled);
		
		GridBagConstraints gbc_btnCopy = new GridBagConstraints();
		gbc_btnCopy.anchor = GridBagConstraints.EAST;
		gbc_btnCopy.insets = new Insets(0, 0, 5, 0);
		gbc_btnCopy.gridx = 4;
		gbc_btnCopy.gridy = 0;
		contentPanel.add(btnCopy, gbc_btnCopy);
		btnCopy.addActionListener(e -> selDialog = new DialogDeviceSelection(owner, this, parent.getModel()));

		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgSetServer"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

		textFieldServer = new JTextField();
		GridBagConstraints gbc_textFieldServer = new GridBagConstraints();
		gbc_textFieldServer.gridwidth = 4;
		gbc_textFieldServer.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldServer.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldServer.gridx = 1;
		gbc_textFieldServer.gridy = 1;
		contentPanel.add(textFieldServer, gbc_textFieldServer);
		textFieldServer.setColumns(10);

		JLabel lblNewLabel_8 = new JLabel(LABELS.getString("dlgSetUser"));
		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_8.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_8.gridx = 0;
		gbc_lblNewLabel_8.gridy = 2;
		contentPanel.add(lblNewLabel_8, gbc_lblNewLabel_8);

		textFieldUser = new JTextField();
		GridBagConstraints gbc_textFieldUser = new GridBagConstraints();
		gbc_textFieldUser.gridwidth = 4;
		gbc_textFieldUser.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldUser.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldUser.gridx = 1;
		gbc_textFieldUser.gridy = 2;
		contentPanel.add(textFieldUser, gbc_textFieldUser);
		textFieldUser.setColumns(10);

		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("labelPassword"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 3;
		contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);

		textFieldPwd = new JPasswordField();
		pwdEchoChar = textFieldPwd.getEchoChar();
		GridBagConstraints gbc_textFieldPwd = new GridBagConstraints();
		gbc_textFieldPwd.gridwidth = 4;
		gbc_textFieldPwd.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPwd.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPwd.gridx = 1;
		gbc_textFieldPwd.gridy = 3;
		contentPanel.add(textFieldPwd, gbc_textFieldPwd);
		textFieldPwd.setColumns(10);

		chckbxShowPwd = new JCheckBox(LABELS.getString("labelShowPwd"));
		chckbxShowPwd.addItemListener(e -> textFieldPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar));
		setLayout(new BorderLayout(0, 0));
		GridBagConstraints gbc_chckbxSPwd = new GridBagConstraints();
		gbc_chckbxSPwd.gridwidth = 3;
		gbc_chckbxSPwd.anchor = GridBagConstraints.WEST;
		gbc_chckbxSPwd.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxSPwd.gridx = 1;
		gbc_chckbxSPwd.gridy = 4;
		contentPanel.add(chckbxShowPwd, gbc_chckbxSPwd);

		chckbxNoPWD = new JCheckBox(LABELS.getString("labelNoPwd"));
		GridBagConstraints gbc_chckbxNoPWD = new GridBagConstraints();
		gbc_chckbxNoPWD.anchor = GridBagConstraints.WEST;
		gbc_chckbxNoPWD.insets = new Insets(0, 10, 5, 0);
		gbc_chckbxNoPWD.gridx = 4;
		gbc_chckbxNoPWD.gridy = 4;
		contentPanel.add(chckbxNoPWD, gbc_chckbxNoPWD);

		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgSetMqttId"));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 5;
		contentPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);

		textFieldID = new JTextField();
		GridBagConstraints gbc_textFieldID = new GridBagConstraints();
		gbc_textFieldID.gridwidth = 4;
		gbc_textFieldID.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldID.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldID.gridx = 1;
		gbc_textFieldID.gridy = 5;
		contentPanel.add(textFieldID, gbc_textFieldID);
		textFieldID.setColumns(10);

		chckbxDefaultPrefix = new JCheckBox(LABELS.getString("dlgSetMqttIdDefault"));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 6;
		contentPanel.add(chckbxDefaultPrefix, gbc_chckbxNewCheckBox);

		JLabel lblNewLabel_12 = new JLabel(LABELS.getString("dlgSetMsgMqttReboot"));
		lblNewLabel_12.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_12.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_12 = new GridBagConstraints();
		gbc_lblNewLabel_12.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_12.weighty = 1.0;
		gbc_lblNewLabel_12.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_12.gridwidth = 4;
		gbc_lblNewLabel_12.gridx = 1;
		gbc_lblNewLabel_12.gridy = 7;
		contentPanel.add(lblNewLabel_12, gbc_lblNewLabel_12);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);
		scrollPane.setViewportView(contentPanel);
		add(scrollPane, BorderLayout.CENTER);

		chckbxDefaultPrefix.addItemListener(event -> textFieldID.setEnabled(event.getStateChange() != java.awt.event.ItemEvent.SELECTED  && owner.getLocalSize() == 1));
		chckbxEnabled.addItemListener(event -> setEnabledMQTT(event.getStateChange() == java.awt.event.ItemEvent.SELECTED, owner.getLocalSize() == 1));
		chckbxNoPWD.addItemListener(event -> setPasswordRequired(event.getStateChange() == java.awt.event.ItemEvent.DESELECTED));
	}

	private void setPasswordRequired(boolean pwdRequired) {
		textFieldPwd.setEnabled(pwdRequired);
		textFieldUser.setEnabled(pwdRequired);
		chckbxShowPwd.setEnabled(pwdRequired);
	}

	private void setEnabledMQTT(boolean enabled, boolean single) {
		textFieldServer.setEnabled(enabled);
		textFieldUser.setEnabled(enabled);
		textFieldPwd.setEnabled(enabled);
		chckbxShowPwd.setEnabled(enabled);
		chckbxNoPWD.setEnabled(enabled /*&& DialogDeviceSettings.getTypes(devices) == DialogDeviceSettings.Gen.G2*/);
		textFieldID.setEnabled(enabled && chckbxDefaultPrefix.isSelected() == false && single);
		chckbxDefaultPrefix.setEnabled(enabled);

		setPasswordRequired(enabled && chckbxNoPWD.isSelected() == false);
	}

	@Override
	public String showing() throws InterruptedException {
		mqttModule.clear();
		ShellyAbstractDevice d = null;
		String exclude = "<html>" + LABELS.getString("dlgExcludedDevicesMsg");
		int excludeCount = 0;
		try {
			btnCopy.setEnabled(false);
			chckbxEnabled.setEnabled(false);
			setEnabledMQTT(false, false); // disable while checking
			boolean enabledGlobal = false;
			String serverGlobal = "";
			String userGlobal = "";
			String idGlobal = "";
			boolean noPwdGlobal = false;
			boolean first = true;
			for(int i = 0; i < parent.getLocalSize(); i++) {
				try {
					d = parent.getLocalDevice(i);
					MQTTManager mqttm = d.getMQTTManager();
					if(Thread.interrupted()) {
						throw new InterruptedException();
					}
					boolean enabled = mqttm.isEnabled();
					String server = mqttm.getServer();
					String user = mqttm.getUser();
					String id = mqttm.getPrefix();
					if(first) {
						enabledGlobal = enabled;
						serverGlobal = server;
						userGlobal = user;
						idGlobal = id;
						noPwdGlobal = user.isEmpty();
						first = false;
					} else {
						if(enabled != enabledGlobal) enabledGlobal = false;
						if(server.equals(serverGlobal) == false) serverGlobal = "";
						if(user.equals(userGlobal) == false) userGlobal = "";
						if(id.equals(idGlobal) == false) idGlobal = "";
						noPwdGlobal &= user.isEmpty();
					}
					mqttModule.add(mqttm);
				} catch(IOException | RuntimeException e) {
					mqttModule.add(null);
					exclude += "<br>" + UtilMiscellaneous.getFullName(d);
					excludeCount++;
				}
			}
//			if(Thread.interrupted()) {
//				throw new InterruptedException();
//			}
			if(excludeCount == parent.getLocalSize()) {
				return LABELS.getString("msgAllDevicesExcluded");
			} else if (excludeCount > 0) {
				Msg.showHtmlMessageDialog(this, exclude, LABELS.getString("dlgExcludedDevicesTitle"), JOptionPane.WARNING_MESSAGE);
			}
			chckbxEnabled.setEnabled(true); // form is active
			chckbxEnabled.setSelected(enabledGlobal);
			textFieldServer.setText(serverGlobal);
			textFieldUser.setText(userGlobal);
			chckbxNoPWD.setSelected(noPwdGlobal);
			textFieldID.setText(idGlobal);

			setEnabledMQTT(enabledGlobal, parent.getLocalSize() == 1);
			btnCopy.setEnabled(true);
			return null;
		} catch (RuntimeException e) {
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
		final String server = textFieldServer.getText().trim();
		String user = textFieldUser.getText().trim();
		String pwd = new String(textFieldPwd.getPassword()).trim();
		if(enabled) {
			if(chckbxNoPWD.isSelected()) {
				user = pwd = null;
			}
			// Validation
			if(server.isEmpty()) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgMqttServer"));
			}
			if(chckbxNoPWD.isSelected() == false && (user.isEmpty() || pwd.isEmpty())) {
				throw new IllegalArgumentException(LABELS.getString("dlgSetMsgMqttUser"));
			}
		}
		String res = "<html>";
		for(int i=0; i < parent.getLocalSize(); i++) {
			String msg;
			MQTTManager mqttM = mqttModule.get(i);
			if(mqttM != null) {
				if(enabled) {
					String prefix ;
					if(chckbxDefaultPrefix.isSelected()) {
						prefix = null;
					} else if(parent.getLocalSize() > 1) {
						prefix = "";
					} else {
						prefix = textFieldID.getText();
					}
					msg = mqttM.set(server, user, pwd, prefix);
				} else {
					msg = mqttM.disable();
				}
				if(msg != null) {
					res += String.format(LABELS.getString("dlgSetMultiMsgFail"), parent.getLocalDevice(i).getHostname()) + " (" + msg + ")<br>";
				} else {
					res += String.format(LABELS.getString("dlgSetMultiMsgOk"), parent.getLocalDevice(i).getHostname()) + "<br>";
				}
			}
		}
		try {
			showing();
		} catch (InterruptedException e) {}
		return res;
	}
	
	@Override
	public void update(ShellyAbstractDevice device, Future<?> future) {
		if(future.isCancelled() == false) {
			try {
				MQTTManager m = device.getMQTTManager();
				chckbxEnabled.setSelected(m.isEnabled());
				textFieldServer.setText(m.getServer());
				textFieldUser.setText(m.getUser());
			} catch (IOException e) {
				LOG.error("copy", e);
			}
		}
	}
} // 370