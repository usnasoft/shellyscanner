package it.usna.shellyscan.view.devsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.DeferrableTask;
import it.usna.shellyscan.controller.DeferrablesContainer;
import it.usna.shellyscan.model.DeviceOfflineException;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.blu.AbstractBTHomeDevice;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.g1.modules.InputModeManagerG1;
import it.usna.shellyscan.model.device.g1.modules.InputResetManagerG1;
import it.usna.shellyscan.model.device.g1.modules.PowerOnStateManagerG1;
import it.usna.shellyscan.model.device.g1.modules.TimeAndLocationManagerG1;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.InputModeManagerG2;
import it.usna.shellyscan.model.device.g2.modules.InputResetManagerG2;
import it.usna.shellyscan.model.device.g2.modules.PowerOnStateManagerG2;
import it.usna.shellyscan.model.device.g2.modules.TimeAndLocationManagerG2;
import it.usna.shellyscan.model.device.modules.InputModeManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.PowerOnStateManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import tools.jackson.databind.JsonNode;

public class PanelOthers extends AbstractSettingsPanel {
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = LoggerFactory.getLogger(PanelOthers.class);
	private final ButtonGroup radioMainSectionGroup = new ButtonGroup();
	private JRadioButton rdbtnSNTP;
	private JRadioButton rdbtnCloud;
	private JRadioButton rdbtnInReset;
	private JRadioButton rdbtnInMode;
	private JRadioButton rdbtnPowerOn;

	private JTextField ntpServerTextField;
	private final ButtonGroup radioCloudGroup = new ButtonGroup();
	private JRadioButton radioCloudEnable;	
	private JRadioButton radioCloudDisable;
	private final ButtonGroup radioResetGroup = new ButtonGroup();
	private JRadioButton radioResetEnable;
	private JRadioButton radioResetDisable;
	private final ButtonGroup radioInModeGroup = new ButtonGroup();
	private JRadioButton radioInModeButton;
	private JRadioButton radioInModeFlip;
	private JRadioButton radioInModeFollow;
	private JRadioButton radioInModeDetached;
	private final ButtonGroup radioPowerOnGroup = new ButtonGroup();
	private JRadioButton radioPowerOnOff;
	private JRadioButton radioPowerOnOn;
	private JRadioButton radioPowerOnLast;
	private JRadioButton radioPowerOnMatch;
	
	private ArrayList<DeviceData> devicesData = new ArrayList<>();

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
		gridBagLayout.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
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
		
		radioResetGroup.add(radioResetEnable);
		radioResetGroup.add(radioResetDisable);
		
		// Input mode (button / switch)
		rdbtnInMode = new JRadioButton(LABELS.getString("dlgInputModeConf"));
		rdbtnInMode.setToolTipText(LABELS.getString("dlgInputModeConfTooltip"));
		GridBagConstraints gbc_rdbtnInMode = new GridBagConstraints();
		gbc_rdbtnInMode.anchor = GridBagConstraints.WEST;
		gbc_rdbtnInMode.insets = new Insets(5, 0, 5, 5);
		gbc_rdbtnInMode.gridx = 0;
		gbc_rdbtnInMode.gridy = 3;
		contentPanel.add(rdbtnInMode, gbc_rdbtnInMode);
		
		radioInModeButton = new JRadioButton(LABELS.getString("dlgInputModeButton"));
		radioInModeFlip = new JRadioButton(LABELS.getString("dlgInputModeFlip"));
		radioInModeFollow = new JRadioButton(LABELS.getString("dlgInputModeFollow"));
		radioInModeDetached = new JRadioButton(LABELS.getString("dlgInputModeDetached"));
		contentPanel.add(optionsPanel(radioInModeGroup, radioInModeButton, radioInModeFlip, radioInModeFollow, radioInModeDetached), optionsConstraints(3));
		
		// Power on default (output state when power is restored)
		rdbtnPowerOn = new JRadioButton(LABELS.getString("dlgPowerOnConf"));
		rdbtnPowerOn.setToolTipText(LABELS.getString("dlgPowerOnConfTooltip"));
		GridBagConstraints gbc_rdbtnPowerOn = new GridBagConstraints();
		gbc_rdbtnPowerOn.anchor = GridBagConstraints.WEST;
		gbc_rdbtnPowerOn.insets = new Insets(5, 0, 5, 5);
		gbc_rdbtnPowerOn.gridx = 0;
		gbc_rdbtnPowerOn.gridy = 4;
		contentPanel.add(rdbtnPowerOn, gbc_rdbtnPowerOn);
		
		radioPowerOnOff = new JRadioButton(LABELS.getString("dlgPowerOnOff"));
		radioPowerOnOn = new JRadioButton(LABELS.getString("dlgPowerOnOn"));
		radioPowerOnLast = new JRadioButton(LABELS.getString("dlgPowerOnLast"));
		radioPowerOnMatch = new JRadioButton(LABELS.getString("dlgPowerOnMatch"));
		contentPanel.add(optionsPanel(radioPowerOnGroup, radioPowerOnOff, radioPowerOnOn, radioPowerOnLast, radioPowerOnMatch), optionsConstraints(4));
		
		radioMainSectionGroup.add(rdbtnSNTP);
		radioMainSectionGroup.add(rdbtnCloud);
		radioMainSectionGroup.add(rdbtnInReset);
		radioMainSectionGroup.add(rdbtnInMode);
		radioMainSectionGroup.add(rdbtnPowerOn);
		rdbtnSNTP.addActionListener(e -> radioSelection(e));
		rdbtnCloud.addActionListener(e -> radioSelection(e));
		rdbtnInReset.addActionListener(e -> radioSelection(e));
		rdbtnInMode.addActionListener(e -> radioSelection(e));
		rdbtnPowerOn.addActionListener(e -> radioSelection(e));

		rdbtnSNTP.setSelected(true);
	}
	
	private static JPanel optionsPanel(ButtonGroup group, JRadioButton... options) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		for(int i = 0; i < options.length; i++) {
			if(i > 0) {
				panel.add(Box.createHorizontalStrut(10));
			}
			group.add(options[i]);
			panel.add(options[i]);
		}
		return panel;
	}
	
	private static GridBagConstraints optionsConstraints(int row) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 0, 5, 0);
		gbc.gridx = 1;
		gbc.gridy = row;
		return gbc;
	}
	
	private void radioSelection(ActionEvent e) {
		if(e == null || ((JRadioButton)e.getSource()).isSelected()) {
			ntpServerTextField.setEnabled(false);
			enableRadioGroup(radioCloudGroup, false);
			enableRadioGroup(radioResetGroup, false);
			enableRadioGroup(radioInModeGroup, false);
			enableRadioGroup(radioPowerOnGroup, false);
			if(rdbtnSNTP.isSelected()) {
				ntpServerTextField.setEnabled(true);
			} else if(rdbtnCloud.isSelected()) {
				enableRadioGroup(radioCloudGroup, true);
			} else if(rdbtnInReset.isSelected()) {
				enableRadioGroup(radioResetGroup, true);
			} else if(rdbtnInMode.isSelected()) {
				enableRadioGroup(radioInModeGroup, true);
			} else if(rdbtnPowerOn.isSelected()) {
				enableRadioGroup(radioPowerOnGroup, true);
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
	public String showing() throws InterruptedException {
		return fill(true);
	}

	private String fill(boolean showExcluded) throws InterruptedException {
		String exclude = "<html>" + LABELS.getString("dlgExcludedDevicesMsg");
		int excludeCount = 0;
		devicesData.clear();
		
		ButtonModel selectedRadio = radioMainSectionGroup.getSelection();
		enableRadioGroup(radioMainSectionGroup, false);
		selectedRadio.setSelected(false);
		radioSelection(null); // form not active
		ShellyAbstractDevice d = null;
		String sntpServerGlobal = "";
		Boolean cloudEnabledGlobal = null;
		Boolean resetEnabledGlobal = null;
		InputModeManager.Mode inModeGlobal = InputModeManager.Mode.NOT_APPLICABLE;
		PowerOnStateManager.State powerOnGlobal = PowerOnStateManager.State.NOT_APPLICABLE;
		boolean first = true;
		for(int i = 0; i < parentDlg.getLocalSize(); i++) {
			d = parentDlg.getLocalDevice(i);
			if(Thread.interrupted()) {
				throw new InterruptedException();
			}
			try {
				final TimeAndLocationManager timeManager;
				final InputResetManager inputResetMode;
				final InputModeManager inputMode;
				final PowerOnStateManager powerOn;
				if (d instanceof AbstractG1Device g1) {
					JsonNode config = d.getJSON("/settings");
					timeManager = new TimeAndLocationManagerG1(g1, config);
					inputResetMode = new InputResetManagerG1(g1, config);
					inputMode = new InputModeManagerG1(g1, config);
					powerOn = new PowerOnStateManagerG1(g1, config);
				} else if (d instanceof AbstractG2Device g2) { // G2-G3-G4
					JsonNode config = d.getJSON("/rpc/Shelly.GetConfig");
					timeManager = new TimeAndLocationManagerG2(g2, config);
					inputResetMode = new InputResetManagerG2(g2, config);
					inputMode = new InputModeManagerG2(g2, config);
					powerOn = new PowerOnStateManagerG2(g2, config);
				} else if (d instanceof GhostDevice) {
					devicesData.add(null);
					continue;
				} else {
					exclude += "<br>" + UtilMiscellaneous.getFullName(d);
					excludeCount++;
					devicesData.add(null);
					continue;
				}

				String ntpServer = timeManager.getSNTPServer();
				boolean cloudEnabled = d.getCloudEnabled();
				devicesData.add(new DeviceData(timeManager, inputResetMode, inputMode, powerOn));
				inModeGlobal = mergeInputMode(inModeGlobal, inputMode.getVal());
				powerOnGlobal = mergePowerOn(powerOnGlobal, powerOn.getVal());
				
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
		if(showExcluded) {
			if(excludeCount == parentDlg.getLocalSize() && isShowing()) {
				return LABELS.getString("msgAllDevicesExcluded");
			} else if (excludeCount > 0 && isShowing()) {
				Msg.showHtmlMessageDialog(this, exclude, LABELS.getString("dlgExcludedDevicesTitle"), JOptionPane.WARNING_MESSAGE);
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
			radioResetGroup.clearSelection();
		}
		switch(inModeGlobal) {
			case BUTTON -> radioInModeButton.setSelected(true);
			case SWITCH_FLIP -> radioInModeFlip.setSelected(true);
			case SWITCH_FOLLOW -> radioInModeFollow.setSelected(true);
			case DETACHED -> radioInModeDetached.setSelected(true);
			default -> radioInModeGroup.clearSelection();
		}
		switch(powerOnGlobal) {
			case OFF -> radioPowerOnOff.setSelected(true);
			case ON -> radioPowerOnOn.setSelected(true);
			case RESTORE_LAST -> radioPowerOnLast.setSelected(true);
			case MATCH_INPUT -> radioPowerOnMatch.setSelected(true);
			default -> radioPowerOnGroup.clearSelection();
		}
		
		selectedRadio.setSelected(true); // form is now active
		radioSelection(null);
		enableRadioGroup(radioMainSectionGroup, true);
		return null;
	}

	/** devices where the setting is not applicable are ignored; different values -> MIX */
	private static InputModeManager.Mode mergeInputMode(InputModeManager.Mode global, InputModeManager.Mode current) {
		if(current == InputModeManager.Mode.NOT_APPLICABLE) {
			return global;
		} else if(global == InputModeManager.Mode.NOT_APPLICABLE) {
			return current;
		} else {
			return global == current ? global : InputModeManager.Mode.MIX;
		}
	}
	
	private static PowerOnStateManager.State mergePowerOn(PowerOnStateManager.State global, PowerOnStateManager.State current) {
		if(current == PowerOnStateManager.State.NOT_APPLICABLE) {
			return global;
		} else if(global == PowerOnStateManager.State.NOT_APPLICABLE) {
			return current;
		} else {
			return global == current ? global : PowerOnStateManager.State.MIX;
		}
	}

	@Override
	String apply() {
		final String res;
		if(rdbtnSNTP.isSelected()) {
			res = applyNTP();
		} else if(rdbtnCloud.isSelected()) {
			res = applyCloud();
		} else if(rdbtnInReset.isSelected()) {
			res = applyInputReset();
		} else if(rdbtnInMode.isSelected()) {
			res = applyInputMode();
		} else {
			res = applyPowerOn();
		}
		try {
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
			fill(false);
		} catch (InterruptedException e) {}
		return res;
	}

	private String applyNTP() {
		final String server = ntpServerTextField.getText().trim();
		if(server.isEmpty()) {
			throw new IllegalArgumentException(LABELS.getString("dlgNTPServerEmptyError"));
		}
		String res = "<html>";
		for(int i = 0; i < parentDlg.getLocalSize(); i++) {
			final ShellyAbstractDevice device = parentDlg.getLocalDevice(i);
			if(device instanceof AbstractBTHomeDevice == false) { // not blu
				if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
					res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
					DeferrablesContainer dc = DeferrablesContainer.getInstance();
					dc.addOrUpdate(parentDlg.getModelIndex(i), DeferrableTask.Type.NTP, LABELS.getString("dlgNTPServer"),
							(def, dev) -> dev.getTimeAndLocationManager().setSNTPServer(server) );
				} else {
					try {
						DeviceData data = devicesData.get(i);
						String msg;
						if(data == null) {
							msg = device.getTimeAndLocationManager().setSNTPServer(server);
						} else {
							msg = data.timeManager.setSNTPServer(server);
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
		}
		return res;
	}
	
	private String applyCloud() {
		final boolean enable = radioCloudEnable.isSelected();
		final boolean disable = radioCloudDisable.isSelected();
		if(enable || disable) {
			String res = "<html>";
			for(int i = 0; i < parentDlg.getLocalSize(); i++) {
				final ShellyAbstractDevice device = parentDlg.getLocalDevice(i);
				if(device instanceof AbstractG1Device || device instanceof AbstractG2Device || device instanceof GhostDevice) {
					if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
						res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
						DeferrablesContainer dc = DeferrablesContainer.getInstance();
						dc.addOrUpdate(parentDlg.getModelIndex(i), DeferrableTask.Type.CLOUD_ENABLE, LABELS.getString("dlgCloudConf"),
								(def, dev) -> dev.setCloudEnabled(enable) );
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
			for(int i = 0; i < parentDlg.getLocalSize(); i++) {
				final ShellyAbstractDevice device = parentDlg.getLocalDevice(i);
				if(device instanceof AbstractG1Device || device instanceof AbstractG2Device || device instanceof GhostDevice) {
					if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
						res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
						DeferrablesContainer dc = DeferrablesContainer.getInstance();
						dc.addOrUpdate(parentDlg.getModelIndex(i), DeferrableTask.Type.INPUT_RESET_ENABLE, LABELS.getString("dlgResetConf"),
								(def, dev) -> dev.getInputResetManager().enableReset(enable) );
					} else {
						try {
							DeviceData data = devicesData.get(i);
							String msg;
							if(data == null) {
								msg = device.getInputResetManager().enableReset(enable);
							} else {
								msg = data.inReset.enableReset(enable);
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
			}
			return res;
		} else {
			throw new IllegalArgumentException(LABELS.getString("dlgResetEmptyError"));
		}
	}
	
	private String applyInputMode() {
		final InputModeManager.Mode mode;
		if(radioInModeButton.isSelected()) {
			mode = InputModeManager.Mode.BUTTON;
		} else if(radioInModeFlip.isSelected()) {
			mode = InputModeManager.Mode.SWITCH_FLIP;
		} else if(radioInModeFollow.isSelected()) {
			mode = InputModeManager.Mode.SWITCH_FOLLOW;
		} else if(radioInModeDetached.isSelected()) {
			mode = InputModeManager.Mode.DETACHED;
		} else {
			throw new IllegalArgumentException(LABELS.getString("dlgInputModeEmptyError"));
		}
		return applyToAll(DeferrableTask.Type.INPUT_MODE, LABELS.getString("dlgInputModeConf"),
				dev -> inputModeManager(dev).setMode(mode),
				data -> data.inMode.setMode(mode));
	}
	
	private String applyPowerOn() {
		final PowerOnStateManager.State state;
		if(radioPowerOnOff.isSelected()) {
			state = PowerOnStateManager.State.OFF;
		} else if(radioPowerOnOn.isSelected()) {
			state = PowerOnStateManager.State.ON;
		} else if(radioPowerOnLast.isSelected()) {
			state = PowerOnStateManager.State.RESTORE_LAST;
		} else if(radioPowerOnMatch.isSelected()) {
			state = PowerOnStateManager.State.MATCH_INPUT;
		} else {
			throw new IllegalArgumentException(LABELS.getString("dlgPowerOnEmptyError"));
		}
		return applyToAll(DeferrableTask.Type.POWER_ON_STATE, LABELS.getString("dlgPowerOnConf"),
				dev -> powerOnManager(dev).setState(state),
				data -> data.powerOn.setState(state));
	}
	
	@FunctionalInterface
	private interface DeviceSetter {
		String set(ShellyAbstractDevice device) throws IOException;
	}
	
	/**
	 * Apply a setting to all (G1/G2+) selected devices; off-line devices are queued (deferred)
	 * @param fromDevice used for deferred tasks or if cached data are not available
	 * @param fromData used when cached data (read on showing) are available
	 */
	private String applyToAll(DeferrableTask.Type type, String description, DeviceSetter fromDevice, Function<DeviceData, String> fromData) {
		String res = "<html>";
		for(int i = 0; i < parentDlg.getLocalSize(); i++) {
			final ShellyAbstractDevice device = parentDlg.getLocalDevice(i);
			if(device instanceof AbstractG1Device || device instanceof AbstractG2Device || device instanceof GhostDevice) {
				if(device.getStatus() == Status.OFF_LINE || device instanceof GhostDevice) { // defer
					res += String.format(LABELS.getString("dlgSetMultiMsgQueue"), device.getHostname()) + "<br>";
					DeferrablesContainer dc = DeferrablesContainer.getInstance();
					dc.addOrUpdate(parentDlg.getModelIndex(i), type, description, (def, dev) -> fromDevice.set(dev));
				} else {
					try {
						DeviceData data = devicesData.get(i);
						String msg = (data == null) ? fromDevice.set(device) : fromData.apply(data);
						if("notApplicable".equals(msg)) { // bulk use: devices without this setting are just skipped
							res += String.format(LABELS.getString("dlgSetMultiMsgSkip"), device.getHostname()) + "<br>";
						} else if(msg != null) {
							if(LABELS.containsKey(msg)) {
								msg = LABELS.getString(msg);
							}
							res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + " (" + msg + ")<br>";
						} else {
							res += String.format(LABELS.getString("dlgSetMultiMsgOk"), device.getHostname()) + "<br>";
						}
					} catch(IOException | UnsupportedOperationException e) {
						res += String.format(LABELS.getString("dlgSetMultiMsgFail"), device.getHostname()) + "<br>";
					}
				}
			}
		}
		return res;
	}
	
	private static InputModeManager inputModeManager(ShellyAbstractDevice d) throws IOException {
		if(d instanceof AbstractG1Device g1) {
			return new InputModeManagerG1(g1);
		} else if(d instanceof AbstractG2Device g2) {
			return new InputModeManagerG2(g2);
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	private static PowerOnStateManager powerOnManager(ShellyAbstractDevice d) throws IOException {
		if(d instanceof AbstractG1Device g1) {
			return new PowerOnStateManagerG1(g1);
		} else if(d instanceof AbstractG2Device g2) {
			return new PowerOnStateManagerG2(g2);
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	private record DeviceData(TimeAndLocationManager timeManager, InputResetManager inReset, InputModeManager inMode, PowerOnStateManager powerOn) {};
}