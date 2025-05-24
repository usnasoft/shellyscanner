package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.IOException;
import java.util.EventObject;
import java.util.MissingResourceException;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
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
import it.usna.shellyscan.view.lightsEditor.DialogEditLights;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.VerticalFlowLayout;

public class DevicesCommandCellEditor extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(DevicesCommandCellEditor.class);
	private Object edited;
	
	// Generic
	private JButton editDialogButton = new JButton(DevicesCommandCellRenderer.EDIT_IMG);

	// Dimmer
	private JLabel lightLabel = new JLabel();
	private JPanel lightPanel = new JPanel(new BorderLayout());
	private JButton lightButton = new JButton();
	private JSlider lightBrightness = new JSlider();
	
	// RGBW Bulbs
	private JLabel lightRGBBulbLabel = new JLabel();
	private JPanel lightRGBBulbPanel = new JPanel(new BorderLayout());
	private JButton lightRGBBulbButton = new JButton();
	private JSlider lightRGBBulbBrightness = new JSlider(0, 100);
	
	// RGB
	private JPanel colorRGBPanel = new JPanel(new BorderLayout());
	private JLabel colorRGBLabel = new JLabel();
	private JButton colorRGBButton = new JButton();
	private JSlider colorRGBBrightness = new JSlider(0, 100);
	private JPanel lightRGBSouthPanel = new JPanel(new BorderLayout());
	
	// RGBW
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
	
	private final Color selBackground;
	private final Color selForeground;
	
	private boolean tempUnitCelsius;

	public DevicesCommandCellEditor(JTable table, boolean celsius) {
		this.tempUnitCelsius = celsius;
		this.selBackground = table.getSelectionBackground();
		this.selForeground = table.getSelectionForeground();
		
		// Generic
		editDialogButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		editDialogButton.setContentAreaFilled(false);
		editDialogButton.addActionListener(e -> {
			final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
			win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if(edited != null && edited instanceof DeviceModule[] modules) {
				final String title;
				if(modules[0] instanceof WhiteInterface w) {
					title = UtilMiscellaneous.getDescName(w.getParent());
				} else if(modules[0] instanceof RGBInterface rgb) {
					title = UtilMiscellaneous.getDescName(rgb.getParent());
				} else {
					title = modules[0].getLabel();
				}
				new DialogEditLights(win, title, modules);
			}
			cancelCellEditing();
			win.setCursor(Cursor.getDefaultCursor());
		});
		
		// Dimmer (single)
		lightPanel.setBackground(selBackground);
		lightLabel.setForeground(selForeground);
		lightPanel.add(lightLabel, BorderLayout.WEST);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		lightButton.addActionListener(e -> {
			if(edited != null && edited instanceof WhiteInterface[] wi) {
				try {
					wi[0].toggle();
				} catch (IOException ex) {
					LOG.error("lightButton", ex);
				}
				cancelCellEditing();
			}
		});
		lightPanel.add(lightBrightness, BorderLayout.SOUTH);
		lightBrightness.addChangeListener(e -> {
			if(edited != null && edited instanceof WhiteInterface[] wi) {
				if(lightBrightness.getValueIsAdjusting()) {
					lightLabel.setText(wi[0].getLabel() + " " + lightBrightness.getValue() + "%");
				} else {
					try {
						wi[0].setBrightness(lightBrightness.getValue());
					} catch (IOException ex) {
						LOG.error("lightBrightness", ex);
					}
					cancelCellEditing();
				}
			}
		});
		
		// RGBW Bulbs
		lightRGBBulbPanel.setBackground(selBackground);
		lightRGBBulbLabel.setForeground(selForeground);
		lightRGBBulbPanel.add(lightRGBBulbLabel, BorderLayout.CENTER);
		lightRGBBulbPanel.add(lightRGBBulbButton, BorderLayout.EAST);
		lightRGBBulbButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		lightRGBBulbButton.addActionListener(e -> {
			if(edited != null && edited instanceof LightBulbRGB[] bulbs) {
				try {
					bulbs[0].toggle();
				} catch (IOException ex) {
					LOG.error("lightRGBButton", ex);
				}
				cancelCellEditing();
			}
		});
		JPanel lightRGBBulbSouthPanel = new JPanel(new BorderLayout());
		JButton lightEditRGBBulbButton = new JButton(DevicesCommandCellRenderer.EDIT_IMG);
		lightRGBBulbPanel.add(lightRGBBulbSouthPanel, BorderLayout.SOUTH);
		lightRGBBulbSouthPanel.setOpaque(false);
		lightRGBBulbSouthPanel.add(lightRGBBulbBrightness, BorderLayout.CENTER);
		lightEditRGBBulbButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		lightEditRGBBulbButton.setContentAreaFilled(false);
		lightEditRGBBulbButton.addActionListener(e -> {
			if(edited != null && edited instanceof LightBulbRGB[] bulbs) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				new DialogEditBulbRGB(win, bulbs[0]);
				cancelCellEditing();
				win.setCursor(Cursor.getDefaultCursor());
			}
		});
		lightRGBBulbSouthPanel.add(lightEditRGBBulbButton, BorderLayout.EAST);
		lightRGBBulbBrightness.addChangeListener(e -> {
			if(edited != null && edited instanceof LightBulbRGB[] bulbs) {
				LightBulbRGB light = bulbs[0];
				if(lightRGBBulbBrightness.getValueIsAdjusting()) {
					lightRGBBulbLabel.setText(light.getLabel() + " " + lightRGBBulbBrightness.getValue() + "%");
				} else {
					try {
						if(light.isColorMode()) {
							light.setGain(lightRGBBulbBrightness.getValue());
						} else {
							light.setBrightness(lightRGBBulbBrightness.getValue());
						}
					} catch (IOException ex) {
						LOG.error("lightRGBBrightness", ex);
					}
					cancelCellEditing();
				}
			}
		});
		
		// RGB
		colorRGBPanel.setBackground(selBackground);
		colorRGBLabel.setForeground(selForeground);
		colorRGBButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		colorRGBPanel.add(colorRGBLabel, BorderLayout.WEST);
		colorRGBPanel.add(colorRGBButton, BorderLayout.EAST);
		lightRGBSouthPanel.setOpaque(false);
		lightRGBSouthPanel.add(colorRGBBrightness, BorderLayout.CENTER);
		colorRGBPanel.add(lightRGBSouthPanel, BorderLayout.SOUTH);
		colorRGBButton.addActionListener(e -> {
			if(edited != null && edited instanceof RGBInterface[] rgbs) {
				try {
					rgbs[0].toggle();
				} catch (IOException ex) {
					LOG.error("lightRGBButton", ex);
				}
				cancelCellEditing();
			}
		});
		colorRGBBrightness.addChangeListener(e -> {
			if(edited != null && edited instanceof RGBInterface[] rgbs) {
				if(colorRGBBrightness.getValueIsAdjusting()) {
					colorRGBWGainLabel.setText(rgbs[0].getLabel() + " " + colorRGBBrightness.getValue() + "%");
				} else {
					try {
						rgbs[0].setGain(colorRGBBrightness.getValue());
					} catch (IOException ex) {
						LOG.error("colorRGBGain", ex);
					}
					cancelCellEditing();
				}
			}
		});
		
		// RGBW (color)
		colorRGBWPanel.setBackground(selBackground);
		colorRGBWLabel.setForeground(selForeground);
		colorRGBWPanel.add(colorRGBWLabel, BorderLayout.WEST);
		colorRGBWPanel.add(colorRGBWButton, BorderLayout.EAST);
		colorRGBWButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		colorRGBWButton.addActionListener(e -> {
			if(edited != null && edited instanceof RGBWInterface[] rgbws) {
				try {
					rgbws[0].toggle();
				} catch (IOException ex) {
					LOG.error("colorRGBButton", ex);
				}
				cancelCellEditing();
			}
		});
		colorRGBWGain.addChangeListener(e -> {
			if(edited != null && edited instanceof RGBWInterface[] rgbws) {
				if(colorRGBWGain.getValueIsAdjusting()) {
					colorRGBWGainLabel.setText(/*LABELS.getString("labelShortGain") +*/ String.format("%-5s", colorRGBWGain.getValue() + "%"));
				} else {
					try {
						rgbws[0].setGain(colorRGBWGain.getValue());
					} catch (IOException ex) {
						LOG.error("colorRGBGain", ex);
					}
					cancelCellEditing();
				}
			}
		});
		colorRGBWWhite.addChangeListener(e -> {
			if(edited != null && edited instanceof RGBWInterface[] rgbws) {
				if(colorRGBWWhite.getValueIsAdjusting()) {
					colorRGBWhiteLabel.setText(String.format("%-5s", LABELS.getString("labelShortWhite") + colorRGBWWhite.getValue()));
				} else {
					try {
						rgbws[0].setWhite(colorRGBWWhite.getValue());
					} catch (IOException ex) {
						LOG.error("colorRGBBWhite", ex);
					}
					cancelCellEditing();
				}
			}
		});
		JPanel colorRGBWSlidersPanel = new JPanel();
		BoxLayout colorRGBWSlidersPanelLO = new BoxLayout(colorRGBWSlidersPanel, BoxLayout.X_AXIS);
		colorRGBWSlidersPanel.setLayout(colorRGBWSlidersPanelLO);
		JPanel stackedRGBWLabels = new JPanel(new GridLayout(2, 1));
		stackedRGBWLabels.setOpaque(false);
		colorRGBWGainLabel.setForeground(selForeground);
		colorRGBWhiteLabel.setForeground(selForeground);
		stackedRGBWLabels.add(colorRGBWGainLabel);
		stackedRGBWLabels.add(colorRGBWhiteLabel);
		colorRGBWSlidersPanel.add(stackedRGBWLabels);
		JPanel stackedRGBWSliders = new JPanel(new GridLayout(2, 1));
		JPanel rgbwSliderPanel = new JPanel(new BorderLayout());
		rgbwSliderPanel.setOpaque(false);
		rgbwSliderPanel.add(colorRGBWWhite, BorderLayout.CENTER);
		stackedRGBWSliders.setOpaque(false);
		stackedRGBWSliders.add(colorRGBWGain);
		stackedRGBWSliders.add(rgbwSliderPanel);
		colorRGBWSlidersPanel.add(stackedRGBWSliders);
		JButton editRGBWButton = new JButton(DevicesCommandCellRenderer.EDIT_IMG);
		editRGBWButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3));
		editRGBWButton.setContentAreaFilled(false);
		editRGBWButton.addActionListener(e -> {
			if(edited != null && edited instanceof RGBWInterface[] rgbws) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				new DialogEditLights(win, UtilMiscellaneous.getDescName(rgbws[0].getParent()), rgbws);
				cancelCellEditing();
				win.setCursor(Cursor.getDefaultCursor());
			}
		});
		rgbwSliderPanel.add(editRGBWButton, BorderLayout.EAST);
		colorRGBWSlidersPanel.setOpaque(false);
		colorRGBWPanel.add(colorRGBWSlidersPanel, BorderLayout.SOUTH);
		
		// Roller
//		rollerPanel.setBackground(selBackground);
//		rollerLabel.setForeground(selForeground);
//		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
//		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
//		rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
//		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
//		rollerSouthPanel.add(rollerButtonPanel, BorderLayout.EAST);
//		JButton rollerButtonUp = new JButton(DevicesCommandCellRenderer.UP_IMG);
//		JButton rollerButtonDown = new JButton(DevicesCommandCellRenderer.DOWN_IMG);
//		JButton rollerButtonStop = new JButton(DevicesCommandCellRenderer.STOP_IMG);
//		rollerButtonUp.setBorder(BorderFactory.createEmptyBorder());
//		rollerButtonStop.setBorder(BorderFactory.createEmptyBorder());
//		rollerButtonDown.setBorder(BorderFactory.createEmptyBorder());
//		rollerButtonPanel.add(rollerButtonUp);
//		rollerButtonPanel.add(rollerButtonStop);
//		rollerButtonPanel.add(rollerButtonDown);
//		rollerSouthPanel.setOpaque(false);
//		rollerButtonPanel.setOpaque(false);
//		rollerPanel.add(rollerSouthPanel, BorderLayout.SOUTH);
//		rollerButtonUp.addActionListener(e -> {
//			if(edited != null && edited instanceof RollerInterface[] rollers) {
//				try {
//					rollers[0].open();
//				} catch (IOException ex) {
//					LOG.error("rollerButtonUp", ex);
//				}
//				cancelCellEditing();
//			}
//		});
//		rollerButtonStop.addActionListener(e -> {
//			if(edited != null && edited instanceof RollerInterface[] rollers) {
//				try {
//					rollers[0].stop();
//				} catch (IOException ex) {
//					LOG.error("rollerButtonStop", ex);
//				}
//				cancelCellEditing();
//			}
//		});
//		rollerButtonDown.addActionListener(e -> {
//			if(edited != null && edited instanceof RollerInterface[] rollers) {
//				try {
//					rollers[0].close();
//				} catch (IOException ex) {
//					LOG.error("rollerButtonDown", ex);
//				}
//				cancelCellEditing();
//			}
//		});
//		rollerPerc.addChangeListener(e -> {
//			if(edited != null && edited instanceof RollerInterface[] rollers) {
//				if(rollerPerc.getValueIsAdjusting()) {
//					if(rollers[0].isCalibrated()) {
//						rollerLabel.setText(rollers[0].getLabel() + " " + rollerPerc.getValue() + "%");
//					}
//				} else {
//					try {
//						rollers[0].setPosition(rollerPerc.getValue());
//					} catch (IOException ex) {
//						LOG.error("rollerPerc", ex);
//					}
//					cancelCellEditing();
//				}
//			}
//		});
		
		// Thermostat G1 (TRV)
		trvPanel.setBackground(selBackground);
		trvPanel.add(trvProfileLabel, BorderLayout.CENTER);
		trvPanel.add(trvSlider, BorderLayout.SOUTH);
		JPanel trvButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		trvButtonPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		trvButtonPanel.setOpaque(false);
		JButton trvButtonUp = new JButton(DevicesCommandCellRenderer.UP_IMG);
		trvButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton trvButtonDown = new JButton(DevicesCommandCellRenderer.DOWN_IMG);
		trvButtonDown.setBorder(BorderFactory.createEmptyBorder());
		trvButtonPanel.add(trvButtonUp);
		trvButtonPanel.add(trvButtonDown);
		trvPanel.add(trvButtonPanel, BorderLayout.EAST);
		trvProfileLabel.setForeground(selForeground);
		trvSlider.addChangeListener(e -> {
			if(edited != null && edited instanceof ThermostatG1) {
				if(trvSlider.getValueIsAdjusting()) {
					trvProfileLabel.setText(((ThermostatG1)edited).getCurrentProfile() + " " + trvSlider.getValue()/2f + "°C");
				} else {
					try {
						((ThermostatG1)edited).setTargetTemp(trvSlider.getValue()/2f);
					} catch (/*IO*/Exception ex) {
						LOG.error("thermSlider", ex);
					}
					cancelCellEditing();
				}
			}
		});
		trvButtonUp.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatG1) {
				try {
					((ThermostatG1)edited).targetTempUp(0.5f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonUp", ex);
				}
				cancelCellEditing();
			}
		});
		trvButtonDown.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatG1) {
				try {
					((ThermostatG1)edited).targetTempDown(0.5f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonDown", ex);
				}
				cancelCellEditing();
			}
		});
		
		// ThermostatInterface
		thermPanel.setBackground(selBackground);
		thermPanel.add(thermProfileLabel, BorderLayout.CENTER);
		thermPanel.add(thermSlider, BorderLayout.SOUTH);
		JPanel thermButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		thermButtonPanel.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		thermButtonPanel.setOpaque(false);
		JButton thermButtonUp = new JButton(DevicesCommandCellRenderer.UP_IMG);
		thermButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton thermButtonDown = new JButton(DevicesCommandCellRenderer.DOWN_IMG);
		thermButtonDown.setBorder(BorderFactory.createEmptyBorder());
		thermActiveButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS_SMALLER);
		thermButtonPanel.add(thermActiveButton);
		thermButtonPanel.add(thermButtonUp);
		thermButtonPanel.add(thermButtonDown);
		thermPanel.add(thermButtonPanel, BorderLayout.EAST);
		thermProfileLabel.setForeground(selForeground);
		thermSlider.addChangeListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface[] th) {
				if(thermSlider.getValueIsAdjusting()) {
					if(tempUnitCelsius) {
						thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ ((float)thermSlider.getValue()) / th[0].getUnitDivision() + "°C");
					} else {
						thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ (Math.round((((float)thermSlider.getValue()) / th[0].getUnitDivision()) * 18f + 320f) / 10f) + "°F");
//						thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ String.format(Locale.ENGLISH, "%.1f°F", (((float)thermSlider.getValue()) / th[0].getUnitDivision()) * 1.8f + 32f));
					}
				} else {
					if(th[0] instanceof AbstractBluDevice) {
						table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
					try {
						th[0].setTargetTemp(((float)thermSlider.getValue()) / th[0].getUnitDivision());
					} catch (/*IO*/Exception ex) {
						LOG.error("thermSlider", ex);
					}
					table.setCursor(Cursor.getDefaultCursor());
					cancelCellEditing();
				}
			}
		});
		thermActiveButton.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface[] th) {
				if(th[0] instanceof AbstractBluDevice) {
					table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				try {
					th[0].setEnabled(th[0].isEnabled() == false); // toggle
				} catch (/*IO*/Exception ex) {
					LOG.error("thermActiveButton", ex);
				}
				table.setCursor(Cursor.getDefaultCursor());
				cancelCellEditing();
			}
		});
		thermButtonUp.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface[] th && th[0].getTargetTemp() < th[0].getMaxTargetTemp()) {
				if(th[0] instanceof AbstractBluDevice) {
					table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				try {
					th[0].setTargetTemp(Math.round(10 * th[0].getTargetTemp() + 10f / th[0].getUnitDivision()) / 10f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonUp", ex);
				}
				table.setCursor(Cursor.getDefaultCursor());
				cancelCellEditing();
			}
		});
		thermButtonDown.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface[] th && th[0].getTargetTemp() > th[0].getMinTargetTemp()) {
				if(th[0] instanceof AbstractBluDevice) {
					table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				try {
					th[0].setTargetTemp(Math.round(10 * th[0].getTargetTemp() - 10f / th[0].getUnitDivision()) / 10f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonDown", ex);
				}
				table.setCursor(Cursor.getDefaultCursor());
				cancelCellEditing();
			}
		});
		
		BoxLayout stackedPanelLO = new BoxLayout(stackedPanel, BoxLayout.Y_AXIS);
		stackedPanel.setLayout(stackedPanelLO);
		stackedPanel.setBackground(selBackground);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if(value instanceof RelayInterface[] riArray) {
			stackedPanel.removeAll();
			for(RelayInterface rel: riArray) {
				stackedPanel.add(getRelayPanel(rel));
			}
			edited = riArray;
			return stackedPanel;
		} else if(value instanceof RollerInterface[] rollersArray) {
//			return getRollerPanel(rollersArray);
			stackedPanel.removeAll();
			for(RollerInterface rel: rollersArray) {
				stackedPanel.add(getRollerPanel(rel));
			}
			edited = rollersArray;
			return stackedPanel;
		} else if(value instanceof LightBulbRGB[] bulbsArray) { // RGBW Bulbs
			return getLightRGBWPanel(bulbsArray);
		} else if(value instanceof RGBWInterface[] rgbws) {
			return getRGBWColorPanel(rgbws);
		} else if(value instanceof RGBInterface[] rgbs) {
			return getRGBColorPanel(rgbs);
		} else if(value instanceof WhiteInterface[] whitesArray) {
			return getWhitePanel(whitesArray);
		} else if(value instanceof ThermostatG1 th) { // TRV
			return getTrvG1Panel(th);
		} else if(value instanceof ThermostatInterface[] ths) {
			return getThermostatPanel(ths);
		} else if(value instanceof DeviceModule[] modArray) { // mixed
			stackedPanel.removeAll();
			for(int i = 0; i < modArray.length; i++) {
				DeviceModule module = modArray[i];
				if(module instanceof RelayInterface rel) {
					stackedPanel.add(getRelayPanel(rel));
				} else if(module instanceof InputInterface input) {
					if(input.enabled()) {
						stackedPanel.add(getInputPanel(input, table));
					}
				} else if(module instanceof WhiteInterface white) {
					stackedPanel.add(getWhiteSyntheticPanel(white, i == modArray.length - 1));
				} else if(module instanceof RGBInterface rgb) {
					stackedPanel.add(getRGBSyntheticPanel(rgb, i == modArray.length - 1));
				}
			}
			edited = modArray;
			return stackedPanel.getComponentCount() > 0 ? stackedPanel : null;
		}
		return null;
	}
	
	private JPanel getRelayPanel(RelayInterface rel) {
		JLabel relayLabel = new JLabel(rel.getLabel());
		relayLabel.setForeground(selForeground);
		JPanel relayPanel = new JPanel(new BorderLayout());
		JButton relayButton = new JButton();
		relayButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		relayButton.addActionListener(e -> {
			if(edited != null) {
				try {
					rel.toggle();
				} catch (IOException ex) {
					LOG.error("getRelaysPanel {}", rel, ex);
				}
				cancelCellEditing();
			}
		});
		
		JPanel relayButtonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));
		relayButtonPanel.setOpaque(false);
		relayButtonPanel.add(relayButton);

		relayPanel.setOpaque(false);
		relayPanel.add(relayLabel, BorderLayout.CENTER);
		relayPanel.add(relayButtonPanel, BorderLayout.EAST);
		
		if(rel.isOn()) {
			relayButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			relayButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		if(rel.isInputOn()) {
			relayButton.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
		}
		return relayPanel;
	}
	
//	private Component getRollerPanel(RollerInterface[] rollers) {
//		RollerInterface roller = rollers[0]; // multiple rollers devices currently not supported
//		final String labelText;
//		if(roller.isCalibrated()) {
//			labelText = roller.getLabel() + " " + roller.getPosition() + "%";
//			rollerPerc.setVisible(true);
//		} else {
//			labelText = roller.getLabel();
//			rollerPerc.setVisible(false);
//		}
//		rollerPerc.setValue(roller.getPosition());
//		rollerLabel.setText(labelText);
//		edited = rollers;
//		return rollerPanel;
//	}
	
	private Component getRollerPanel(RollerInterface roller) {
		JPanel rollerPanel = new JPanel(new BorderLayout());
		JLabel rollerLabel = new JLabel();
		
		rollerPanel.setBackground(selBackground);
		rollerLabel.setForeground(selForeground);
		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
		rollerSouthPanel.setOpaque(false);
		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		rollerButtonPanel.setOpaque(false);
		rollerSouthPanel.add(rollerButtonPanel, BorderLayout.EAST);
		JButton rollerButtonUp = new JButton(DevicesCommandCellRenderer.UP_IMG);
		JButton rollerButtonDown = new JButton(DevicesCommandCellRenderer.DOWN_IMG);
		JButton rollerButtonStop = new JButton(DevicesCommandCellRenderer.STOP_IMG);
		rollerButtonUp.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonStop.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonDown.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonPanel.add(rollerButtonUp);
		rollerButtonPanel.add(rollerButtonStop);
		rollerButtonPanel.add(rollerButtonDown);
		rollerPanel.add(rollerSouthPanel, BorderLayout.SOUTH);
		rollerButtonUp.addActionListener(e -> {
			if(edited != null && edited instanceof RollerInterface[]) {
				try {
					roller.open();
				} catch (IOException ex) {
					LOG.error("rollerButtonUp", ex);
				}
				cancelCellEditing();
			}
		});
		rollerButtonStop.addActionListener(e -> {
			if(edited != null && edited instanceof RollerInterface[]) {
				try {
					roller.stop();
				} catch (IOException ex) {
					LOG.error("rollerButtonStop", ex);
				}
				cancelCellEditing();
			}
		});
		rollerButtonDown.addActionListener(e -> {
			if(edited != null && edited instanceof RollerInterface[]) {
				try {
					roller.close();
				} catch (IOException ex) {
					LOG.error("rollerButtonDown", ex);
				}
				cancelCellEditing();
			}
		});

		if(roller.isCalibrated()) {
			rollerLabel.setText(roller.getLabel() + " " + roller.getPosition() + "%");
			
			JSlider rollerPerc = new JSlider(0, 100, roller.getPosition());
			rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
			rollerPerc.addChangeListener(e -> {
				if(edited != null && edited instanceof RollerInterface[]) {
					if(rollerPerc.getValueIsAdjusting()) {
						rollerLabel.setText(roller.getLabel() + " " + rollerPerc.getValue() + "%");
					} else {
						try {
							roller.setPosition(rollerPerc.getValue());
						} catch (IOException ex) {
							LOG.error("rollerPerc", ex);
						}
						cancelCellEditing();
					}
				}
			});
		} else {
			rollerLabel.setText(roller.getLabel());
		}
		return rollerPanel;
	}
	
	private Component getLightRGBWPanel(LightBulbRGB[] lights) {
		LightBulbRGB light = lights[0]; // multiple bulbs devices currently not supported
		final int slider = light.isColorMode() ? light.getGain() : light.getBrightness();
		lightRGBBulbLabel.setText(light.getLabel() + " " + slider + "%");
		lightRGBBulbBrightness.setValue(slider);
		if(light.isOn()) {
			lightRGBBulbButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			lightRGBBulbButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			lightRGBBulbButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			lightRGBBulbButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		edited = lights;
		return lightRGBBulbPanel;
	}
	
	private Component getRGBColorPanel(RGBInterface[] colors) {
		RGBInterface rgb = colors[0];
		colorRGBLabel.setText(rgb.getLabel() + " " + rgb.getGain() + "%");
//		lightBrightness.setMinimum(light.getMinBrightness());
//		lightBrightness.setMaximum(light.getMaxBrightness());
		colorRGBBrightness.setValue(rgb.getGain());
		if(rgb.isOn()) {
			colorRGBButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			colorRGBButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			colorRGBButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			colorRGBButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		colorRGBButton.setForeground(rgb.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		edited = colors;
		lightRGBSouthPanel.add(editDialogButton, BorderLayout.EAST);
		return colorRGBPanel;
	}
	
	private Component getRGBWColorPanel(RGBWInterface[] colors) {
		RGBWInterface color = colors[0]; // multiple LightRGBW devices currently not supported
		colorRGBWLabel.setText(color.getLabel());
		colorRGBWGain.setValue(color.getGain());
		colorRGBWWhite.setValue(color.getWhite());
		colorRGBWGainLabel.setText(/*LABELS.getString("labelShortGain") +*/ String.format("%-5s", color.getGain() + "%"));
		colorRGBWhiteLabel.setText(String.format("%-5s", LABELS.getString("labelShortWhite") + color.getWhite()));
		if(color.isOn()) {
			colorRGBWButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			colorRGBWButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			colorRGBWButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			colorRGBWButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		colorRGBWButton.setForeground(color.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		edited = colors;
		return colorRGBWPanel;
	}
	
	private Component getWhitePanel(WhiteInterface[] lights) {
		if(lights.length == 1) {
			WhiteInterface light = lights[0];
			lightLabel.setText(light.getLabel() + " " + light.getBrightness() + "%");
			lightBrightness.setMinimum(light.getMinBrightness());
			lightBrightness.setMaximum(light.getMaxBrightness());
			lightBrightness.setValue(light.getBrightness());
			if(light.isOn()) {
				lightButton.setText(DevicesCommandCellRenderer.LABEL_ON);
				lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
			} else {
				lightButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
				lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
			}
			lightButton.setForeground(light.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
			edited = lights;
			return lightPanel;
		} else {
			stackedPanel.removeAll();
			for(int i = 0; i < lights.length;) {
				JPanel panel = getWhiteSyntheticPanel(lights[i], ++i == lights.length);
				stackedPanel.add(panel);
			}
			edited = lights;
			return stackedPanel;
		}
	}
	
	private JPanel getRGBSyntheticPanel(RGBInterface rgb, boolean addEditButton) {
		JPanel relayPanel = new JPanel(new BorderLayout());
		JLabel label = new JLabel(rgb.getLabel() + " " + rgb.getGain() + "%");
		label.setForeground(selForeground);
		JButton relayButton = new JButton();
		relayButton.addActionListener(e -> {
			if(edited != null) {
				try {
					rgb.toggle();
				} catch (IOException ex) {
					LOG.error("getRGBSyntheticPanel {}", rgb, ex);
				}
				cancelCellEditing();
			}
		});
		relayButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		relayPanel.setOpaque(false);
		relayPanel.add(label, BorderLayout.CENTER);
		relayPanel.add(relayButton, BorderLayout.EAST);

		if(rgb.isOn()) {
			relayButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			relayButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		if(rgb.isInputOn()) {
			relayButton.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
		}
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.removeAll();
			editSwitchPanel.add(relayButton, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			relayPanel.add(editSwitchPanel, BorderLayout.EAST);
		} else {
			relayPanel.add(relayButton, BorderLayout.EAST);
		}
		return relayPanel;
	}
	
	private JPanel getWhiteSyntheticPanel(WhiteInterface light, boolean addEditButton) {
		JLabel label = new JLabel(light.getLabel() + " " + light.getBrightness() + "%");
		label.setForeground(selForeground);
		JPanel relayPanel = new JPanel(new BorderLayout());
		JButton relayButton = new JButton();
		relayButton.addActionListener(e -> {
			if(edited != null) {
				try {
					light.toggle();
				} catch (IOException ex) {
					LOG.error("getWhiteSyntheticPanel {}", light, ex);
				}
				cancelCellEditing();
			}
		});
		relayPanel.setOpaque(false);
		relayButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		relayPanel.add(label, BorderLayout.CENTER);
		relayPanel.add(relayButton, BorderLayout.EAST);

		if(light.isOn()) {
			relayButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			relayButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		if(light.isInputOn()) {
			relayButton.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
		}
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
			editSwitchPanel.removeAll();
			editSwitchPanel.add(relayButton, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			relayPanel.add(editSwitchPanel, BorderLayout.EAST);
		} else {
			relayPanel.add(relayButton, BorderLayout.EAST);
		}
		return relayPanel;
	}
	
	private Component getTrvG1Panel(ThermostatG1 thermostat) {
		trvSlider.setValue((int)(thermostat.getTargetTemp() * 2f));
		trvProfileLabel.setText(thermostat.getCurrentProfile() + " " + thermostat.getTargetTemp() + "°C");
		trvProfileLabel.setEnabled(thermostat.isScheduleActive());

		edited = thermostat;
		return trvPanel;
	}
	
	private Component getThermostatPanel(ThermostatInterface therm[]) {
		ThermostatInterface thermostat = therm[0];
		thermSlider.setMinimum((int)(thermostat.getMinTargetTemp() * thermostat.getUnitDivision()));
		thermSlider.setMaximum((int)(thermostat.getMaxTargetTemp() * thermostat.getUnitDivision()));
		thermSlider.setValue((int)(thermostat.getTargetTemp() * thermostat.getUnitDivision()));
		if(tempUnitCelsius) {
			thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ thermostat.getTargetTemp() + "°C");
		} else {
			thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ (Math.round(thermostat.getTargetTemp() * 18f + 320f) / 10f) + "°F");
//			thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ String.format(Locale.ENGLISH, "%.1f°F", thermostat.getTargetTemp() * 1.8f + 32f));
		}
		if(thermostat.isEnabled()) {
			thermActiveButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			thermActiveButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
			thermProfileLabel.setEnabled(true);
			thermActiveButton.setForeground(thermostat.isRunning() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		} else {
			thermActiveButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			thermActiveButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
			thermProfileLabel.setEnabled(false);
			thermActiveButton.setForeground(null);
		}
		edited = therm;
		return thermPanel;
	}
	
	private Component getInputPanel(final InputInterface inp, JTable table) {
		JPanel actionsPanel = new JPanel(new BorderLayout());
		String label = inp.getLabel();
		JLabel actionsLabel = new JLabel(label == null || label.isEmpty() ? "\u25CB" : label);
		actionsPanel.setBackground(selBackground);
		actionsLabel.setForeground(selForeground);
		JPanel actionsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		int numEvents = inp.getRegisteredEventsCount();
		if(numEvents > 0) {
			for(int i = 0; i < numEvents; i++) {
				boolean enabled = inp.enabled(i);
				if(enabled || numEvents <= DevicesCommandCellRenderer.MAX_ACTIONS_SHOWN) {
					String bLabel;
					try {
						bLabel = LABELS.getString(inp.getEvent(i));
					} catch( MissingResourceException e) {
						bLabel = "x";
					}
					JButton b = new JButton(bLabel);
					if(enabled) {
						final int index = i;
						b.addActionListener(e -> {
							if(edited != null /*&& edited instanceof InputInterface[]*/) {
								table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
								try {
									new Thread(() -> {
										try {
											inp.execute(index);
										} catch (IOException ex) {
											Msg.errorMsg(ex);
										}
									}).start();
									Thread.sleep(200);
								} catch (InterruptedException e1) {}
								table.setCursor(Cursor.getDefaultCursor());
								cancelCellEditing();
							}
						});
					} else {
						b.setEnabled(false);
					}
					b.setBorder(/*bLabel.length() > 1 ?*/ DevicesCommandCellRenderer.BUTTON_BORDERS_SMALLER /*: DevicesCommandCellRenderer.BUTTON_BORDERS_SMALL*/);
					b.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
					actionsButtonsPanel.add(b);
					if(inp.isInputOn()) {
						b.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
					}
				}
			}
		} else {
			if(inp.isInputOn()) {
				actionsLabel.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
			}
		}
		actionsButtonsPanel.setOpaque(false);
		actionsPanel.add(actionsButtonsPanel, BorderLayout.EAST);
		actionsPanel.add(actionsLabel, BorderLayout.WEST);
		actionsPanel.setOpaque(false);
		return actionsPanel;
	}

	@Override
	public Object getCellEditorValue() {
		return edited;
	}

	@Override
	public void cancelCellEditing() {
		super.cancelCellEditing();
		edited = null;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}
	
	public void setTempUnit(boolean celsius) {
		tempUnitCelsius = celsius;
	}
}