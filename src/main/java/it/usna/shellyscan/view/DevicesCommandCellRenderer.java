package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.g1.modules.LightBulbRGB;
import it.usna.shellyscan.model.device.g1.modules.ThermostatG1;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.InputInterface;
import it.usna.shellyscan.model.device.modules.MotionInterface;
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.RGBWInterface;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.shellyscan.model.device.modules.RollerInterface;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;
import it.usna.shellyscan.model.device.modules.WhiteInterface;
import it.usna.swing.VerticalFlowLayout;

public class DevicesCommandCellRenderer implements TableCellRenderer {
	// Generic
	final static ImageIcon EDIT_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/Write16.png"));
	final static ImageIcon UP_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/Arrow16up.png"));
	final static ImageIcon DOWN_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/Arrow16down.png"));
	final static ImageIcon STOP_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/PlayerStop.png"));
	private JButton onOffButton0 = new JButton();
	private JLabel label0 = new JLabel();
	private JButton editDialogButton = new JButton(EDIT_IMG);
	
	// Dimmer
	private JPanel lightPanel = new JPanel(new BorderLayout());
	private JLabel lightLabel = new JLabel();
	private JButton lightButton = new JButton();
	private JSlider lightBrightness = new JSlider();
	
	// RGBW Bulbs
	private JPanel lightRGBBulbPanel = new JPanel(new BorderLayout());
	private JLabel lightRGBBulbLabel = new JLabel();
	private JButton lightRGBBulbButton = new JButton();
	private JSlider lightRGBBulbBrightness = new JSlider(0, 100);
	
	// RGB
	private JPanel colorRGBPanel = new JPanel(new BorderLayout());
	private JLabel colorRGBLabel = new JLabel();
	private JButton colorRGBButton = new JButton();
	private JSlider colorRGBBrightness = new JSlider(0, 100);
	
	// RGBW color
	private JPanel colorRGBWPanel = new JPanel(new BorderLayout());
	private JLabel colorRGBWLabel = new JLabel();
	private JButton colorRGBWButton = new JButton();
	private JSlider colorRGBWGain = new JSlider(0, 100);
	private JSlider colorRGBWWhite = new JSlider(0, 255);
	private JLabel colorRGBWGainLabel = new JLabel();
	private JLabel colorRGBWhiteLabel = new JLabel();
	
	// Roller
//	private JPanel rollerPanel = new JPanel(new BorderLayout());
//	private JLabel rollerLabel = new JLabel();
//	private JSlider rollerPerc = new JSlider(0, 100);
	
	// Thermostat G1 (TRV)
	private JPanel trvPanel = new JPanel(new BorderLayout());
	private JLabel trvProfileLabel = new JLabel();
	private JSlider trvSlider = new JSlider((int)(ThermostatG1.TARGET_MIN * 2), (int)(ThermostatG1.TARGET_MAX * 2));
	
	// ThermostatInterface
	private JPanel thermPanel = new JPanel(new BorderLayout());
	private JLabel thermProfileLabel = new JLabel();
	private JSlider thermSlider = new JSlider();
	private JButton thermActiveButton = new JButton();

	private JPanel stackedPanel = new JPanel();
	
	private JLabel labelPlain = new JLabel();
	
	static final Color BUTTON_ON_BG_COLOR = new Color(125, 217, 240);
	static final Color BUTTON_OFF_BG_COLOR = Color.WHITE;
	static final Color BUTTON_ON_FG_COLOR = new Color(210, 120, 0);
	static final int MAX_ACTIONS_SHOWN = 5; // if supported actions <= then show also disabled buttons
	private static final int BUTTON_MARGIN_H = 12;
	private static final int BUTTON_MARGIN_V = 1;
	final static Border BUTTON_BORDERS = BorderFactory.createEmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H, BUTTON_MARGIN_V, BUTTON_MARGIN_H);
//	final static Border BUTTON_BORDERS_SMALL = BorderFactory.createEmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H-2, BUTTON_MARGIN_V, BUTTON_MARGIN_H-2);
	final static Border BUTTON_BORDERS_SMALLER = BorderFactory.createEmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H-5, BUTTON_MARGIN_V, BUTTON_MARGIN_H-5);
	final static String LABEL_ON = Main.LABELS.getString("btnOnLabel");
	final static String LABEL_OFF = Main.LABELS.getString("btnOffLabel");
	
	private boolean tempUnitCelsius;

	public DevicesCommandCellRenderer(boolean celsius) {
		this.tempUnitCelsius = celsius;
		// Generic
		onOffButton0.setBorder(BUTTON_BORDERS);
		editDialogButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		editDialogButton.setContentAreaFilled(false);
		
		// Dimmer
		lightButton.setBorder(BUTTON_BORDERS);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightPanel.add(lightLabel, BorderLayout.WEST);
		lightPanel.add(lightBrightness, BorderLayout.SOUTH);
//		lightBrightness.setPreferredSize(new Dimension(20, lightBrightness.getPreferredSize().height));
		
		// RGBW Bulbs
		JPanel lightRGBBulbSouthPanel = new JPanel(new BorderLayout());
		lightRGBBulbSouthPanel.setOpaque(false);
		JButton lightEditRGBBulbButton = new JButton(EDIT_IMG);
		lightEditRGBBulbButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		lightEditRGBBulbButton.setContentAreaFilled(false);
		lightRGBBulbButton.setBorder(BUTTON_BORDERS);
		lightRGBBulbPanel.add(lightRGBBulbLabel, BorderLayout.CENTER);
		lightRGBBulbPanel.add(lightRGBBulbButton, BorderLayout.EAST);
		lightRGBBulbPanel.add(lightRGBBulbSouthPanel, BorderLayout.SOUTH);
		lightRGBBulbSouthPanel.add(lightRGBBulbBrightness, BorderLayout.CENTER);
//		lightRGBBulbBrightness.setPreferredSize(new Dimension(20, lightRGBBulbBrightness.getPreferredSize().height));
		lightRGBBulbSouthPanel.add(lightEditRGBBulbButton, BorderLayout.EAST);
		
		// RGB
		JPanel lightRGBSouthPanel = new JPanel(new BorderLayout());
		lightRGBSouthPanel.setOpaque(false);
		JButton lightEditRGBButton = new JButton(EDIT_IMG);
		lightEditRGBButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		lightEditRGBButton.setContentAreaFilled(false);
		colorRGBButton.setBorder(BUTTON_BORDERS);
		colorRGBPanel.add(colorRGBLabel, BorderLayout.WEST);
		colorRGBPanel.add(colorRGBButton, BorderLayout.EAST);
//		colorRGBlightBrightness.setPreferredSize(new Dimension(20, lightBrightness.getPreferredSize().height));
		lightRGBSouthPanel.add(colorRGBBrightness, BorderLayout.CENTER);
		lightRGBSouthPanel.add(lightEditRGBButton, BorderLayout.EAST);
		colorRGBPanel.add(lightRGBSouthPanel, BorderLayout.SOUTH);
		
		// RGBW
		colorRGBWPanel.add(colorRGBWLabel, BorderLayout.WEST);
		colorRGBWPanel.add(colorRGBWButton, BorderLayout.EAST);
		colorRGBWButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		JPanel colorRGBWSlidersPanel = new JPanel();
		BoxLayout colorRGBWSlidersPanelLO = new BoxLayout(colorRGBWSlidersPanel, BoxLayout.X_AXIS);
		colorRGBWSlidersPanel.setLayout(colorRGBWSlidersPanelLO);
		JPanel stackedLabels = new JPanel(new GridLayout(2, 1));
		stackedLabels.setOpaque(false);
		stackedLabels.add(colorRGBWGainLabel);
		stackedLabels.add(colorRGBWhiteLabel);
		colorRGBWSlidersPanel.add(stackedLabels);
		JPanel stackedRGBWSliders = new JPanel(new GridLayout(2, 1));
		stackedRGBWSliders.setOpaque(false);
		JPanel rgbwSliderPanel = new JPanel(new BorderLayout());
		rgbwSliderPanel.setOpaque(false);
		rgbwSliderPanel.add(colorRGBWWhite, BorderLayout.CENTER);
		JButton editRGBWButton = new JButton(EDIT_IMG);
		editRGBWButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3));
		editRGBWButton.setContentAreaFilled(false);
		rgbwSliderPanel.add(editRGBWButton, BorderLayout.EAST);
		stackedRGBWSliders.add(colorRGBWGain);
		stackedRGBWSliders.add(rgbwSliderPanel);
		colorRGBWSlidersPanel.add(stackedRGBWSliders);
		colorRGBWSlidersPanel.setOpaque(false);
		colorRGBWPanel.add(colorRGBWSlidersPanel, BorderLayout.SOUTH);
		
		// Roller
//		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
//		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
//		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
//		rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
//		rollerPerc.setPreferredSize(new Dimension(20, rollerPerc.getPreferredSize().height));
//		rollerSouthPanel.add(rollerButtonPanel, BorderLayout.EAST);
//		JButton rollerButtonUp = new JButton(UP_IMG);
//		JButton rollerButtonDown = new JButton(DOWN_IMG);
//		JButton rollerButtonStop = new JButton(new ImageIcon(getClass().getResource("/images/PlayerStop.png")));
//		rollerButtonUp.setBorder(BorderFactory.createEmptyBorder());
//		rollerButtonStop.setBorder(BorderFactory.createEmptyBorder());
//		rollerButtonDown.setBorder(BorderFactory.createEmptyBorder());
//		rollerButtonPanel.add(rollerButtonUp);
//		rollerButtonPanel.add(rollerButtonStop);
//		rollerButtonPanel.add(rollerButtonDown);
//		rollerSouthPanel.setOpaque(false);
//		rollerButtonPanel.setOpaque(false);
//		rollerPanel.add(rollerSouthPanel, BorderLayout.SOUTH);

		// Thermostat G1 (TRV)
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
		
		// ThermostatInterface
		thermPanel.add(thermProfileLabel, BorderLayout.CENTER);
		thermPanel.add(thermSlider, BorderLayout.SOUTH);
		thermSlider.setPreferredSize(new Dimension(20, thermSlider.getPreferredSize().height));
		JPanel thermButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		thermButtonPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		thermButtonPanel.setOpaque(false);
		JButton thermButtonUp = new JButton(UP_IMG);
		thermButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton thermButtonDown = new JButton(DOWN_IMG);
		thermButtonDown.setBorder(BorderFactory.createEmptyBorder());
		thermActiveButton.setBorder(BUTTON_BORDERS_SMALLER);
		thermButtonPanel.add(thermActiveButton);
		thermButtonPanel.add(thermButtonUp);
		thermButtonPanel.add(thermButtonDown);
		thermPanel.add(thermButtonPanel, BorderLayout.EAST);
		
		BoxLayout stackedPanelLO = new BoxLayout(stackedPanel, BoxLayout.Y_AXIS);
		stackedPanel.setLayout(stackedPanelLO);
		
		labelPlain.setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//		try {
		final JComponent ret;
		final Color foregroundColor = isSelected ? table.getSelectionForeground() : table.getForeground();
		if(value instanceof RelayInterface[] riArray) {
			stackedPanel.removeAll();
			for(int i = 0; i < riArray.length; i++) { // 1, 1PM, EM, 2.5 ...
				stackedPanel.add(getRelayPanel(riArray[i], foregroundColor, i == 0));
			}
			ret = stackedPanel;
		} else if(value instanceof RollerInterface[] rollers) { // 2.5 ...
//			RollerInterface roller = rollers[0]; // multiple rollers devices currently not supported
//			String labelText;
//			if(roller.isCalibrated()) {
//				labelText = roller.getLabel() + " " + roller.getPosition() + "%";
//				rollerPerc.setVisible(true);
//			} else {
//				labelText = roller.getLabel();
//				rollerPerc.setVisible(false);
//			}
//			rollerPerc.setValue(roller.getPosition());
//			rollerLabel.setText(labelText);
//			rollerLabel.setForeground(foregroundColor);
//			ret = rollerPanel;
			stackedPanel.removeAll();
			for(int i = 0; i < rollers.length; i++) { // 1, 1PM, EM, 2.5 ...
				stackedPanel.add(getRollerPanel(rollers[i], foregroundColor, i == 0));
			}
			ret = stackedPanel;
		} else if(value instanceof LightBulbRGB[] lights) { // RGBW Bulbs
			LightBulbRGB light = lights[0]; // multiple bulbs devices currently not supported
			if(light.isOn()) {
				lightRGBBulbButton.setText(LABEL_ON);
				lightRGBBulbButton.setBackground(BUTTON_ON_BG_COLOR);
			} else {
				lightRGBBulbButton.setText(LABEL_OFF);
				lightRGBBulbButton.setBackground(BUTTON_OFF_BG_COLOR);
			}
			final int slider = light.isColorMode() ? light.getGain() : light.getBrightness();
			lightRGBBulbBrightness.setValue(slider);
			lightRGBBulbLabel.setText(light.getLabel() + " " + slider + "%");
			lightRGBBulbLabel.setForeground(foregroundColor);
			ret = lightRGBBulbPanel;
		} else if(value instanceof RGBWInterface[] rgbws) { // RGBWs
			RGBWInterface color = rgbws[0];
			if(color.isOn()) {
				colorRGBWButton.setText(LABEL_ON);
				colorRGBWButton.setBackground(BUTTON_ON_BG_COLOR);
			} else {
				colorRGBWButton.setText(LABEL_OFF);
				colorRGBWButton.setBackground(BUTTON_OFF_BG_COLOR);
			}
			colorRGBWButton.setForeground(color.isInputOn() ? BUTTON_ON_FG_COLOR : null);
			colorRGBWGain.setValue(color.getGain());
			colorRGBWWhite.setValue(color.getWhite());
			colorRGBWLabel.setText(color.getLabel());
			colorRGBWLabel.setForeground(foregroundColor);
			colorRGBWGainLabel.setForeground(foregroundColor);
			colorRGBWGainLabel.setText(/*Main.LABELS.getString("labelShortGain") +*/ String.format("%-5s", color.getGain() + "%"));
			colorRGBWhiteLabel.setForeground(foregroundColor);
			colorRGBWhiteLabel.setText(String.format("%-5s", Main.LABELS.getString("labelShortWhite") + color.getWhite()));
			ret = colorRGBWPanel;
		} else if(value instanceof RGBInterface[] rgbs) { // RGBs
			RGBInterface rgb = rgbs[0];
			if(rgb.isOn()) {
				colorRGBButton.setText(LABEL_ON);
				colorRGBButton.setBackground(BUTTON_ON_BG_COLOR);
			} else {
				colorRGBButton.setText(LABEL_OFF);
				colorRGBButton.setBackground(BUTTON_OFF_BG_COLOR);
			}
			colorRGBButton.setForeground(rgb.isInputOn() ? BUTTON_ON_FG_COLOR : null);
			colorRGBBrightness.setValue(rgb.getGain());
			colorRGBLabel.setText(rgb.getLabel() + " " + rgb.getGain() + "%");
			colorRGBLabel.setForeground(foregroundColor);
			ret = colorRGBPanel;
		} else if(value instanceof WhiteInterface[] lights) { // Dimmerable white(s)
			if(lights.length == 1) {
				WhiteInterface light = lights[0];
				if(light.isOn()) {
					lightButton.setText(LABEL_ON);
					lightButton.setBackground(BUTTON_ON_BG_COLOR);
				} else {
					lightButton.setText(LABEL_OFF);
					lightButton.setBackground(BUTTON_OFF_BG_COLOR);
				}
				lightButton.setForeground(light.isInputOn() ? BUTTON_ON_FG_COLOR : null);
				lightBrightness.setMinimum(light.getMinBrightness());
				lightBrightness.setMaximum(light.getMaxBrightness());
				lightBrightness.setValue(light.getBrightness());
				lightLabel.setText(light.getLabel() + " " + light.getBrightness() + "%");
				lightLabel.setForeground(foregroundColor);
				ret = lightPanel;
			} else {
				stackedPanel.removeAll();
				for(int i = 0; i < lights.length;) {
					JPanel panel = getWhiteSyntheticPanel(lights[i], foregroundColor, i == 0, ++i == lights.length);
					stackedPanel.add(panel);
				}
				ret = stackedPanel;
			}
//		} else if(value instanceof InputInterface[] inputs) { // Button1 - I3 - I4
//			stackedPanel.removeAll();
//			for(InputInterface inp: inputs) {
//				if(inp.enabled()) {
//					stackedPanel.add(getInputPanel(inp, foregroundColor));
//				}
//			}
//			ret = stackedPanel;
		} else if(value instanceof ThermostatG1 thermostat) { // TRV gen1
			trvSlider.setValue((int)(thermostat.getTargetTemp() * 2));
			trvProfileLabel.setText(thermostat.getCurrentProfile() + " " + thermostat.getTargetTemp() + "°C");
			trvProfileLabel.setEnabled(thermostat.isScheduleActive());
			trvProfileLabel.setForeground(foregroundColor);
			ret = trvPanel;
		} else if(value instanceof ThermostatInterface[] thermostats) {
			ThermostatInterface thermostat = thermostats[0]; // multiple thermostats devices currently not supported
			thermSlider.setMinimum((int)(thermostat.getMinTargetTemp() * thermostat.getUnitDivision()));
			thermSlider.setMaximum((int)(thermostat.getMaxTargetTemp() * thermostat.getUnitDivision()));
			thermSlider.setValue((int)(thermostat.getTargetTemp() * thermostat.getUnitDivision()));
			if(tempUnitCelsius) {
				thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ thermostat.getTargetTemp() + "°C");
			} else {
				thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ (Math.round(thermostat.getTargetTemp() * 18f + 320f) / 10f) + "°F");
//				thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ String.format(Locale.ENGLISH, "%.1f°F", thermostat.getTargetTemp() * 1.8f + 32f));
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
			ret = thermPanel;
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
//		} catch(Exception e) {e.printStackTrace(); return null;}
	}

	private JPanel getRelayPanel(RelayInterface rel, final Color foregroundColor, boolean ind0) {
		JPanel relayPanel = new JPanel(new BorderLayout());
		final JLabel relayLabel;// = new JLabel(rel.getLabel());
		final JButton button;
		if(ind0) {
			relayLabel = label0;
			relayLabel.setText(rel.getLabel());
			button = onOffButton0;
		} else {
			relayLabel = new JLabel(rel.getLabel());
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
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
		button.setForeground(rel.isInputOn() ? BUTTON_ON_FG_COLOR : null);
		return relayPanel;
	}
	
	private JPanel getRollerPanel(RollerInterface roller, final Color foregroundColor, boolean ind0) {
		JPanel rollerPanel = new JPanel(new BorderLayout());
		rollerPanel.setOpaque(false);
		JLabel rollerLabel = (ind0) ? label0 : new JLabel();
		
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
			rollerSlider.setPreferredSize(new Dimension(20, rollerSlider.getPreferredSize().height));
			rollerSouthPanel.add(rollerSlider, BorderLayout.CENTER);
		} else {
			rollerLabel.setText(roller.getLabel());
		}
		rollerLabel.setForeground(foregroundColor);
		return rollerPanel;
	}
	
	private JPanel getInputPanel(InputInterface inp, final Color foregroundColor) {
		JPanel actionsPanel = new JPanel(new BorderLayout());
		String inpName = inp.getLabel();
		JLabel actionsLabel = new JLabel(inpName == null || inpName.isEmpty() ? "\u25CB" : inpName);
		actionsLabel.setForeground(foregroundColor);
		
		JPanel actionsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		int numEvents = inp.getRegisteredEventsCount();
		if(numEvents > 0) {
			for(String type: inp.getRegisteredEvents()) { // webhooks
				boolean enabled = inp.enabled(type);
				if(enabled || numEvents <= MAX_ACTIONS_SHOWN) {
					String bLabel;
					try {
						bLabel = LABELS.getString(type);
					} catch( MissingResourceException e) {
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
	
	private JPanel getRGBSyntheticPanel(RGBInterface rgb, final Color foregroundColor, boolean useButton0, boolean addEditButton) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		JLabel label = new JLabel(rgb.getLabel() + " " + rgb.getGain() + "%");
		panel.add(label, BorderLayout.CENTER);
		label.setForeground(foregroundColor);
		JButton button;
		if(useButton0) {
			button = onOffButton0;
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
		}
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
	
	private JPanel getWhiteSyntheticPanel(WhiteInterface light, final Color foregroundColor, boolean useButton0, boolean addEditButton) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		JLabel label = new JLabel(light.getLabel() + " " + light.getBrightness() + "%");
		panel.add(label, BorderLayout.CENTER);
		label.setForeground(foregroundColor);
		JButton button;
		if(useButton0) {
			button = onOffButton0;
		} else {
			button = new JButton();
			button.setBorder(BUTTON_BORDERS);
		}
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
	
	public void setTempUnit(boolean celsius) {
		tempUnitCelsius = celsius;
	}
}