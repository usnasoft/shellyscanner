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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.modules.MQTTManagerG1;
import it.usna.shellyscan.view.DialogDeviceSelection;
import it.usna.shellyscan.view.util.IntegerTextFieldPanel;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.UsnaEventListener;

//https://shelly-api-docs.shelly.cloud/gen1/#settings
public class PanelMQTTG1 extends AbstractSettingsPanel implements UsnaEventListener<ShellyAbstractDevice, Future<?>> {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(PanelMQTTG1.class);
	
	private char pwdEchoChar;
	private final JCheckBox chckbxEnabled = new JCheckBox();
	private final JTextField textFieldServer = new JTextField();
	private final JPasswordField textFieldPwd = new JPasswordField();
	private final JCheckBox chckbxShowPwd = new JCheckBox(LABELS.getString("labelShowPwd"));
	private final IntegerTextFieldPanel textFieldMaxTimeout = new IntegerTextFieldPanel(0, 65535);
	private final IntegerTextFieldPanel textFieldMinTimeout = new IntegerTextFieldPanel(0, 65535);
	private final IntegerTextFieldPanel textFieldKeepAlive = new IntegerTextFieldPanel(0, 65535);
	private final JTextField textFieldUser = new JTextField();
	private final JTextField textFieldID = new JTextField();
	private final IntegerTextFieldPanel textFieldQOS = new IntegerTextFieldPanel(0, 2);
	private final IntegerTextFieldPanel textFieldUpdatePeriod = new IntegerTextFieldPanel(0, 65535);
	private final JRadioButton rdbtnCleanSessionYes = new JRadioButton(LABELS.getString("true_yna"));
	private final JRadioButton rdbtnCleanSessionNo = new JRadioButton(LABELS.getString("false_yna"));
	private final JRadioButton rdbtnCleanSessionUnchange = new JRadioButton(LABELS.getString("dlgSetDoNotChange"));
	private final JRadioButton rdbtnRetainYes = new JRadioButton(LABELS.getString("true_yna"));
	private final JRadioButton rdbtnRetainNo = new JRadioButton(LABELS.getString("false_yna"));;
	private final JRadioButton rdbtnRetainUnchange = new JRadioButton(LABELS.getString("dlgSetDoNotChange"));
	private final JCheckBox chckbxNoPWD = new JCheckBox(LABELS.getString("labelNoPwd"));
	private final JCheckBox chckbxDefaultPrefix = new JCheckBox(LABELS.getString("dlgSetMqttIdDefault"));
	private final List<MQTTManagerG1> mqttModule = new ArrayList<>();
	
	private JButton btnCopy = new JButton(LABELS.getString("btnCopyFrom"));
	private DialogDeviceSelection selDialog = null;

	public PanelMQTTG1(DialogDeviceSettings owner) {
		super(owner);
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {10, 0, 30, 0, 0};
		//		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		//		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0};
		//		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(LABELS.getString("lblEnabled"));
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

		pwdEchoChar = textFieldPwd.getEchoChar();
		GridBagConstraints gbc_textFieldPwd = new GridBagConstraints();
		gbc_textFieldPwd.gridwidth = 4;
		gbc_textFieldPwd.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPwd.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPwd.gridx = 1;
		gbc_textFieldPwd.gridy = 3;
		contentPanel.add(textFieldPwd, gbc_textFieldPwd);
		textFieldPwd.setColumns(10);

		chckbxShowPwd.addItemListener(e -> textFieldPwd.setEchoChar((e.getStateChange() == java.awt.event.ItemEvent.SELECTED) ? '\0' : pwdEchoChar));
		setLayout(new BorderLayout(0, 0));
		GridBagConstraints gbc_chckbxSPwd = new GridBagConstraints();
		gbc_chckbxSPwd.gridwidth = 3;
		gbc_chckbxSPwd.anchor = GridBagConstraints.WEST;
		gbc_chckbxSPwd.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxSPwd.gridx = 1;
		gbc_chckbxSPwd.gridy = 4;
		contentPanel.add(chckbxShowPwd, gbc_chckbxSPwd);

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

		GridBagConstraints gbc_textFieldID = new GridBagConstraints();
		gbc_textFieldID.gridwidth = 4;
		gbc_textFieldID.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldID.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldID.gridx = 1;
		gbc_textFieldID.gridy = 5;
		contentPanel.add(textFieldID, gbc_textFieldID);
		textFieldID.setColumns(10);

		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.gridwidth = 4;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 0);
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 6;
		contentPanel.add(chckbxDefaultPrefix, gbc_chckbxNewCheckBox);

		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("dlgSetMqttMaxTimeout"));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 7;
		contentPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);

		GridBagConstraints gbc_textFieldMaxTimeout = new GridBagConstraints();
		gbc_textFieldMaxTimeout.gridwidth = 4;
		gbc_textFieldMaxTimeout.anchor = GridBagConstraints.WEST;
		gbc_textFieldMaxTimeout.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldMaxTimeout.gridx = 1;
		gbc_textFieldMaxTimeout.gridy = 7;
		contentPanel.add(textFieldMaxTimeout, gbc_textFieldMaxTimeout);
		textFieldMaxTimeout.setColumns(10);

		JLabel lblNewLabel_5 = new JLabel(LABELS.getString("dlgSetMqttMinTimeout"));
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 8;
		contentPanel.add(lblNewLabel_5, gbc_lblNewLabel_5);

		GridBagConstraints gbc_textFieldMinTimeout = new GridBagConstraints();
		gbc_textFieldMinTimeout.gridwidth = 4;
		gbc_textFieldMinTimeout.anchor = GridBagConstraints.WEST;
		gbc_textFieldMinTimeout.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldMinTimeout.gridx = 1;
		gbc_textFieldMinTimeout.gridy = 8;
		contentPanel.add(textFieldMinTimeout, gbc_textFieldMinTimeout);
		textFieldMinTimeout.setColumns(10);

		JLabel lblNewLabel_6 = new JLabel(LABELS.getString("dlgSetMqttCleanSession"));
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 9;
		contentPanel.add(lblNewLabel_6, gbc_lblNewLabel_6);

		GridBagConstraints gbc_rdbtnCleanSessionYes = new GridBagConstraints();
		gbc_rdbtnCleanSessionYes.anchor = GridBagConstraints.WEST;
		gbc_rdbtnCleanSessionYes.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnCleanSessionYes.gridx = 1;
		gbc_rdbtnCleanSessionYes.gridy = 9;
		contentPanel.add(rdbtnCleanSessionYes, gbc_rdbtnCleanSessionYes);

		GridBagConstraints gbc_rdbtnCleanSessionNo = new GridBagConstraints();
		gbc_rdbtnCleanSessionNo.anchor = GridBagConstraints.WEST;
		gbc_rdbtnCleanSessionNo.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnCleanSessionNo.gridx = 2;
		gbc_rdbtnCleanSessionNo.gridy = 9;
		contentPanel.add(rdbtnCleanSessionNo, gbc_rdbtnCleanSessionNo);

		GridBagConstraints gbc_rdbtnCleanSessionUnchange = new GridBagConstraints();
		gbc_rdbtnCleanSessionUnchange.gridwidth = 2;
		gbc_rdbtnCleanSessionUnchange.weightx = 2.0;
		gbc_rdbtnCleanSessionUnchange.anchor = GridBagConstraints.WEST;
		gbc_rdbtnCleanSessionUnchange.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnCleanSessionUnchange.gridx = 3;
		gbc_rdbtnCleanSessionUnchange.gridy = 9;
		contentPanel.add(rdbtnCleanSessionUnchange, gbc_rdbtnCleanSessionUnchange);

		ButtonGroup cleanSessionRadio = new ButtonGroup();
		cleanSessionRadio.add(rdbtnCleanSessionYes);
		cleanSessionRadio.add(rdbtnCleanSessionNo);
		cleanSessionRadio.add(rdbtnCleanSessionUnchange);

		JLabel lblNewLabel_7 = new JLabel(LABELS.getString("dlgSetMqttKeepAlive"));
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 10;
		contentPanel.add(lblNewLabel_7, gbc_lblNewLabel_7);

		GridBagConstraints gbc_textFieldKeepAlive = new GridBagConstraints();
		gbc_textFieldKeepAlive.gridwidth = 4;
		gbc_textFieldKeepAlive.anchor = GridBagConstraints.WEST;
		gbc_textFieldKeepAlive.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldKeepAlive.gridx = 1;
		gbc_textFieldKeepAlive.gridy = 10;
		contentPanel.add(textFieldKeepAlive, gbc_textFieldKeepAlive);
		textFieldKeepAlive.setColumns(10);

		JLabel lblNewLabel_9 = new JLabel(LABELS.getString("dlgSetMqttMaxQOS"));
		GridBagConstraints gbc_lblNewLabel_9 = new GridBagConstraints();
		gbc_lblNewLabel_9.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_9.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_9.gridx = 0;
		gbc_lblNewLabel_9.gridy = 11;
		contentPanel.add(lblNewLabel_9, gbc_lblNewLabel_9);

		GridBagConstraints gbc_textFieldQOS = new GridBagConstraints();
		gbc_textFieldQOS.gridwidth = 4;
		gbc_textFieldQOS.anchor = GridBagConstraints.WEST;
		gbc_textFieldQOS.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldQOS.gridx = 1;
		gbc_textFieldQOS.gridy = 11;
		contentPanel.add(textFieldQOS, gbc_textFieldQOS);
		textFieldQOS.setColumns(10);

		JLabel lblNewLabel_10 = new JLabel(LABELS.getString("dlgSetMqttRetain"));
		GridBagConstraints gbc_lblNewLabel_10 = new GridBagConstraints();
		gbc_lblNewLabel_10.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_10.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_10.gridx = 0;
		gbc_lblNewLabel_10.gridy = 12;
		contentPanel.add(lblNewLabel_10, gbc_lblNewLabel_10);

		GridBagConstraints gbc_rdbtnNewRetainYes = new GridBagConstraints();
		gbc_rdbtnNewRetainYes.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRetainYes.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRetainYes.gridx = 1;
		gbc_rdbtnNewRetainYes.gridy = 12;
		contentPanel.add(rdbtnRetainYes, gbc_rdbtnNewRetainYes);

		GridBagConstraints gbc_rdbtnNewRetainNo = new GridBagConstraints();
		gbc_rdbtnNewRetainNo.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRetainNo.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRetainNo.gridx = 2;
		gbc_rdbtnNewRetainNo.gridy = 12;
		contentPanel.add(rdbtnRetainNo, gbc_rdbtnNewRetainNo);

		GridBagConstraints gbc_rdbtnNewRetainUnchange = new GridBagConstraints();
		gbc_rdbtnNewRetainUnchange.gridwidth = 2;
		gbc_rdbtnNewRetainUnchange.weightx = 2.0;
		gbc_rdbtnNewRetainUnchange.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRetainUnchange.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnNewRetainUnchange.gridx = 3;
		gbc_rdbtnNewRetainUnchange.gridy = 12;
		contentPanel.add(rdbtnRetainUnchange, gbc_rdbtnNewRetainUnchange);

		ButtonGroup retainRadio = new ButtonGroup();
		retainRadio.add(rdbtnRetainYes);
		retainRadio.add(rdbtnRetainNo);
		retainRadio.add(rdbtnRetainUnchange);

		JLabel lblNewLabel_11 = new JLabel(LABELS.getString("dlgSetMqttUpdatePeriod"));
		lblNewLabel_11.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_lblNewLabel_11 = new GridBagConstraints();
		gbc_lblNewLabel_11.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_11.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_11.gridx = 0;
		gbc_lblNewLabel_11.gridy = 13;
		contentPanel.add(lblNewLabel_11, gbc_lblNewLabel_11);

		GridBagConstraints gbc_textFieldUpdatePeriod = new GridBagConstraints();
		gbc_textFieldUpdatePeriod.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldUpdatePeriod.gridwidth = 4;
		gbc_textFieldUpdatePeriod.anchor = GridBagConstraints.WEST;
		gbc_textFieldUpdatePeriod.gridx = 1;
		gbc_textFieldUpdatePeriod.gridy = 13;
		contentPanel.add(textFieldUpdatePeriod, gbc_textFieldUpdatePeriod);
		textFieldUpdatePeriod.setColumns(10);

		JLabel lblNewLabel_12 = new JLabel(LABELS.getString("dlgSetMsgMqttReboot"));
		lblNewLabel_12.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_12 = new GridBagConstraints();
		gbc_lblNewLabel_12.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_12.gridwidth = 5;
		gbc_lblNewLabel_12.gridx = 0;
		gbc_lblNewLabel_12.gridy = 14;
		contentPanel.add(lblNewLabel_12, gbc_lblNewLabel_12);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);
		scrollPane.setViewportView(contentPanel);
		add(scrollPane, BorderLayout.CENTER);

		chckbxDefaultPrefix.addItemListener(event -> textFieldID.setEnabled(event.getStateChange() != java.awt.event.ItemEvent.SELECTED && owner.getLocalSize() == 1));
		chckbxEnabled.addItemListener(event -> setEnabledMQTT(event.getStateChange() == java.awt.event.ItemEvent.SELECTED, owner.getLocalSize() == 1));
		chckbxNoPWD.addItemListener(event -> setPasswordRequired(event.getStateChange() == java.awt.event.ItemEvent.DESELECTED));
	}

	private void setPasswordRequired(boolean pwdRequired) {
		textFieldPwd.setEnabled(pwdRequired);
		textFieldUser.setEnabled(pwdRequired);
		chckbxShowPwd.setEnabled(pwdRequired);
		//		if(pwdRequired == false) {
		//			textFieldUser.setText("");
		//			textFieldPwd.setText("");
		//		}
	}

	private void setEnabledMQTT(boolean enabled, boolean single) {
		textFieldServer.setEnabled(enabled);
		textFieldUser.setEnabled(enabled);
		textFieldPwd.setEnabled(enabled);
		chckbxShowPwd.setEnabled(enabled);
		chckbxNoPWD.setEnabled(enabled);
		textFieldID.setEnabled(enabled && chckbxDefaultPrefix.isSelected() == false && single);
		chckbxDefaultPrefix.setEnabled(enabled);
		textFieldMaxTimeout.setEnabled(enabled);
		textFieldMinTimeout.setEnabled(enabled);
		//		chckbxClearSession.setEnabled(enabled);

		rdbtnCleanSessionYes.setEnabled(enabled);
		rdbtnCleanSessionNo.setEnabled(enabled);
		rdbtnCleanSessionUnchange.setEnabled(enabled);

		textFieldKeepAlive.setEnabled(enabled);
		textFieldQOS.setEnabled(enabled);
		//		chckbxRetain.setEnabled(enabled);

		rdbtnRetainYes.setEnabled(enabled);
		rdbtnRetainNo.setEnabled(enabled);
		rdbtnRetainUnchange.setEnabled(enabled);

		textFieldUpdatePeriod.setEnabled(enabled);

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
			int rTimeoutMaxGlobal = 0;
			int rTimeoutMinGlobal = 0;
			Boolean cleanSessionGlobal = false;
			int keepAliveGlobal = 0;
			int qosGlobal = 0;
			Boolean retainGlobal = false;
			int updatePerGlobal = 0;
			boolean noPwdGlobal = false;
			boolean first = true;
			for(int i = 0; i < parent.getLocalSize(); i++) {
				try {
					d = parent.getLocalDevice(i);
					MQTTManagerG1 mqttm = (MQTTManagerG1)d.getMQTTManager();
					if(Thread.interrupted()) {
						throw new InterruptedException();
					}
					boolean enabled = mqttm.isEnabled();
					String server = mqttm.getServer();
					String user = mqttm.getUser();
					String id = mqttm.getPrefix();
					int rTimeoutMax = mqttm.getrTimeoutMax();
					int rTimeoutMin = mqttm.getrTimeoutMin();
					Boolean cleanSession = mqttm.isCleanSession();// ? "Y" : "N";
					int keepAlive = mqttm.getKeepAlive();
					int qos = mqttm.getQos();
					Boolean retain = mqttm.isRetain();// ? "Y" : "N";
					int updatePer = mqttm.getUpdatePeriod();
					if(first) {
						enabledGlobal = enabled;
						serverGlobal = server;
						userGlobal = user;
						idGlobal = id;
						rTimeoutMaxGlobal = rTimeoutMax;
						rTimeoutMinGlobal = rTimeoutMin;
						cleanSessionGlobal = cleanSession;
						keepAliveGlobal = keepAlive;
						qosGlobal = qos;
						retainGlobal = retain;
						updatePerGlobal = updatePer;
						noPwdGlobal = user.isEmpty();
						first = false;
					} else {
						if(enabled != enabledGlobal) enabledGlobal = false;
						if(server.equals(serverGlobal) == false) serverGlobal = "";
						if(user.equals(userGlobal) == false) userGlobal = "";
						if(id.equals(idGlobal) == false) idGlobal = "";
						if(rTimeoutMaxGlobal != rTimeoutMax) rTimeoutMaxGlobal = -1;
						if(rTimeoutMinGlobal != rTimeoutMin) rTimeoutMinGlobal = -1;
						if(cleanSessionGlobal != null && cleanSession.equals(cleanSessionGlobal) == false) cleanSessionGlobal = null;
						if(keepAliveGlobal != keepAlive) keepAliveGlobal = -1;
						if(qosGlobal != qos) qosGlobal = -1;
						if(retainGlobal != null && retain.equals(retainGlobal) == false) retainGlobal = null;
						if(updatePerGlobal != updatePer) updatePerGlobal = -1;
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
			if(excludeCount == parent.getLocalSize() && isShowing()) {
				return LABELS.getString("msgAllDevicesExcluded");
			} else if (excludeCount > 0 && isShowing()) {
				Msg.showHtmlMessageDialog(this, exclude, LABELS.getString("dlgExcludedDevicesTitle"), JOptionPane.WARNING_MESSAGE);
			}
			chckbxEnabled.setEnabled(true); // form is active
			chckbxEnabled.setSelected(enabledGlobal);
			textFieldServer.setText(serverGlobal);
			textFieldUser.setText(userGlobal);
			chckbxNoPWD.setSelected(noPwdGlobal);
			textFieldID.setText(idGlobal);
			textFieldMaxTimeout.setValue(rTimeoutMaxGlobal >= 0 ? rTimeoutMaxGlobal : null);
			textFieldMinTimeout.setValue(rTimeoutMinGlobal >= 0 ? rTimeoutMinGlobal : null);
			if(cleanSessionGlobal == null) {
				rdbtnCleanSessionUnchange.setSelected(true);
				rdbtnCleanSessionUnchange.setVisible(true);
			} else if(cleanSessionGlobal == Boolean.TRUE) {
				rdbtnCleanSessionYes.setSelected(true);
				rdbtnCleanSessionUnchange.setVisible(false);
			} else if(cleanSessionGlobal == Boolean.FALSE) {
				rdbtnCleanSessionNo.setSelected(true);
				rdbtnCleanSessionUnchange.setVisible(false);
			}
			textFieldKeepAlive.setValue(keepAliveGlobal >= 0 ? keepAliveGlobal : null);
			textFieldQOS.setValue(qosGlobal >= 0 ? qosGlobal : null);
			if(retainGlobal == null) {
				rdbtnRetainUnchange.setSelected(true);
				rdbtnRetainUnchange.setVisible(true);
			} else if(retainGlobal == Boolean.TRUE) {
				rdbtnRetainYes.setSelected(true);
				rdbtnRetainUnchange.setVisible(false);
			} else if(retainGlobal == Boolean.FALSE) {
				rdbtnRetainNo.setSelected(true);
				rdbtnRetainUnchange.setVisible(false);
			}
			textFieldUpdatePeriod.setValue(updatePerGlobal >= 0 ? updatePerGlobal : null);

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
		for(int i = 0; i < parent.getLocalSize(); i++) {
			String msg;
			MQTTManagerG1 mqttM = mqttModule.get(i);
			if(mqttM != null) {
				if(enabled) {
					String clearSession = rdbtnCleanSessionUnchange.isSelected() ? null : (rdbtnCleanSessionYes.isSelected() ? "true" : "false");
					String retain = rdbtnRetainUnchange.isSelected() ? null : (rdbtnRetainYes.isSelected() ? "true" : "false");
					String prefix ;
					if(chckbxDefaultPrefix.isSelected()) {
						prefix = null;
					} else if(parent.getLocalSize() > 1) {
						prefix = "";
					} else {
						prefix = textFieldID.getText();
					}
					msg = mqttM.set(
							server, user, pwd, prefix,
							(textFieldMaxTimeout.isEmpty()) ? -1 : textFieldMaxTimeout.getIntValue(),
							(textFieldMinTimeout.isEmpty()) ? -1 : textFieldMinTimeout.getIntValue(),
							clearSession,
							(textFieldKeepAlive.isEmpty()) ? -1 : textFieldKeepAlive.getIntValue(),
							(textFieldQOS.isEmpty()) ? -1 : textFieldQOS.getIntValue(),
							retain,
							(textFieldUpdatePeriod.isEmpty()) ? -1 : textFieldUpdatePeriod.getIntValue());
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
				MQTTManagerG1 m = (MQTTManagerG1)device.getMQTTManager();
				chckbxEnabled.setSelected(m.isEnabled());
				textFieldServer.setText(m.getServer());
				textFieldUser.setText(m.getUser());
				textFieldMaxTimeout.setValue(m.getrTimeoutMax());
				textFieldMinTimeout.setValue(m.getrTimeoutMin());
				textFieldKeepAlive.setValue(m.getKeepAlive());
				textFieldQOS.setValue(m.getQos());
				textFieldUpdatePeriod.setValue(m.getUpdatePeriod());
				rdbtnCleanSessionYes.setSelected(m.isCleanSession());
				rdbtnCleanSessionNo.setSelected(m.isCleanSession() == false);
				rdbtnRetainYes.setSelected(m.isRetain());
				rdbtnRetainNo.setSelected(m.isRetain() == false);
			} catch (IOException e) {
				LOG.error("copy", e);
			} catch (UnsupportedOperationException e) {
				LOG.debug("copy", e);
			}
		}
	}
} //629