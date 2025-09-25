package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.MissingResourceException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.g1.modules.ThermostatG1;
import it.usna.shellyscan.model.device.modules.CCTInterface;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.InputInterface;
import it.usna.shellyscan.model.device.modules.MotionInterface;
import it.usna.shellyscan.model.device.modules.RGBCCTInterface;
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.RGBWInterface;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.shellyscan.model.device.modules.RollerInterface;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;
import it.usna.shellyscan.model.device.modules.WhiteInterface;
import it.usna.swing.VerticalFlowLayout;

public class DevicesCommandCellRenderer implements TableCellRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(DevicesCommandCellRenderer.class);

	// Generic
	static final ImageIcon EDIT_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/Write16.png"));
	static final ImageIcon UP_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/Arrow16up.png"));
	static final ImageIcon DOWN_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/Arrow16down.png"));
	static final ImageIcon STOP_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/PlayerStop16.png"));
	private JButton onOffButton0 = new JButton();
	private JLabel label0 = new JLabel();
	private JButton editDialogButton = new JButton(EDIT_IMG);
	private JPanel stackedPanel = new JPanel();
	private JLabel labelPlain = new JLabel();
	
	static final Color BUTTON_ON_BG_COLOR = new Color(125, 217, 240);
	static final Color BUTTON_OFF_BG_COLOR = Color.WHITE;
	static final Color BUTTON_ON_FG_COLOR = new Color(210, 120, 0);
	static final int MAX_ACTIONS_SHOWN = 5; // if supported actions <= then show also disabled buttons
	private static final int BUTTON_MARGIN_H = 12;
	private static final int BUTTON_MARGIN_V = 1;
	static final Border BUTTON_BORDERS = BorderFactory.createEmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H, BUTTON_MARGIN_V, BUTTON_MARGIN_H);
//	static final Border BUTTON_BORDERS_SMALL = BorderFactory.createEmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H-2, BUTTON_MARGIN_V, BUTTON_MARGIN_H-2);
	static final Border BUTTON_BORDERS_SMALLER = BorderFactory.createEmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H-5, BUTTON_MARGIN_V, BUTTON_MARGIN_H-5);
	static final String LABEL_ON = Main.LABELS.getString("btnOnLabel");
	static final String LABEL_OFF = Main.LABELS.getString("btnOffLabel");
	
	private boolean tempUnitCelsius;

	public DevicesCommandCellRenderer(boolean celsius) {
		this.tempUnitCelsius = celsius;
		// Generic
		onOffButton0.setBorder(BUTTON_BORDERS);
		editDialogButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		editDialogButton.setContentAreaFilled(false);

		BoxLayout stackedPanelLO = new BoxLayout(stackedPanel, BoxLayout.Y_AXIS);
		stackedPanel.setLayout(stackedPanelLO);
		
		labelPlain.setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		try {
			final JComponent ret;
			final Color foregroundColor = isSelected ? table.getSelectionForeground() : table.getForeground();
			if(value instanceof RelayInterface[] riArray) {
				stackedPanel.removeAll();
				for(int i = 0; i < riArray.length; i++) { // 1, 1PM, EM, 2.5 ...
					stackedPanel.add(getRelayPanel(riArray[i], foregroundColor, i == 0));
				}
				ret = stackedPanel;
			} else if(value instanceof RollerInterface[] rollers) { // 2.5 ...
				stackedPanel.removeAll();
				for(int i = 0; i < rollers.length; i++) { // 1, 1PM, EM, 2.5 ...
					stackedPanel.add(getRollerPanel(rollers[i], foregroundColor, i == 0));
				}
				ret = stackedPanel;
			} else if(value instanceof RGBCCTInterface[] lights) { // RGBW Bulbs
				ret = getRGBCCTPanel(lights[0], foregroundColor, true, true);
			} else if(value instanceof RGBWInterface[] rgbs) { // RGBs
				ret = getRGBWPanel(rgbs[0], foregroundColor, true, true);
			} else if(value instanceof RGBInterface[] rgbs) { // RGBs
				ret = getRGBPanel(rgbs[0], foregroundColor, true, true);
			} else if(value instanceof WhiteInterface[] lights && lights.length == 1) { // Dimmable (CCT) white
				ret = getWhitePanel(lights[0], foregroundColor, true, lights[0] instanceof CCTInterface);
			} else if(value instanceof ThermostatG1 thermostat) { // TRV gen1
				ret = getThermostatG1Panel(thermostat, foregroundColor);
			} else if(value instanceof ThermostatInterface[] thermostats) {
				ret = getThermostatPanel(thermostats[0], foregroundColor);
			} else if(value instanceof DeviceModule[] modArray) { // mixed modules
				stackedPanel.removeAll();
				for(int i = 0; i < modArray.length; i++) {
					DeviceModule module = modArray[i];
					if(module instanceof RelayInterface rel) {
						stackedPanel.add(getRelayPanel(rel, foregroundColor, i == 0));
					} else if(module instanceof InputInterface input) {
						if(input.enabled()) {
							stackedPanel.add(getInputPanel(input, foregroundColor));
						}
					} else if(module instanceof WhiteInterface white) {
						stackedPanel.add(getWhiteSyntheticPanel(white, foregroundColor, i == 0, i == modArray.length - 1));
					} else if(module instanceof RGBInterface rgb) {
						stackedPanel.add(getRGBSyntheticPanel(rgb, foregroundColor, i == 0, i == modArray.length - 1));
					} else if(module instanceof MotionInterface pir) {
						JLabel motionLabel = (i == 0) ? label0 : new JLabel();
						motionLabel.setText(LABELS.getString(pir.motion() ? "labelStatusMotion_true" : "labelStatusMotion_false"));
						motionLabel.setForeground(foregroundColor);
						stackedPanel.add(motionLabel);
					}
				}
				ret = stackedPanel;
			} else {
				labelPlain.setText(value == null ? "" : value.toString());
				labelPlain.setForeground(foregroundColor);
				ret = labelPlain;
			}
			return ret;
		} catch(Exception e) {
			LOG.error("rendering error", e);
			labelPlain.setText("--");
			return labelPlain;
		}
	}

	private JPanel getRelayPanel(RelayInterface rel, final Color foregroundColor, boolean ind0) {
		JPanel relayPanel = new JPanel(new BorderLayout());
		final JLabel relayLabel;// = new JLabel(rel.getLabel());
		final JButton button;
		if(ind0) {
			relayLabel = label0;
			relayLabel.setText(rel.getLabel());
			button = onOffButton0;
			button.setForeground(rel.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		} else {
			relayLabel = new JLabel(rel.getLabel());
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
			if(rel.isInputOn()) {
				button.setForeground(BUTTON_ON_FG_COLOR);
			}
		}
		relayLabel.setForeground(foregroundColor);
		JPanel relayButtonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));
		relayButtonPanel.setOpaque(false);
		relayButtonPanel.add(button);

		relayPanel.setOpaque(false);
		relayPanel.add(relayLabel, BorderLayout.CENTER);
		relayPanel.add(relayButtonPanel, BorderLayout.EAST);
		if(rel.isOn()) {
			button.setText(LABEL_ON);
			button.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			button.setText(LABEL_OFF);
			button.setBackground(BUTTON_OFF_BG_COLOR);
		}
//		button.setForeground(rel.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		return relayPanel;
	}
	
	private JPanel getRollerPanel(RollerInterface roller, final Color foregroundColor, boolean ind0) {
		final JPanel rollerPanel = new JPanel(new BorderLayout());
		rollerPanel.setOpaque(false);
		final JLabel rollerLabel = (ind0) ? label0 : new JLabel();
		
		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
		rollerSouthPanel.setOpaque(false);
		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		rollerButtonPanel.setOpaque(false);
		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
		rollerSouthPanel.add(rollerButtonPanel, BorderLayout.EAST);
		JButton rollerButtonUp = new JButton(UP_IMG);
		JButton rollerButtonDown = new JButton(DOWN_IMG);
		JButton rollerButtonStop = new JButton(STOP_IMG);
		rollerButtonUp.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonStop.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonDown.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonPanel.add(rollerButtonUp);
		rollerButtonPanel.add(rollerButtonStop);
		rollerButtonPanel.add(rollerButtonDown);
		rollerPanel.add(rollerSouthPanel, BorderLayout.SOUTH);
		
		if(roller.isCalibrated()) {
			rollerLabel.setText(roller.getLabel() + " " + roller.getPosition() + "%");
			JSlider rollerSlider = new JSlider(0, 100, roller.getPosition());
//			rollerSlider.setPreferredSize(new Dimension(200, rollerSlider.getPreferredSize().height));
			rollerSouthPanel.add(rollerSlider, BorderLayout.CENTER);
		} else {
			rollerLabel.setText(roller.getLabel());
		}
		rollerLabel.setForeground(foregroundColor);
//		rollerPanel.setPreferredSize(new Dimension(500, rollerPanel.getPreferredSize().height));
		return rollerPanel;
	}
	
	private JPanel getInputPanel(InputInterface inp, final Color foregroundColor) {
		final JPanel actionsPanel = new JPanel(new BorderLayout());
		String inpName = inp.getLabel();
		JLabel actionsLabel = new JLabel(inpName == null || inpName.isEmpty() ? "\u25CB" : inpName);
		actionsLabel.setForeground(foregroundColor);
		
		JPanel actionsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		int numEvents = inp.getRegisteredEventsCount();
		if(numEvents > 0) {
			for(int i = 0; i < numEvents; i++) {
				boolean enabled = inp.enabled(i);
				if(enabled || numEvents <= MAX_ACTIONS_SHOWN) {
					String bLabel;
					try {
						bLabel = LABELS.getString(inp.getEvent(i));
					} catch(MissingResourceException e) {
						LOG.debug("Missing event {}", inp.getEvent(i));
						bLabel = "x";
					}
					JButton b = new JButton(bLabel);
					b.setBorder(/*bLabel.length() > 1 ?*/ BUTTON_BORDERS_SMALLER /*: BUTTON_BORDERS_SMALL*/);
					b.setEnabled(enabled);
					b.setBackground(BUTTON_OFF_BG_COLOR);
					actionsButtonsPanel.add(b);
					if(inp.isInputOn()) {
						b.setForeground(BUTTON_ON_FG_COLOR);
					}
				}
			}
		} else {
			if(inp.isInputOn()) {
				actionsLabel.setForeground(BUTTON_ON_FG_COLOR);
			}
		}
		actionsButtonsPanel.setOpaque(false);
		actionsPanel.add(actionsButtonsPanel, BorderLayout.EAST);
		actionsPanel.add(actionsLabel, BorderLayout.WEST);
		actionsPanel.setOpaque(false);
		return actionsPanel;
	}
	
	private JPanel getRGBSyntheticPanel(RGBInterface rgb, final Color foregroundColor, boolean ind0, boolean addEditButton) {
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		final JLabel label;// = new JLabel(rgb.getLabel() + " " + rgb.getGain() + "%");
		JButton button;
		if(ind0) {
			button = onOffButton0;
			label = label0;
			label.setText(rgb.getLabel() + " " + rgb.getGain() + "%");
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
			label = new JLabel(rgb.getLabel() + " " + rgb.getGain() + "%");
		}
		panel.add(label, BorderLayout.CENTER);
		label.setForeground(foregroundColor);
		if(rgb.isOn()) {
			button.setText(LABEL_ON);
			button.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			button.setText(LABEL_OFF);
			button.setBackground(BUTTON_OFF_BG_COLOR);
		}
		button.setForeground(rgb.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.add(button, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			panel.add(editSwitchPanel, BorderLayout.EAST);
		} else {
			panel.add(button, BorderLayout.EAST);
		}
		return panel;
	}
	
	private JPanel getWhiteSyntheticPanel(WhiteInterface light, final Color foregroundColor, boolean ind0, boolean addEditButton) {
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		final JLabel label;// = new JLabel(light.getLabel() + " " + light.getBrightness() + "%");
		final JButton button;
		if(ind0) {
			button = onOffButton0;
			label = label0;
			label.setText(light.getLabel() + " " + light.getBrightness() + "%");
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
			label = new JLabel(light.getLabel() + " " + light.getBrightness() + "%");
		}
		panel.add(label, BorderLayout.CENTER);
		label.setForeground(foregroundColor);
		if(light.isOn()) {
			button.setText(LABEL_ON);
			button.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			button.setText(LABEL_OFF);
			button.setBackground(BUTTON_OFF_BG_COLOR);
		}
		button.setForeground(light.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.add(button, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			panel.add(editSwitchPanel, BorderLayout.EAST);
		} else {
			panel.add(button, BorderLayout.EAST);
		}
		return panel;
	}
	
	private JPanel getWhitePanel(WhiteInterface light, final Color foregroundColor, boolean useButton0, boolean addEditButton) {
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel label;
		final JButton button;
		if(useButton0) {
			button = onOffButton0;
			label = label0;
			label.setText(light.getLabel() + " " + light.getBrightness() + "%");
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
			label = new JLabel(light.getLabel() + " " + light.getBrightness() + "%");
		}
		panel.add(label, BorderLayout.WEST);
		JSlider lightBrightness = new JSlider(light.getMinBrightness(), light.getMaxBrightness(), light.getBrightness());
		panel.add(lightBrightness, BorderLayout.SOUTH);
		
		if(light.isOn()) {
			button.setText(LABEL_ON);
			button.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			button.setText(LABEL_OFF);
			button.setBackground(BUTTON_OFF_BG_COLOR);
		}
		button.setForeground(light.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		label.setForeground(foregroundColor);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.add(button, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			panel.add(editSwitchPanel, BorderLayout.EAST);
			panel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			panel.add(button, BorderLayout.EAST);
		}
		return panel;
	}
	
	private JPanel getRGBPanel(RGBInterface light, final Color foregroundColor, boolean useButton0, boolean addEditButton) {
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel label;
		final JButton button;
		if(useButton0) {
			button = onOffButton0;
			label = label0;
			label.setText(light.getLabel() + " " + light.getGain() + "%");
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
			label = new JLabel(light.getLabel() + " " + light.getGain() + "%");
		}
		panel.add(label, BorderLayout.WEST);
		JSlider lightBrightness = new JSlider(0/*light.getMinBrightness()*/, 100/*light.getMaxBrightness()*/, light.getGain());
		panel.add(lightBrightness, BorderLayout.SOUTH);
		
		if(light.isOn()) {
			button.setText(LABEL_ON);
			button.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			button.setText(LABEL_OFF);
			button.setBackground(BUTTON_OFF_BG_COLOR);
		}
		button.setForeground(light.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		label.setForeground(foregroundColor);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.add(button, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			panel.add(editSwitchPanel, BorderLayout.EAST);
			panel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			panel.add(button, BorderLayout.EAST);
		}
		return panel;
	}
	
	private JPanel getRGBWPanel(RGBWInterface light, final Color foregroundColor, boolean useButton0, boolean addEditButton) {
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel label;
		final JButton button;
		if(useButton0) {
			button = onOffButton0;
			label = label0;
			label.setText(light.getLabel() + " " + light.getGain() + "%");
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
			label = new JLabel(light.getLabel() + " " + light.getGain() + "%");
		}
		panel.add(label, BorderLayout.WEST);

		final JPanel sliderPanel = new JPanel(new BorderLayout(0, 0));
		sliderPanel.setOpaque(false);
		JSlider lightBrightness = new JSlider(0/*light.getMinBrightness()*/, 100/*light.getMaxBrightness()*/, light.getGain());
		sliderPanel.add(lightBrightness, BorderLayout.NORTH);
		JSlider lightWhite = new JSlider(0, 255, light.getWhite());
		sliderPanel.add(lightWhite, BorderLayout.SOUTH);
		panel.add(sliderPanel, BorderLayout.SOUTH);
		
		if(light.isOn()) {
			button.setText(LABEL_ON);
			button.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			button.setText(LABEL_OFF);
			button.setBackground(BUTTON_OFF_BG_COLOR);
		}
		button.setForeground(light.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		lightBrightness.setValue(light.getGain());
		label.setForeground(foregroundColor);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.add(button, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			panel.add(editSwitchPanel, BorderLayout.EAST);
			panel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			panel.add(button, BorderLayout.EAST);
		}
		return panel;
	}
		
	private JPanel getRGBCCTPanel(RGBCCTInterface light, final Color foregroundColor, boolean useButton0, boolean addEditButton) {
		final JPanel panel = new JPanel(new BorderLayout());
		final JLabel label;
		final JButton button;
		final int sliderValue = light.isColorMode() ? light.getGain() : light.getBrightness();
		if(useButton0) {
			button = onOffButton0;
			label = label0;
			label.setText(light.getLabel() + " " + sliderValue + "%");
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
			label = new JLabel(light.getLabel() + " " + sliderValue + "%");
		}
		panel.add(label, BorderLayout.WEST);
		JSlider lightBrightness = new JSlider(light.getMinBrightness(), light.getMaxBrightness(), sliderValue);
		panel.add(lightBrightness, BorderLayout.SOUTH);
		
		if(light.isOn()) {
			button.setText(LABEL_ON);
			button.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			button.setText(LABEL_OFF);
			button.setBackground(BUTTON_OFF_BG_COLOR);
		}
		button.setForeground(light.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		label.setForeground(foregroundColor);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.add(button, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			panel.add(editSwitchPanel, BorderLayout.EAST);
			panel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			panel.add(button, BorderLayout.EAST);
		}
		return panel; 
	}

	private JPanel getThermostatPanel(ThermostatInterface thermostat, final Color foregroundColor) {
		JPanel thermPanel = new JPanel(new BorderLayout());
		JLabel thermProfileLabel = new JLabel();
		JSlider thermSlider = new JSlider(
				(int)(thermostat.getMinTargetTemp() * thermostat.getUnitDivision()),
				(int)(thermostat.getMaxTargetTemp() * thermostat.getUnitDivision()),
				(int)(thermostat.getTargetTemp() * thermostat.getUnitDivision()));
		
		thermPanel.add(thermProfileLabel, BorderLayout.CENTER);
		thermPanel.add(thermSlider, BorderLayout.SOUTH);
//		thermSlider.setPreferredSize(new Dimension(20, thermSlider.getPreferredSize().height));
		JPanel thermButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		thermButtonPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		thermButtonPanel.setOpaque(false);
		JButton thermButtonUp = new JButton(UP_IMG);
		thermButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton thermButtonDown = new JButton(DOWN_IMG);
		thermButtonDown.setBorder(BorderFactory.createEmptyBorder());
		JButton thermActiveButton = new JButton();
		thermActiveButton.setBorder(BUTTON_BORDERS_SMALLER);
		thermButtonPanel.add(thermActiveButton);
		thermButtonPanel.add(thermButtonUp);
		thermButtonPanel.add(thermButtonDown);
		thermPanel.add(thermButtonPanel, BorderLayout.EAST);

		if(tempUnitCelsius) {
			thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ thermostat.getTargetTemp() + "°C");
		} else {
			thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ (Math.round(thermostat.getTargetTemp() * 18f + 320f) / 10f) + "°F");
		}
		if(thermostat.isEnabled()) {
			thermActiveButton.setText(LABEL_ON);
			thermActiveButton.setBackground(BUTTON_ON_BG_COLOR);
			thermProfileLabel.setEnabled(true);
			thermActiveButton.setForeground(thermostat.isRunning() ? BUTTON_ON_FG_COLOR : null);
		} else {
			thermActiveButton.setText(LABEL_OFF);
			thermActiveButton.setBackground(BUTTON_OFF_BG_COLOR);
			thermProfileLabel.setEnabled(false);
			thermActiveButton.setForeground(null);
		}
		thermProfileLabel.setForeground(foregroundColor);
		return thermPanel; 
	}
	
	private JPanel getThermostatG1Panel(ThermostatG1 thermostat, final Color foregroundColor) { // TRV Gen1
		JPanel trvPanel = new JPanel(new BorderLayout());
		JLabel trvProfileLabel = new JLabel();
		JSlider trvSlider = new JSlider((int)(ThermostatG1.TARGET_MIN * 2), (int)(ThermostatG1.TARGET_MAX * 2), (int)(thermostat.getTargetTemp() * 2));
		
		trvPanel.add(trvProfileLabel, BorderLayout.CENTER);
		trvPanel.add(trvSlider, BorderLayout.SOUTH);
		trvSlider.setPreferredSize(new Dimension(20, trvSlider.getPreferredSize().height));
		JPanel trvButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		trvButtonPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		trvButtonPanel.setOpaque(false);
		JButton trvButtonUp = new JButton(UP_IMG);
		trvButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton trvButtonDown = new JButton(DOWN_IMG);
		trvButtonDown.setBorder(BorderFactory.createEmptyBorder());
		trvButtonPanel.add(trvButtonUp);
		trvButtonPanel.add(trvButtonDown);
		trvPanel.add(trvButtonPanel, BorderLayout.EAST);

		trvProfileLabel.setText(thermostat.getCurrentProfile() + " " + thermostat.getTargetTemp() + "°C");
		trvProfileLabel.setEnabled(thermostat.isScheduleActive());
		trvProfileLabel.setForeground(foregroundColor);
		
		return trvPanel; 
	}
	
	public void setTempUnit(boolean celsius) {
		tempUnitCelsius = celsius;
	}
}