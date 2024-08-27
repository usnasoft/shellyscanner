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

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g1.modules.InputResetManagerG1;
import it.usna.shellyscan.model.device.g1.modules.TimeAndLocationManagerG1;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.InputResetManagerG2;
import it.usna.shellyscan.model.device.g2.modules.TimeAndLocationManagerG2;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;

public class PanelOthers extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	
	private final static Logger LOG = LoggerFactory.getLogger(PanelOthers.class);
	private final ButtonGroup radioMainSectionGroup = new ButtonGroup();
	private JRadioButton rdbtnSNTP;
	private JRadioButton rdbtnCloud;
	private JRadioButton rdbtnInReset;

	private JTextField ntpServerTextField;
	private final ButtonGroup radioCloudGroup = new ButtonGroup();
	private JRadioButton radioCloudEnable;	
	private JRadioButton radioCloudDisable;
	private final ButtonGroup resetGroup = new ButtonGroup();
	private JRadioButton radioResetEnable;
	private JRadioButton radioResetDisable;
	
	private ArrayList<DevData> devicesData = new ArrayList<>();

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
		gbc_textField.insets = new Insets(0, 0, 5, 0);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		contentPanel.add(ntpServerTextField, gbc_textField);
		ntpServerTextField.setColumns(30);
		
		rdbtnCloud = new JRadioButton(LABELS.getString("dlgCloudConf"));
		GridBagConstraints gbc_rdbtnCloud = new GridBagConstraints();
		gbc_rdbtnCloud.anchor = GridBagConstraints.WEST;
		gbc_rdbtnCloud.insets = new Insets(0, 0, 10, 5);
		gbc_rdbtnCloud.gridx = 0;
		gbc_rdbtnCloud.gridy = 1;
		contentPanel.add(rdbtnCloud, gbc_rdbtnCloud);
		
		radioCloudEnable = new JRadioButton(LABELS.getString("lblEnabled"));
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 10);
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.gridx = 1;
		gbc_chckbxNewCheckBox.gridy = 1;
		contentPanel.add(radioCloudEnable, gbc_chckbxNewCheckBox);

		radioCloudDisable = new JRadioButton(LABELS.getString("lblDisabled"));
		GridBagConstraints gbc_rdbtnCloudEn = new GridBagConstraints();
		gbc_rdbtnCloudEn.anchor = GridBagConstraints.WEST;
		gbc_rdbtnCloudEn.insets = new Insets(0, 0, 10, 0);
		gbc_rdbtnCloudEn.gridx = 2;
		gbc_rdbtnCloudEn.gridy = 1;
		contentPanel.add(radioCloudDisable, gbc_rdbtnCloudEn);
		
		radioCloudGroup.add(radioCloudEnable);
		radioCloudGroup.add(radioCloudDisable);
	
		rdbtnInReset = new JRadioButton(LABELS.getString("dlgResetConf"));
		GridBagConstraints gbc_rdbtnInReset = new GridBagConstraints();
		gbc_rdbtnInReset.anchor = GridBagConstraints.WEST;
		gbc_rdbtnInReset.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnInReset.gridx = 0;
		gbc_rdbtnInReset.gridy = 2;
		contentPanel.add(rdbtnInReset, gbc_rdbtnInReset);
		
		radioResetEnable = new JRadioButton(LABELS.getString("lblEnabled"));
		GridBagConstraints gbc_radioResetEnable = new GridBagConstraints();
		gbc_radioResetEnable.insets = new Insets(0, 0, 5, 10);
		gbc_radioResetEnable.gridx = 1;
		gbc_radioResetEnable.gridy = 2;
		contentPanel.add(radioResetEnable, gbc_radioResetEnable);
		
		radioResetDisable = new JRadioButton(LABELS.getString("lblDisabled"));
		GridBagConstraints gbc_radioResetDisable = new GridBagConstraints();
		gbc_radioResetDisable.insets = new Insets(0, 0, 5, 0);
		gbc_radioResetDisable.anchor = GridBagConstraints.WEST;
		gbc_radioResetDisable.gridx = 2;
		gbc_radioResetDisable.gridy = 2;
		contentPanel.add(radioResetDisable, gbc_radioResetDisable);
		
		resetGroup.add(radioResetEnable);
		resetGroup.add(radioResetDisable);
		
		radioMainSectionGroup.add(rdbtnSNTP);
		radioMainSectionGroup.add(rdbtnCloud);
		radioMainSectionGroup.add(rdbtnInReset);
		rdbtnSNTP.addActionListener(e -> radioSelection(e));
		rdbtnCloud.addActionListener(e -> radioSelection(e));
		rdbtnInReset.addActionListener(e -> radioSelection(e));

		rdbtnSNTP.setSelected(true);
	}
	
	private void radioSelection(ActionEvent e) {
		if(e == null || ((JRadioButton)e.getSource()).isSelected()) {
			ntpServerTextField.setEnabled(false);
			radioCloudEnable.setEnabled(false);
			radioCloudDisable.setEnabled(false);
			radioResetEnable.setEnabled(false);
			radioResetDisable.setEnabled(false);
			if(rdbtnSNTP.isSelected()) {
				ntpServerTextField.setEnabled(true);
			} else if(rdbtnCloud.isSelected()) {
				radioCloudEnable.setEnabled(true);
				radioCloudDisable.setEnabled(true);
			} else if(rdbtnInReset.isSelected()) {
				radioResetEnable.setEnabled(true);
				radioResetDisable.setEnabled(true);
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
		devicesData.clear();
		
		ButtonModel selectedRadio = radioMainSectionGroup.getSelection();
		enableRadioGroup(radioMainSectionGroup, false);
		selectedRadio.setSelected(false);
		radioSelection(null); // form not active
		ShellyAbstractDevice d = null;
		String sntpServerGlobal = "";
		Boolean cloudEnabledGlobal = null;
		Boolean resetEnabledGlobal = null;
		boolean first = true;
		for(int i = 0; i < parent.getLocalSize(); i++) {
			d = parent.getLocalDevice(i);
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}

			try {
				final TimeAndLocationManager timeManager;
				final InputResetManager inputResetMode;
				if (d instanceof AbstractG1Device) {
					JsonNode config = d.getJSON("/settings");
					timeManager = new TimeAndLocationManagerG1((AbstractG1Device)d, config);
					inputResetMode = new InputResetManagerG1((AbstractG1Device)d, config);
				} else { // G2-G3
					JsonNode config = d.getJSON("/rpc/Shelly.GetConfig");
					timeManager = new TimeAndLocationManagerG2((AbstractG2Device)d, config);
					inputResetMode = new InputResetManagerG2((AbstractG2Device)d, config);
				}

				String ntpServer = timeManager.getSNTPServer();
				boolean cloudEnabled = d.getCloudEnabled();
				devicesData.add(new DevData(timeManager, inputResetMode));
				
				if(first) {
					sntpServerGlobal = ntpServer;
					cloudEnabledGlobal = cloudEnabled;
					resetEnabledGlobal = inputResetMode.getValAsBoolean();
					first = false;
				} else {
					if(ntpServer == null || ntpServer.equals(sntpServerGlobal) == false) sntpServerGlobal = "";
					if(cloudEnabled != cloudEnabledGlobal) cloudEnabledGlobal = null;
					if(inputResetMode.getValAsBoolean() != resetEnabledGlobal) resetEnabledGlobal = null;
				}
			} catch (DeviceOfflineException | UnsupportedOperationException e) {
				LOG.debug("PanelOthers.showing offline {}", d.getHostname());
				devicesData.add(null);
			} catch (IOException | RuntimeException e) {
				LOG.error("PanelOthers.showing", e);
				devicesData.add(null);
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
			radioCloudGroup.clearSelection();
		}
		if(resetEnabledGlobal != null) {
			if(resetEnabledGlobal) {
				radioResetEnable.setSelected(true);
			} else {
				radioResetDisable.setSelected(true);
			}
		} else {
			resetGroup.clearSelection();
		}
		
		selectedRadio.setSelected(true); // form is now active
		radioSelection(null);
		enableRadioGroup(radioMainSectionGroup, true);
		
		
		radioResetEnable.setSelected(false);
		radioResetDisable.setSelected(false);
		return null;
	}

	@Override
	String apply() {
		final String res;
		if(rdbtnSNTP.isSelected()) {
			res = applyNTP();
		} else if(rdbtnCloud.isSelected()) {
			res = applyCloud();
		} else {
			res = applyInputReset();
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
					TimeAndLocationManager timeManager = devicesData.get(i).timeManager;
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
		final boolean enable = radioCloudEnable.isSelected();
		final boolean disable = radioCloudDisable.isSelected();
		if(enable || disable) {
			String res = "<html>";
			for(int i = 0; i < parent.getLocalSize(); i++) {
				final ShellyAbstractDevice device = parent.getLocalDevice(i);
				if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
					res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
					DeferrablesContainer dc = DeferrablesContainer.getInstance();
					dc.addOrUpdate(parent.getModelIndex(i), DeferrableTask.Type.CLOUD_ENABLE, LABELS.getString("dlgCloudConf"), (def, dev) -> {
						return dev.setCloudEnabled(enable);
					});
				} else {
					String msg = device.setCloudEnabled(enable);
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
	
	private String applyInputReset() {
		final boolean enable = radioResetEnable.isSelected();
		final boolean disable = radioResetDisable.isSelected();
		if(enable || disable) {
			String res = "<html>";
			for(int i = 0; i < parent.getLocalSize(); i++) {
				final ShellyAbstractDevice device = parent.getLocalDevice(i);
				if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
					res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
					DeferrablesContainer dc = DeferrablesContainer.getInstance();
					dc.addOrUpdate(parent.getModelIndex(i), DeferrableTask.Type.INPUT_RESET_ENABLE, LABELS.getString("dlgResetConf"), (def, dev) -> {
						return dev.getInputResetManager().enableReset(enable);
					});
				} else {
					try {
						InputResetManager resetManager = devicesData.get(i).inReset;
						String msg;
						if(resetManager == null) {
							msg = device.getInputResetManager().enableReset(enable);
						} else {
							msg = resetManager.enableReset(enable);
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
		} else {
			throw new IllegalArgumentException(LABELS.getString("dlgResetEmptyError"));
		}
	}
	
	private record DevData(TimeAndLocationManager timeManager, InputResetManager inReset) {};
}