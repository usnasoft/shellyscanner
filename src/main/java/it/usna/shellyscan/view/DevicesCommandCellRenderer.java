package it.usna.shellyscan.view;

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
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.RGBWInterface;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.shellyscan.model.device.modules.RollerInterface;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;
import it.usna.shellyscan.model.device.modules.WhiteInterface;
import it.usna.swing.VerticalFlowLayout;

public class DevicesCommandCellRenderer implements TableCellRenderer {
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
	private JPanel rollerPanel = new JPanel(new BorderLayout());
	private JLabel rollerLabel = new JLabel();
	private JSlider rollerPerc = new JSlider(0, 100);
	
	// LightWhite[] - rgbw2 white
	private JPanel editSwitchPanel = new JPanel(new BorderLayout());
	private JButton editLightWhiteButton = new JButton(new ImageIcon(getClass().getResource("/images/Write16.png")));
	
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
	
	static final Color BUTTON_ON_BG_COLOR = new Color(120, 212, 233);
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
	final static ImageIcon EDIT_IMG = new ImageIcon(DevicesCommandCellRenderer.class.getResource("/images/Write16.png"));

	public DevicesCommandCellRenderer() {
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
//		colorRGBWPanel.add(colorRGBWLabel, BorderLayout.WEST);
//		colorRGBWPanel.add(colorRGBWButton, BorderLayout.EAST);
//		colorRGBWButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
//		JPanel colorRGBWSlidersPanel = new JPanel();
//		BoxLayout colorRGBWSlidersPanelLO = new BoxLayout(colorRGBWSlidersPanel, BoxLayout.X_AXIS);
//		colorRGBWSlidersPanel.setLayout(colorRGBWSlidersPanelLO);
//		JPanel stackedLabels = new JPanel(new GridLayout(2, 1));
//		stackedLabels.setOpaque(false);
//		stackedLabels.add(colorRGBWGainLabel);
//		stackedLabels.add(colorRGBWhiteLabel);
//		colorRGBWSlidersPanel.add(stackedLabels);
//		JPanel stackedRGBWSliders = new JPanel(new GridLayout(2, 1));
//		stackedRGBWSliders.setOpaque(false);
//		stackedRGBWSliders.add(colorRGBWGain);
//		stackedRGBWSliders.add(colorRGBWWhite);
//		colorRGBWSlidersPanel.add(stackedRGBWSliders);
//		JButton editRGBWButton = new JButton(EDIT_IMG);
//		editRGBWButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3));
//		editRGBWButton.setContentAreaFilled(false);
//		colorRGBWSlidersPanel.add(editRGBWButton);
//		colorRGBWSlidersPanel.setOpaque(false);
//		colorRGBWPanel.add(colorRGBWSlidersPanel, BorderLayout.SOUTH);
		
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
		rgbwSliderPanel.add(colorRGBWGain, BorderLayout.CENTER);
		JButton editRGBWButton = new JButton(EDIT_IMG);
		editRGBWButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3));
		editRGBWButton.setContentAreaFilled(false);
		rgbwSliderPanel.add(editRGBWButton, BorderLayout.EAST);
		stackedRGBWSliders.add(rgbwSliderPanel);
		stackedRGBWSliders.add(colorRGBWWhite);
		colorRGBWSlidersPanel.add(stackedRGBWSliders);
		colorRGBWSlidersPanel.setOpaque(false);
		colorRGBWPanel.add(colorRGBWSlidersPanel, BorderLayout.SOUTH);
		
		// Roller
		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
		rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
		rollerPerc.setPreferredSize(new Dimension(20, rollerPerc.getPreferredSize().height));
		rollerSouthPanel.add(rollerButtonPanel, BorderLayout.EAST);
		JButton rollerButtonUp = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16up.png")));
		JButton rollerButtonDown = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16down.png")));
		JButton rollerButtonStop = new JButton(new ImageIcon(getClass().getResource("/images/PlayerStop.png")));
		rollerButtonUp.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonStop.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonDown.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonPanel.add(rollerButtonUp);
		rollerButtonPanel.add(rollerButtonStop);
		rollerButtonPanel.add(rollerButtonDown);
		rollerSouthPanel.setOpaque(false);
		rollerButtonPanel.setOpaque(false);
		rollerPanel.add(rollerSouthPanel, BorderLayout.SOUTH);
		
		// LightWhite[] - rgbw2 white
		editSwitchPanel.setOpaque(false);
		editLightWhiteButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		editLightWhiteButton.setContentAreaFilled(false);

		// Thermostat G1 (TRV)
		trvPanel.add(trvProfileLabel, BorderLayout.CENTER);
		trvPanel.add(trvSlider, BorderLayout.SOUTH);
		trvSlider.setPreferredSize(new Dimension(20, trvSlider.getPreferredSize().height));
		JPanel trvButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		trvButtonPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		trvButtonPanel.setOpaque(false);
		JButton trvButtonUp = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16up.png")));
		trvButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton trvButtonDown = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16down.png")));
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
		JButton thermButtonUp = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16up.png")));
		thermButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton thermButtonDown = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16down.png")));
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
			for(RelayInterface rel: riArray) { // 1, 1PM, EM, 2.5 ...
				stackedPanel.add(getRelayPanel(rel, foregroundColor));
			}
			ret = stackedPanel;
		} else if(value instanceof RollerInterface[] rollers) { // 2.5 ...
			RollerInterface roller = rollers[0]; // multiple rollers devices currently not supported
			String labelText;
			if(roller.isCalibrated()) {
				labelText = roller.getLabel() + " " + roller.getPosition() + "%";
				rollerPerc.setVisible(true);
			} else {
				labelText = roller.getLabel();
				rollerPerc.setVisible(false);
			}
			rollerPerc.setValue(roller.getPosition());
			rollerLabel.setText(labelText);
			rollerLabel.setForeground(foregroundColor);
			ret = rollerPanel;
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
			colorRGBWGainLabel.setText(Main.LABELS.getString("labelShortGain") + String.format("%-4s", color.getGain() + "%"));
			colorRGBWhiteLabel.setForeground(foregroundColor);
			colorRGBWhiteLabel.setText(Main.LABELS.getString("labelShortWhite") + color.getWhite() /*+ " "*/);
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
					WhiteInterface light = lights[i];
					JLabel relayLabel = new JLabel(light.getLabel() + " " + light.getBrightness() + "%");
					JPanel relayPanel = new JPanel(new BorderLayout());
					JButton button = new JButton();
					relayPanel.setOpaque(false);
					button.setBorder(BUTTON_BORDERS);
					relayPanel.add(relayLabel, BorderLayout.CENTER);
					relayLabel.setForeground(foregroundColor);
					if(light.isOn()) {
						button.setText(LABEL_ON);
						button.setBackground(BUTTON_ON_BG_COLOR);
					} else {
						button.setText(LABEL_OFF);
						button.setBackground(BUTTON_OFF_BG_COLOR);
					}
					if(light.isInputOn()) {
						button.setForeground(BUTTON_ON_FG_COLOR);
					}
					if(++i < lights.length) {
						relayPanel.add(button, BorderLayout.EAST);
					} else {
						editSwitchPanel.removeAll();
						editSwitchPanel.add(button, BorderLayout.EAST);
						editSwitchPanel.add(BorderLayout.WEST, editLightWhiteButton);
						relayPanel.add(editSwitchPanel, BorderLayout.EAST);
					}
					stackedPanel.add(relayPanel);
				}
				ret = stackedPanel;
			}
		} else if(value instanceof InputInterface[] inputs) { // Button1 - I3 - I4
			stackedPanel.removeAll();
			for(InputInterface inp: inputs) {
				if(inp.enabled()) {
					stackedPanel.add(getInputPanel(inp, foregroundColor));
				}
			}
			ret = stackedPanel;
		} else if(value instanceof ThermostatG1 thermostat) { // TRV
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
			thermProfileLabel.setText(/*thermostat.getCurrentProfile()*//*thermostat.getLabel() + " " +*/ thermostat.getTargetTemp() + "°C");
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
		} else if(value instanceof DeviceModule[] devArray) { // mixed modules
			stackedPanel.removeAll();
			for(DeviceModule module: devArray) {
				if(module instanceof RelayInterface rel) {
					stackedPanel.add(getRelayPanel(rel, foregroundColor));
				} else if(module instanceof InputInterface input && input.enabled()) {
					stackedPanel.add(getInputPanel(input, foregroundColor));
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

	private JPanel getRelayPanel(RelayInterface rel, final Color foregroundColor) {
		JLabel relayLabel = new JLabel(rel.getLabel());
		relayLabel.setForeground(foregroundColor);
		JPanel relayPanel = new JPanel(new BorderLayout());
		JButton relayButton = new JButton();
		relayButton.setBorder(BUTTON_BORDERS);
		
		JPanel relayButtonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));
		relayButtonPanel.setOpaque(false);
		relayButtonPanel.add(relayButton);

		relayPanel.setOpaque(false);
		relayPanel.add(relayLabel, BorderLayout.CENTER);
		relayPanel.add(relayButtonPanel, BorderLayout.EAST);
		if(rel.isOn()) {
			relayButton.setText(LABEL_ON);
			relayButton.setBackground(BUTTON_ON_BG_COLOR);
		} else {
			relayButton.setText(LABEL_OFF);
			relayButton.setBackground(BUTTON_OFF_BG_COLOR);
		}
		if(rel.isInputOn()) {
			relayButton.setForeground(BUTTON_ON_FG_COLOR);
		}
		return relayPanel;
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
						bLabel = Main.LABELS.getString(type);
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
}