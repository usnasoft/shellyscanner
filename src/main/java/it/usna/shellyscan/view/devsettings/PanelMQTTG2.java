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
import it.usna.shellyscan.model.device.g2.modules.MQTTManagerG2;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.view.DialogDeviceSelection;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.UsnaEventListener;

public class PanelMQTTG2 extends AbstractSettingsPanel implements UsnaEventListener<ShellyAbstractDevice, Future<?>> {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(PanelMQTTG2.class);

	private JRadioButton rdbtnMQTTControlYes = new JRadioButton(LABELS.getString("true_yna"));
	private JRadioButton rdbtnMQTTControlNo = new JRadioButton(LABELS.getString("false_yna"));
	private JRadioButton rdbtnMQTTControlKeep = new JRadioButton(LABELS.getString("dlgSetDoNotChange"));
	private JRadioButton rdbtnRPCOverYes = new JRadioButton(LABELS.getString("true_yna"));
	private JRadioButton rdbtnRPCOverNo = new JRadioButton(LABELS.getString("false_yna"));
	private JRadioButton rdbtnRPCOverKeep = new JRadioButton(LABELS.getString("dlgSetDoNotChange"));
	private JRadioButton rdbtnRPCYes = new JRadioButton(LABELS.getString("true_yna"));
	private JRadioButton rdbtnRPCNo = new JRadioButton(LABELS.getString("false_yna"));
	private JRadioButton rdbtnRPKeep = new JRadioButton(LABELS.getString("dlgSetDoNotChange"));
	private JRadioButton rdbtnGenericSUpdateYes = new JRadioButton(LABELS.getString("true_yna"));
	private JRadioButton rdbtnGenericSUpdateNo = new JRadioButton(LABELS.getString("false_yna"));
	private JRadioButton rdbtnGenericSUpdateKeep = new JRadioButton(LABELS.getString("dlgSetDoNotChange"));
	private char pwdEchoChar;
	private JCheckBox chckbxEnabled = new JCheckBox();
	private JTextField textFieldServer;
	private JPasswordField textFieldPwd;
	private JCheckBox chckbxShowPwd;
	private JTextField textFieldUser;
	private JTextField textFieldID;
	private JCheckBox chckbxNoPWD;
	private JCheckBox chckbxDefaultPrefix;
	private List<MQTTManagerG2> mqttModule = new ArrayList<>();
	
	private JButton btnCopy = new JButton(LABELS.getString("btnCopyFrom"));
	private DialogDeviceSelection selDialog = null;

	public PanelMQTTG2(DialogDeviceSettings owner) {
		super(owner);
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {10, 0, 30, 0, 0};
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
		btnCopy.addActionListener(e -> selDialog = new DialogDeviceSelection(owner, this, parentDlg.getModel()));
		
		JLabel lblNewLabel_7 = new JLabel(LABELS.getString("dlgSetMqttControl"));
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_7.gridx = 0;
		gbc_lblNewLabel_7.gridy = 1;
		contentPanel.add(lblNewLabel_7, gbc_lblNewLabel_7);
		
		GridBagConstraints gbc_rdbtnMQTTControlYes = new GridBagConstraints();
		gbc_rdbtnMQTTControlYes.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnMQTTControlYes.gridx = 1;
		gbc_rdbtnMQTTControlYes.gridy = 1;
		contentPanel.add(rdbtnMQTTControlYes, gbc_rdbtnMQTTControlYes);

		GridBagConstraints gbc_rdbtnMQTTControlNo = new GridBagConstraints();
		gbc_rdbtnMQTTControlNo.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnMQTTControlNo.gridx = 2;
		gbc_rdbtnMQTTControlNo.gridy = 1;
		contentPanel.add(rdbtnMQTTControlNo, gbc_rdbtnMQTTControlNo);

		GridBagConstraints gbc_rdbtnMQTTControlKeep = new GridBagConstraints();
		gbc_rdbtnMQTTControlKeep.anchor = GridBagConstraints.WEST;
		gbc_rdbtnMQTTControlKeep.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnMQTTControlKeep.gridx = 3;
		gbc_rdbtnMQTTControlKeep.gridy = 1;
		contentPanel.add(rdbtnMQTTControlKeep, gbc_rdbtnMQTTControlKeep);
		
		ButtonGroup rdbtnMQTTControlRadio = new ButtonGroup();
		rdbtnMQTTControlRadio.add(rdbtnMQTTControlYes);
		rdbtnMQTTControlRadio.add(rdbtnMQTTControlNo);
		rdbtnMQTTControlRadio.add(rdbtnMQTTControlKeep);

		JLabel lblNewLabel_6 = new JLabel(LABELS.getString("dlgSetMqttRPC"));
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 2;
		contentPanel.add(lblNewLabel_6, gbc_lblNewLabel_6);

		GridBagConstraints gbc_rdbtnRPCOverYes = new GridBagConstraints();
		gbc_rdbtnRPCOverYes.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRPCOverYes.gridx = 1;
		gbc_rdbtnRPCOverYes.gridy = 2;
		contentPanel.add(rdbtnRPCOverYes, gbc_rdbtnRPCOverYes);

		GridBagConstraints gbc_rdbtnRPCOverNo = new GridBagConstraints();
		gbc_rdbtnRPCOverNo.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRPCOverNo.gridx = 2;
		gbc_rdbtnRPCOverNo.gridy = 2;
		contentPanel.add(rdbtnRPCOverNo, gbc_rdbtnRPCOverNo);

		GridBagConstraints gbc_rdbtnRPCOverKeep = new GridBagConstraints();
		gbc_rdbtnRPCOverKeep.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRPCOverKeep.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRPCOverKeep.gridx = 3;
		gbc_rdbtnRPCOverKeep.gridy = 2;
		contentPanel.add(rdbtnRPCOverKeep, gbc_rdbtnRPCOverKeep);
		
		ButtonGroup rpcOverMQTTRadio = new ButtonGroup();
		rpcOverMQTTRadio.add(rdbtnRPCOverYes);
		rpcOverMQTTRadio.add(rdbtnRPCOverNo);
		rpcOverMQTTRadio.add(rdbtnRPCOverKeep);
		
		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("dlgSetMqttRPCnotif"));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 3;
		contentPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);

		GridBagConstraints gbc_rdbtnRPCYes = new GridBagConstraints();
		gbc_rdbtnRPCYes.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRPCYes.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRPCYes.gridx = 1;
		gbc_rdbtnRPCYes.gridy = 3;
		contentPanel.add(rdbtnRPCYes, gbc_rdbtnRPCYes);

		GridBagConstraints gbc_rdbtnRPCNo = new GridBagConstraints();
		gbc_rdbtnRPCNo.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRPCNo.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRPCNo.gridx = 2;
		gbc_rdbtnRPCNo.gridy = 3;
		contentPanel.add(rdbtnRPCNo, gbc_rdbtnRPCNo);
		
		GridBagConstraints gbc_rdbtnRPCNoChange = new GridBagConstraints();
		gbc_rdbtnRPCNoChange.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRPCNoChange.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRPCNoChange.gridx = 3;
		gbc_rdbtnRPCNoChange.gridy = 3;
		contentPanel.add(rdbtnRPKeep, gbc_rdbtnRPCNoChange);
		
		ButtonGroup rpcRadio = new ButtonGroup();
		rpcRadio.add(rdbtnRPCYes);
		rpcRadio.add(rdbtnRPCNo);
		rpcRadio.add(rdbtnRPKeep);
		
		JLabel lblNewLabel_5 = new JLabel(LABELS.getString("dlgSetMqttGenericNotif"));
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 4;
		contentPanel.add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		GridBagConstraints gbc_rdbtnGenericSUpdareYes = new GridBagConstraints();
		gbc_rdbtnGenericSUpdareYes.anchor = GridBagConstraints.WEST;
		gbc_rdbtnGenericSUpdareYes.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnGenericSUpdareYes.gridx = 1;
		gbc_rdbtnGenericSUpdareYes.gridy = 4;
		contentPanel.add(rdbtnGenericSUpdateYes, gbc_rdbtnGenericSUpdareYes);

		GridBagConstraints gbc_rdbtnGenericSUpdareNo = new GridBagConstraints();
		gbc_rdbtnGenericSUpdareNo.anchor = GridBagConstraints.WEST;
		gbc_rdbtnGenericSUpdareNo.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnGenericSUpdareNo.gridx = 2;
		gbc_rdbtnGenericSUpdareNo.gridy = 4;
		contentPanel.add(rdbtnGenericSUpdateNo, gbc_rdbtnGenericSUpdareNo);
		
		GridBagConstraints gbc_rdbtnGenericSUpdareNoChange = new GridBagConstraints();
		gbc_rdbtnGenericSUpdareNoChange.anchor = GridBagConstraints.WEST;
		gbc_rdbtnGenericSUpdareNoChange.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnGenericSUpdareNoChange.gridx = 3;
		gbc_rdbtnGenericSUpdareNoChange.gridy = 4;
		contentPanel.add(rdbtnGenericSUpdateKeep, gbc_rdbtnGenericSUpdareNoChange);
		
		ButtonGroup rpcNotificationRadio = new ButtonGroup();
		rpcNotificationRadio.add(rdbtnGenericSUpdateYes);
		rpcNotificationRadio.add(rdbtnGenericSUpdateNo);
		rpcNotificationRadio.add(rdbtnGenericSUpdateKeep);

		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("dlgSetServer"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 5;
		contentPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

		textFieldServer = new JTextField();
		GridBagConstraints gbc_textFieldServer = new GridBagConstraints();
		gbc_textFieldServer.gridwidth = 4;
		gbc_textFieldServer.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldServer.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldServer.gridx = 1;
		gbc_textFieldServer.gridy = 5;
		contentPanel.add(textFieldServer, gbc_textFieldServer);
		textFieldServer.setColumns(10);

		JLabel lblNewLabel_8 = new JLabel(LABELS.getString("dlgSetUser"));
		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_8.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_8.gridx = 0;
		gbc_lblNewLabel_8.gridy = 6;
		contentPanel.add(lblNewLabel_8, gbc_lblNewLabel_8);

		textFieldUser = new JTextField();
		GridBagConstraints gbc_textFieldUser = new GridBagConstraints();
		gbc_textFieldUser.gridwidth = 4;
		gbc_textFieldUser.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldUser.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldUser.gridx = 1;
		gbc_textFieldUser.gridy = 6;
		contentPanel.add(textFieldUser, gbc_textFieldUser);
		textFieldUser.setColumns(10);

		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("labelPassword"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 7;
		contentPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);

		textFieldPwd = new JPasswordField();
		pwdEchoChar = textFieldPwd.getEchoChar();
		GridBagConstraints gbc_textFieldPwd = new GridBagConstraints();
		gbc_textFieldPwd.gridwidth = 4;
		gbc_textFieldPwd.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldPwd.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldPwd.gridx = 1;
		gbc_textFieldPwd.gridy = 7;
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
		gbc_chckbxSPwd.gridy = 8;
		contentPanel.add(chckbxShowPwd, gbc_chckbxSPwd);

		chckbxNoPWD = new JCheckBox(LABELS.getString("labelNoPwd"));
		GridBagConstraints gbc_chckbxNoPWD = new GridBagConstraints();
		gbc_chckbxNoPWD.anchor = GridBagConstraints.WEST;
		gbc_chckbxNoPWD.insets = new Insets(0, 10, 5, 0);
		gbc_chckbxNoPWD.gridx = 4;
		gbc_chckbxNoPWD.gridy = 8;
		contentPanel.add(chckbxNoPWD, gbc_chckbxNoPWD);

		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("dlgSetMqttId"));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 9;
		contentPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);

		textFieldID = new JTextField();
		GridBagConstraints gbc_textFieldID = new GridBagConstraints();
		gbc_textFieldID.gridwidth = 4;
		gbc_textFieldID.insets = new Insets(0, 0, 5, 0);
		gbc_textFieldID.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldID.gridx = 1;
		gbc_textFieldID.gridy = 9;
		contentPanel.add(textFieldID, gbc_textFieldID);
		textFieldID.setColumns(10);

		chckbxDefaultPrefix = new JCheckBox(LABELS.getString("dlgSetMqttIdDefault"));
		GridBagConstraints gbc_chckbxNewCheckBox1 = new GridBagConstraints();
		gbc_chckbxNewCheckBox1.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox1.gridwidth = 3;
		gbc_chckbxNewCheckBox1.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox1.gridx = 1;
		gbc_chckbxNewCheckBox1.gridy = 10;
		contentPanel.add(chckbxDefaultPrefix, gbc_chckbxNewCheckBox1);

		JLabel lblNewLabel_12 = new JLabel(LABELS.getString("dlgSetMsgMqttReboot"));
		lblNewLabel_12.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_12.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblNewLabel_12 = new GridBagConstraints();
		gbc_lblNewLabel_12.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_12.weighty = 1.0;
		gbc_lblNewLabel_12.anchor = GridBagConstraints.NORTH;
		gbc_lblNewLabel_12.gridwidth = 4;
		gbc_lblNewLabel_12.gridx = 1;
		gbc_lblNewLabel_12.gridy = 11;
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
		rdbtnMQTTControlYes.setEnabled(enabled);
		rdbtnMQTTControlNo.setEnabled(enabled);
		rdbtnMQTTControlKeep.setEnabled(enabled);
		rdbtnRPCOverYes.setEnabled(enabled);
		rdbtnRPCOverNo.setEnabled(enabled);
		rdbtnRPCOverKeep.setEnabled(enabled);
		rdbtnRPCYes.setEnabled(enabled);
		rdbtnRPCNo.setEnabled(enabled);
		rdbtnRPKeep.setEnabled(enabled);
		rdbtnGenericSUpdateYes.setEnabled(enabled);
		rdbtnGenericSUpdateNo.setEnabled(enabled);
		rdbtnGenericSUpdateKeep.setEnabled(enabled);
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
		return fill(true);
	}

	private String fill(boolean showExcluded) throws InterruptedException {
		mqttModule.clear();
		ShellyAbstractDevice d = null;
		String exclude = "<html>" + LABELS.getString("dlgExcludedDevicesMsg");
		int excludeCount = 0;
		try {
			btnCopy.setEnabled(false);
			chckbxEnabled.setEnabled(false);
			setEnabledMQTT(false, false); // disable while checking
			boolean enabledGlobal = false;
			Boolean controlGlobal = Boolean.FALSE;
			Boolean rpcGlobal = Boolean.FALSE;
			Boolean rpcStatusGlobal = Boolean.FALSE;
			Boolean genStatusGlobal = Boolean.FALSE;
			String serverGlobal = "";
			String userGlobal = "";
			String idGlobal = "";
			boolean noPwdGlobal = false;
			boolean first = true;
			for(int i = 0; i < parentDlg.getLocalSize(); i++) {
				try {
					d = parentDlg.getLocalDevice(i);
					MQTTManagerG2 mqttm = (MQTTManagerG2)d.getMQTTManager();
					if(Thread.interrupted()) {
						throw new InterruptedException();
					}
					boolean enabled = mqttm.isEnabled();
					boolean control = mqttm.isControlEnabled();
					boolean rpc = mqttm.isRpcEnabled();
					boolean rpcStatus = mqttm.isRpcNtf();
					boolean genStatus = mqttm.isStatusNtf();
					String server = mqttm.getServer();
					String user = mqttm.getUser();
					String id = mqttm.getPrefix();
					if(first) {
						enabledGlobal = enabled;
						controlGlobal = control; 
						rpcGlobal = rpc;
						rpcStatusGlobal = rpcStatus;
						genStatusGlobal = genStatus;
						serverGlobal = server;
						userGlobal = user;
						idGlobal = id;
						noPwdGlobal = user.isEmpty();
						first = false;
					} else {
						if(enabled != enabledGlobal) enabledGlobal = false;
						if(control != controlGlobal) controlGlobal = null;
						if(rpc != rpcGlobal) rpcGlobal = null;
						if(rpcStatusGlobal != null && rpcStatusGlobal != rpcStatus) rpcStatusGlobal = null;
						if(genStatusGlobal != null && genStatusGlobal != genStatus) genStatusGlobal = null;
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
			if(showExcluded) {
				if(excludeCount == parentDlg.getLocalSize() && isShowing()) {
					return LABELS.getString("msgAllDevicesExcluded");
				} else if (excludeCount > 0 && isShowing()) {
					Msg.showHtmlMessageDialog(this, exclude, LABELS.getString("dlgExcludedDevicesTitle"), JOptionPane.WARNING_MESSAGE);
				}
			}
			if(controlGlobal != null) {
				rdbtnMQTTControlKeep.setVisible(false);
				if(controlGlobal) {
					rdbtnMQTTControlYes.setSelected(true);
				} else {
					rdbtnMQTTControlNo.setSelected(true);
				}
			} else {
				rdbtnMQTTControlKeep.setVisible(true);
				rdbtnMQTTControlKeep.setSelected(true);
			}
			if(rpcGlobal != null) {
				rdbtnRPCOverKeep.setVisible(false);
				if(rpcGlobal) {
					rdbtnRPCOverYes.setSelected(true);
				} else {
					rdbtnRPCOverNo.setSelected(true);
				}
			} else {
				rdbtnRPCOverKeep.setVisible(true);
				rdbtnRPCOverKeep.setSelected(true);
			}
			if(rpcStatusGlobal != null) {
				rdbtnRPKeep.setVisible(false);
				if(rpcStatusGlobal) {
					rdbtnRPCYes.setSelected(true);
				} else {
					rdbtnRPCNo.setSelected(true);
				}
			} else {
				rdbtnRPKeep.setVisible(true);
				rdbtnRPKeep.setSelected(true);
			}
			if(genStatusGlobal != null) {
				rdbtnGenericSUpdateKeep.setVisible(false);
				if(genStatusGlobal) {
					rdbtnGenericSUpdateYes.setSelected(true);
				} else {
					rdbtnGenericSUpdateNo.setSelected(true);
				}
			} else {
				rdbtnGenericSUpdateKeep.setVisible(true);
				rdbtnGenericSUpdateKeep.setSelected(true);
			}
			chckbxEnabled.setEnabled(true); // form is active
			chckbxEnabled.setSelected(enabledGlobal);
			textFieldServer.setText(serverGlobal);
			textFieldUser.setText(userGlobal);
			chckbxNoPWD.setSelected(noPwdGlobal);
			textFieldID.setText(idGlobal);

			setEnabledMQTT(enabledGlobal, parentDlg.getLocalSize() == 1);
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
		Boolean control = rdbtnMQTTControlYes.isSelected() ? Boolean.TRUE : rdbtnMQTTControlNo.isSelected() ? Boolean.FALSE : null;
		Boolean prc = rdbtnRPCOverYes.isSelected() ? Boolean.TRUE : rdbtnRPCOverNo.isSelected() ? Boolean.FALSE : null;
		Boolean prcNtf = rdbtnRPCYes.isSelected() ? Boolean.TRUE : rdbtnRPCNo.isSelected() ? Boolean.FALSE : null;
		Boolean genNtf = rdbtnGenericSUpdateYes.isSelected() ? Boolean.TRUE : rdbtnGenericSUpdateNo.isSelected() ? Boolean.FALSE : null;
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
		for(int i = 0; i < parentDlg.getLocalSize(); i++) {
			String msg;
			MQTTManagerG2 mqttM = mqttModule.get(i);
			if(mqttM != null) {
				if(enabled) {
					String prefix ;
					if(chckbxDefaultPrefix.isSelected()) {
						prefix = null;
					} else if(parentDlg.getLocalSize() > 1) {
						prefix = "";
					} else {
						prefix = textFieldID.getText();
					}
					msg = mqttM.set(control, prc, prcNtf, genNtf, server, user, pwd, prefix);
				} else {
					msg = mqttM.disable();
				}
				if(msg != null) {
					res += String.format(LABELS.getString("dlgSetMultiMsgFail"), parentDlg.getLocalDevice(i).getHostname()) + " (" + msg + ")<br>";
				} else {
					res += String.format(LABELS.getString("dlgSetMultiMsgOk"), parentDlg.getLocalDevice(i).getHostname()) + "<br>";
				}
			}
		}
		try {
			fill(false);
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
				if(m instanceof MQTTManagerG2) {
					if(((MQTTManagerG2)m).isControlEnabled()) {
						rdbtnMQTTControlYes.setSelected(true);
					} else {
						rdbtnMQTTControlNo.setSelected(true);
					}
					if(((MQTTManagerG2)m).isRpcEnabled()) {
						rdbtnRPCOverYes.setSelected(true);
					} else {
						rdbtnRPCOverNo.setSelected(true);
					}
					if(((MQTTManagerG2)m).isRpcNtf()) {
						rdbtnRPCYes.setSelected(true);
					} else {
						rdbtnRPCNo.setSelected(true);
					}
					if(((MQTTManagerG2)m).isStatusNtf()) {
						rdbtnGenericSUpdateYes.setSelected(true);
					} else {
						rdbtnGenericSUpdateNo.setSelected(true);
					}
				}
			} catch (IOException e) {
				LOG.error("copy", e);
			} catch (UnsupportedOperationException e) {
				LOG.debug("copy", e);
			}
		}
	}
}