package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.TimeAndLocationManager;

public class PanelOthers extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	
	private final static Logger LOG = LoggerFactory.getLogger(PanelOthers.class);
	private JRadioButton rdbtnSNTP;
	private JRadioButton rdbtnCloud;
	private ButtonGroup radioSectionGroup;
	private JTextField ntpServerTextField;
	private JRadioButton radioCloudEnable;	
	private JRadioButton radioCloudDisable;
	
	private ArrayList<TimeAndLocationManager> timeAndLocationManager = new ArrayList<TimeAndLocationManager>();

	protected PanelOthers(DialogDeviceSettings parent) {
		super(parent);
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);
		
		JPanel contentPanel = new JPanel();
		contentPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
		scrollPane.setViewportView(contentPanel);
		
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0, 30};
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gridBagLayout);
		
		rdbtnSNTP = new JRadioButton(LABELS.getString("dlgNTPServer"));
		GridBagConstraints gbc_rdbtnSNTP = new GridBagConstraints();
		gbc_rdbtnSNTP.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSNTP.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnSNTP.gridx = 0;
		gbc_rdbtnSNTP.gridy = 0;
		contentPanel.add(rdbtnSNTP, gbc_rdbtnSNTP);
		
		ntpServerTextField = new JTextField();
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 2;
		gbc_textField.anchor = GridBagConstraints.WEST;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		contentPanel.add(ntpServerTextField, gbc_textField);
		ntpServerTextField.setColumns(30);
		
		rdbtnCloud = new JRadioButton(LABELS.getString("dlgCloudConf"));
		GridBagConstraints gbc_rdbtnCloud = new GridBagConstraints();
		gbc_rdbtnCloud.anchor = GridBagConstraints.WEST;
		gbc_rdbtnCloud.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnCloud.gridx = 0;
		gbc_rdbtnCloud.gridy = 1;
		contentPanel.add(rdbtnCloud, gbc_rdbtnCloud);
		
		radioSectionGroup = new ButtonGroup();
		radioSectionGroup.add(rdbtnSNTP);
		radioSectionGroup.add(rdbtnCloud);
		rdbtnSNTP.addActionListener(e -> radioSelection(e));
		rdbtnCloud.addActionListener(e -> radioSelection(e));
		
		radioCloudEnable = new JRadioButton(LABELS.getString("lblEnabled"));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 10);
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 1;
		contentPanel.add(radioCloudEnable, gbc_chckbxNewCheckBox);

		radioCloudDisable = new JRadioButton(LABELS.getString("lblDisabled"));
		GridBagConstraints gbc_rdbtnNewRadioButton = new GridBagConstraints();
		gbc_rdbtnNewRadioButton.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNewRadioButton.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnNewRadioButton.gridx = 2;
		gbc_rdbtnNewRadioButton.gridy = 1;
		contentPanel.add(radioCloudDisable, gbc_rdbtnNewRadioButton);
		
		ButtonGroup radiCloudGroup = new ButtonGroup();
		radiCloudGroup.add(radioCloudEnable);
		radiCloudGroup.add(radioCloudDisable);

		rdbtnSNTP.setSelected(true);
	}
	
	private void radioSelection(ActionEvent e) {
		if(e == null || ((JRadioButton)e.getSource()).isSelected()) {
			radioCloudEnable.setEnabled(false);
			radioCloudDisable.setEnabled(false);
			ntpServerTextField.setEnabled(false);
			if(rdbtnSNTP.isSelected()) {
				ntpServerTextField.setEnabled(true);
			} else if(rdbtnCloud.isSelected()) {
				radioCloudEnable.setEnabled(true);
				radioCloudDisable.setEnabled(true);
			}
		}
	}
	
	private static void enableRadioGroup(ButtonGroup buttonGroup, boolean enable) {
		Enumeration<AbstractButton> buttons = buttonGroup.getElements();
		while (buttons.hasMoreElements()) {
			buttons.nextElement().setEnabled(enable);
		}
	}

	@Override
	String showing() throws InterruptedException {
		timeAndLocationManager.clear();
		
		ButtonModel selectedRadio = radioSectionGroup.getSelection();
		enableRadioGroup(radioSectionGroup, false);
		selectedRadio.setSelected(false);
		radioSelection(null); // form not active
		ShellyAbstractDevice d = null;
		String sntpServerGlobal = "";
		Boolean cloudEnabledGlobal = null;
		boolean first = true;
		for(int i = 0; i < parent.getLocalSize(); i++) {
			d = parent.getLocalDevice(i);
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}
			try {
				TimeAndLocationManager timeManager = d.getTimeAndLocationManager();
				String ntpServer = timeManager.getSNTPServer();
				boolean cloudEnabled = d.getCloudEnabled();
				timeAndLocationManager.add(timeManager);
				
				if(first) {
					sntpServerGlobal = ntpServer;
					cloudEnabledGlobal = cloudEnabled;
					first = false;
				} else {
					if(ntpServer == null || ntpServer.equals(sntpServerGlobal) == false) sntpServerGlobal = "";
					if(cloudEnabled != cloudEnabledGlobal) cloudEnabledGlobal = null;
				}
			} catch (DeviceOfflineException | UnsupportedOperationException e) {
				LOG.debug("PanelOthers.showing offline {}", d.getHostname());
				timeAndLocationManager.add(null);
			} catch (IOException | RuntimeException e) {
				LOG.error("PanelOthers.showing", e);
				timeAndLocationManager.add(null);
			}
		}
		ntpServerTextField.setText(sntpServerGlobal);
		if(cloudEnabledGlobal != null) {
			if(cloudEnabledGlobal) {
				radioCloudEnable.setSelected(true);
			} else {
				radioCloudDisable.setSelected(true);
			}
		} else {
			radioCloudEnable.setSelected(false);
			radioCloudDisable.setSelected(false);
		}
		
		selectedRadio.setSelected(true); // form is now active
		radioSelection(null);
		enableRadioGroup(radioSectionGroup, true);
		return null;
	}

	@Override
	String apply() {
		final String res;
		if(rdbtnSNTP.isSelected()) {
			res = applyNTP();
		} else {
			res =  applyCloud();
		}
		try {
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
			showing();
		} catch (InterruptedException e) {}
		return res;
	}

	private String applyNTP() {
		final String server = ntpServerTextField.getText().trim();
		if(server.isEmpty()) {
			throw new IllegalArgumentException(LABELS.getString("dlgNTPServerEmptyError"));
		}
		String res = "<html>";
		for(int i = 0; i < parent.getLocalSize(); i++) {
			final ShellyAbstractDevice device = parent.getLocalDevice(i);
			if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
				res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
				DeferrablesContainer dc = DeferrablesContainer.getInstance();
				dc.addOrUpdate(parent.getModelIndex(i), DeferrableTask.Type.NTP, LABELS.getString("dlgNTPServer"), (def, dev) -> {
					return dev.getTimeAndLocationManager().setSNTPServer(server);
				});
			} else {
				try {
					TimeAndLocationManager timeManager = timeAndLocationManager.get(i);
					String msg;
					if(timeManager == null) {
						msg = device.getTimeAndLocationManager().setSNTPServer(server);
					} else {
						msg = timeManager.setSNTPServer(server);
					}
					if(msg != null) {
						if(LABELS.containsKey(msg)) {
							msg = LABELS.getString(msg);
						}
						res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + " (" + msg + ")<br>";
					} else {
						res += String.format(LABELS.getString("dlgSetMultiMsgOk"), device.getHostname()) + "<br>";
					}
				} catch(IOException e) {
					res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + "<br>";
				}
			}
		}
		return res;
	}
	
	private String applyCloud() {
		final boolean cloudEnable = radioCloudEnable.isSelected();
		final boolean cloudDisable = radioCloudDisable.isSelected();
		if(cloudEnable || cloudDisable) {
			String res = "<html>";
			for(int i = 0; i < parent.getLocalSize(); i++) {
				final ShellyAbstractDevice device = parent.getLocalDevice(i);
				if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
					res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
					DeferrablesContainer dc = DeferrablesContainer.getInstance();
					dc.addOrUpdate(parent.getModelIndex(i), DeferrableTask.Type.CLOUD_ENABLE, LABELS.getString("dlgCloudConf"), (def, dev) -> {
						return dev.setCloudEnabled(cloudEnable);
					});
				} else {
					String msg = device.setCloudEnabled(cloudEnable);
					if(msg != null) {
						if(LABELS.containsKey(msg)) {
							msg = LABELS.getString(msg);
						}
						res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + " (" + msg + ")<br>";
					} else {
						res += String.format(LABELS.getString("dlgSetMultiMsgOk"), device.getHostname()) + "<br>";
					}
				}
			}
			return res;
		} else {
			throw new IllegalArgumentException(LABELS.getString("dlgCloudEmptyError"));
		}
	}
}