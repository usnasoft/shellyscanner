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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import it.usna.shellyscan.view.util.Msg;

public class DevicesCommandCellEditor extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(DevicesCommandCellEditor.class);
	private Object edited;

	// Dimmer
	private JLabel lightLabel = new JLabel();
	private JPanel lightPanel = new JPanel(new BorderLayout());
	private JButton lightButton = new JButton();
	private JSlider lightBrightness = new JSlider();
	
	// RGBW Bulbs
	private JLabel lightRGBLabel = new JLabel();
	private JPanel lightRGBPanel = new JPanel(new BorderLayout());
	private JButton lightRGBButton = new JButton();
	private JSlider lightRGBBrightness = new JSlider(0, 100);
	
	// RGB
	private JPanel colorRGBPanel = new JPanel(new BorderLayout());
	private JLabel colorRGBLabel = new JLabel();
	private JButton colorRGBButton = new JButton();
	private JSlider colorRGBBrightness = new JSlider(0, 100);
	
	// RGBW
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
	
	private final Color selBackground;
	private final Color selForeground;

	public DevicesCommandCellEditor(JTable table) {
		this.selBackground = table.getSelectionBackground();
		this.selForeground = table.getSelectionForeground();
		
		// Dimmer
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
		lightRGBPanel.setBackground(selBackground);
		lightRGBLabel.setForeground(selForeground);
		lightRGBPanel.add(lightRGBLabel, BorderLayout.CENTER);
		lightRGBPanel.add(lightRGBButton, BorderLayout.EAST);
		lightRGBButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		lightRGBButton.addActionListener(e -> {
			if(edited != null && edited instanceof LightBulbRGB[] bulbs) {
				try {
					bulbs[0].toggle();
				} catch (IOException ex) {
					LOG.error("lightRGBButton", ex);
				}
				cancelCellEditing();
			}
		});
		JPanel lightRGBSouthPanel = new JPanel(new BorderLayout());
		JButton lightEditRGBButton = new JButton(new ImageIcon(getClass().getResource("/images/Write16.png")));
		lightRGBPanel.add(lightRGBSouthPanel, BorderLayout.SOUTH);
		lightRGBSouthPanel.setOpaque(false);
		lightRGBSouthPanel.add(lightRGBBrightness, BorderLayout.CENTER);
		lightEditRGBButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		lightEditRGBButton.setContentAreaFilled(false);
		lightEditRGBButton.addActionListener(e -> {
			if(edited != null && edited instanceof LightBulbRGB[] bulbs) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				new DialogEditBulbRGB(win, bulbs[0]);
				cancelCellEditing();
				win.setCursor(Cursor.getDefaultCursor());
			}
		});
		lightRGBSouthPanel.add(lightEditRGBButton, BorderLayout.EAST);
		lightRGBBrightness.addChangeListener(e -> {
			if(edited != null && edited instanceof LightBulbRGB[] bulbs) {
				LightBulbRGB light = bulbs[0];
				if(lightRGBBrightness.getValueIsAdjusting()) {
					lightRGBLabel.setText(light.getLabel() + " " + lightRGBBrightness.getValue() + "%");
				} else {
					try {
						if(light.isColorMode()) {
							light.setGain(lightRGBBrightness.getValue());
						} else {
							light.setBrightness(lightRGBBrightness.getValue());
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
		colorRGBPanel.add(colorRGBBrightness, BorderLayout.SOUTH);
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
					colorRGBWGainLabel.setText(LABELS.getString("labelShortGain") + String.format("%-4s", colorRGBWGain.getValue() + "%"));
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
					colorRGBWhiteLabel.setText(LABELS.getString("labelShortWhite") + colorRGBWWhite.getValue());
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
		stackedRGBWSliders.setOpaque(false);
		stackedRGBWSliders.add(colorRGBWGain);
		stackedRGBWSliders.add(colorRGBWWhite);
		colorRGBWSlidersPanel.add(stackedRGBWSliders);
		JButton editRGBWButton = new JButton(new ImageIcon(getClass().getResource("/images/Write16.png")));
		editRGBWButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3));
		editRGBWButton.setContentAreaFilled(false);
		editRGBWButton.addActionListener(e -> {
			if(edited != null && edited instanceof RGBWInterface[] rgbws) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				new DialogEditRGBW(win, rgbws[0]);
				cancelCellEditing();
				win.setCursor(Cursor.getDefaultCursor());
			}
		});
		colorRGBWSlidersPanel.add(editRGBWButton);
		colorRGBWSlidersPanel.setOpaque(false);
		colorRGBWPanel.add(colorRGBWSlidersPanel, BorderLayout.SOUTH);
		
		// Roller
		rollerPanel.setBackground(selBackground);
		rollerLabel.setForeground(selForeground);
		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
		rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
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
		rollerButtonUp.addActionListener(e -> {
			if(edited != null && edited instanceof RollerInterface[] rollers) {
				try {
					rollers[0].open();
				} catch (IOException ex) {
					LOG.error("rollerButtonUp", ex);
				}
				cancelCellEditing();
			}
		});
		rollerButtonStop.addActionListener(e -> {
			if(edited != null && edited instanceof RollerInterface[] rollers) {
				try {
					rollers[0].stop();
				} catch (IOException ex) {
					LOG.error("rollerButtonStop", ex);
				}
				cancelCellEditing();
			}
		});
		rollerButtonDown.addActionListener(e -> {
			if(edited != null && edited instanceof RollerInterface[] rollers) {
				try {
					rollers[0].close();
				} catch (IOException ex) {
					LOG.error("rollerButtonDown", ex);
				}
				cancelCellEditing();
			}
		});
		rollerPerc.addChangeListener(e -> {
			if(edited != null && edited instanceof RollerInterface[] rollers) {
				if(rollerPerc.getValueIsAdjusting()) {
					if(rollers[0].isCalibrated()) {
						rollerLabel.setText(rollers[0].getLabel() + " " + rollerPerc.getValue() + "%");
					}
				} else {
					try {
						rollers[0].setPosition(rollerPerc.getValue());
					} catch (IOException ex) {
						LOG.error("rollerPerc", ex);
					}
					cancelCellEditing();
				}
			}
		});
		
		// LightWhite[] - rgbw2 white
		editSwitchPanel.setOpaque(false);
		editLightWhiteButton.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		editLightWhiteButton.setContentAreaFilled(false);
		editLightWhiteButton.addChangeListener(e -> {
			if(edited != null && edited instanceof WhiteInterface[]) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				new DialogEditLightsArray(win, (WhiteInterface[])edited);
				cancelCellEditing();
				win.setCursor(Cursor.getDefaultCursor());
			}
		});
		
		// Thermostat G1 (TRV)
		trvPanel.setBackground(selBackground);
		trvPanel.add(trvProfileLabel, BorderLayout.CENTER);
		trvPanel.add(trvSlider, BorderLayout.SOUTH);
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
		JButton thermButtonUp = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16up.png")));
		thermButtonUp.setBorder(BorderFactory.createEmptyBorder());
		JButton thermButtonDown = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16down.png")));
		thermButtonDown.setBorder(BorderFactory.createEmptyBorder());
		thermActiveButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS_SMALLER);
		thermButtonPanel.add(thermActiveButton);
		thermButtonPanel.add(thermButtonUp);
		thermButtonPanel.add(thermButtonDown);
		thermPanel.add(thermButtonPanel, BorderLayout.EAST);
		thermProfileLabel.setForeground(selForeground);
		thermSlider.addChangeListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface th) {
				if(thermSlider.getValueIsAdjusting()) {
					thermProfileLabel.setText(/*th.getCurrentProfile() + " " +*/ thermSlider.getValue() / 2f + "°C");
				} else {
					try {
						th.setTargetTemp(thermSlider.getValue() / 2f);
					} catch (/*IO*/Exception ex) {
						LOG.error("thermSlider", ex);
					}
					cancelCellEditing();
				}
			}
		});
		thermActiveButton.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface th) {
				try {
					th.setEnabled(th.isEnabled() == false); // toggle
				} catch (/*IO*/Exception ex) {
					LOG.error("thermActiveButton", ex);
				}
				cancelCellEditing();
			}
		});
		thermButtonUp.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface th && th.getTargetTemp() < th.getMaxTargetTemp()) {
				try {
					th.setTargetTemp(th.getTargetTemp() + 0.5f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonUp", ex);
				}
				cancelCellEditing();
			}
		});
		thermButtonDown.addActionListener(e -> {
			if(edited != null && edited instanceof ThermostatInterface th && th.getTargetTemp() > th.getMinTargetTemp()) {
				try {
					th.setTargetTemp(th.getTargetTemp() - 0.5f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonDown", ex);
				}
				cancelCellEditing();
			}
		});
		
		// used for Relay[]
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
			return getRollerPanel(rollersArray);
		} else if(value instanceof LightBulbRGB[] bulbsArray) { // RGBW Bulbs
			return getLightRGBWPanel(bulbsArray);
		} else if(value instanceof RGBWInterface[] rgbws) {
			return getRGBWColorPanel(rgbws);
		} else if(value instanceof RGBInterface[] rgbs) {
			return getRGBColorPanel(rgbs);
		} else if(value instanceof WhiteInterface[] whitesArray) {
			return getWhitePanel(whitesArray);
		} else if(value instanceof InputInterface[] inputArray) {
			stackedPanel.removeAll();
			for(InputInterface act: inputArray) {
				if(act.enabled()) {
					Component actionsPanel = getInputPanel(act, table);
					stackedPanel.add(actionsPanel);
				}
			}
			edited = inputArray;
			return stackedPanel;
		} else if(value instanceof ThermostatG1 th) { // TRV
			return getTrvPanel(th);
		} else if(value instanceof ThermostatInterface[] ths) {
			return getThermostatPanel(ths[0]);
		} else if(value instanceof DeviceModule[] modArray) { // mixed
			stackedPanel.removeAll();
			for(DeviceModule module: modArray) {
				if(module instanceof RelayInterface rel) {
					stackedPanel.add(getRelayPanel(rel));
				} else if(module instanceof InputInterface input) {
//					stackedPanel.add(new JLabel(input.getLabel()));
					getInputPanel(input, table);
				}
			}
			edited = modArray;
			return stackedPanel;
		}
		return null;
	}
	
	private JPanel getRelayPanel(RelayInterface rel) {
		JLabel relayLabel = new JLabel(rel.getLabel());
		relayLabel.setForeground(selForeground);
		JPanel relayPanel = new JPanel(new BorderLayout());
		JButton relayButton = new JButton();
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
		
		JPanel relayButtonPanel = new JPanel();
		relayButtonPanel.setLayout(new BoxLayout(relayButtonPanel, BoxLayout.Y_AXIS));
		relayButtonPanel.setOpaque(false);
		relayButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		relayButtonPanel.add(Box.createVerticalGlue());
		relayButtonPanel.add(relayButton);
		relayButtonPanel.add(Box.createVerticalGlue());

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
	
	private Component getRollerPanel(RollerInterface[] rollers) {
		RollerInterface roller = rollers[0]; // multiple rollers devices currently not supported
		final String labelText;
		if(roller.isCalibrated()) {
			labelText = roller.getLabel() + " " + roller.getPosition() + "%";
			rollerPerc.setVisible(true);
		} else {
			labelText = roller.getLabel();
			rollerPerc.setVisible(false);
		}
		rollerPerc.setValue(roller.getPosition());
		rollerLabel.setText(labelText);
		edited = rollers;
		return rollerPanel;
	}
	
	private Component getLightRGBWPanel(LightBulbRGB[] lights) {
		LightBulbRGB light = lights[0]; // multiple bulbs devices currently not supported
		final int slider = light.isColorMode() ? light.getGain() : light.getBrightness();
		lightRGBLabel.setText(light.getLabel() + " " + slider + "%");
		lightRGBBrightness.setValue(slider);
		if(light.isOn()) {
			lightRGBButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			lightRGBButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			lightRGBButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			lightRGBButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		edited = lights;
		return lightRGBPanel;
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
//		lightButton.setForeground(light.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		edited = colors;
		return colorRGBPanel;
	}
	
	private Component getRGBWColorPanel(RGBWInterface[] colors) {
		RGBWInterface color = colors[0]; // multiple LightRGBW devices currently not supported
		colorRGBWLabel.setText(color.getLabel());
		colorRGBWGain.setValue(color.getGain());
		colorRGBWWhite.setValue(color.getWhite());
		colorRGBWGainLabel.setText(LABELS.getString("labelShortGain") + String.format("%-4s", color.getGain() + "%"));
		colorRGBWhiteLabel.setText(LABELS.getString("labelShortWhite") + color.getWhite() /*+ " "*/);
		if(color.isOn()) {
			colorRGBWButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			colorRGBWButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			colorRGBWButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			colorRGBWButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
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
				WhiteInterface light = lights[i];
				JLabel relayLabel = new JLabel(light.getLabel() + " " + light.getBrightness() + "%");
				relayLabel.setForeground(selForeground);
				JPanel relayPanel = new JPanel(new BorderLayout());
				JButton relayButton = new JButton();
				relayButton.addActionListener(e -> {
					if(edited != null) {
						try {
							light.toggle();
						} catch (IOException ex) {
							LOG.error("getRGBWWhitePanel {}", light, ex);
						}
						cancelCellEditing();
					}
				});
				relayPanel.setOpaque(false);
				relayButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
				relayPanel.add(relayLabel, BorderLayout.CENTER);
				relayPanel.add(relayButton, BorderLayout.EAST);

				if(light.isOn()) {
					relayButton.setText(DevicesCommandCellRenderer.LABEL_ON);
					relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
				} else {
					relayButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
					relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
				}
				if(++i < lights.length) {
					relayPanel.add(relayButton, BorderLayout.EAST);
				} else {
					editSwitchPanel.removeAll();
					editSwitchPanel.add(relayButton, BorderLayout.EAST);
					editSwitchPanel.add(BorderLayout.WEST, editLightWhiteButton);
					relayPanel.add(editSwitchPanel, BorderLayout.EAST);
				}
				//			if(light.isInputOn()) {
				//				relayButton.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
				//			}
				stackedPanel.add(relayPanel);
			}
			edited = lights;
			return stackedPanel;
		}
	}
	
	private Component getTrvPanel(ThermostatG1 thermostat) {
		trvSlider.setValue((int)(thermostat.getTargetTemp() * 2f));
		trvProfileLabel.setText(thermostat.getCurrentProfile() + " " + thermostat.getTargetTemp() + "°C");
		trvProfileLabel.setEnabled(thermostat.isScheduleActive());

		edited = thermostat;
		return trvPanel;
	}
	
	private Component getThermostatPanel(ThermostatInterface thermostat) {
		thermSlider.setMinimum((int)(thermostat.getMinTargetTemp() * 2));
		thermSlider.setMaximum((int)(thermostat.getMaxTargetTemp() * 2));
		thermSlider.setValue((int)(thermostat.getTargetTemp() * 2f));
		thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ thermostat.getTargetTemp() + "°C");
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
		edited = thermostat;
		return thermPanel;
	}
	
	private Component getInputPanel(final InputInterface act, JTable table) {
		JPanel actionsPanel = new JPanel(new BorderLayout());
		String label = act.getLabel();
		JLabel actionsLabel = new JLabel(label.isEmpty() ? "-" : label);
		actionsPanel.setBackground(selBackground);
		actionsLabel.setForeground(selForeground);
		actionsPanel.add(actionsLabel, BorderLayout.CENTER);
		JPanel actionsSouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		int numEvents = act.getRegisteredEventsCount();
		if(numEvents > 0) {
			for(String type: act.getRegisteredEvents()) {
				boolean enabled = act.enabled(type);
				if(enabled || numEvents <= DevicesCommandCellRenderer.MAX_ACTIONS_SHOWN) {
					String bLabel;
					try {
						bLabel = LABELS.getString(type);
					} catch( MissingResourceException e) {
						bLabel = "x";
					}
					JButton b = new JButton(bLabel);
					if(enabled) {
						b.addActionListener(e -> {
							if(edited != null && edited instanceof InputInterface[]) {
								table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
								try {
									new Thread(() -> {
										try {
											act.execute(type);
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
					b.setBorder(bLabel.length() > 1 ? DevicesCommandCellRenderer.BUTTON_BORDERS_SMALLER : DevicesCommandCellRenderer.BUTTON_BORDERS_SMALL/*new EmptyBorder(DevicesTableRenderer.BUTTON_MARGIN_V, DevicesTableRenderer.BUTTON_MARGIN_H-2, DevicesTableRenderer.BUTTON_MARGIN_V, DevicesTableRenderer.BUTTON_MARGIN_H-2)*/);
					b.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
					actionsSouthPanel.add(b);
					if(act.isInputOn()) {
						b.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
					}
				}
			}
		} else {
			if(act.isInputOn()) {
				actionsLabel.setForeground(DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR);
			}
		}
		actionsSouthPanel.setOpaque(false);
		actionsPanel.add(actionsSouthPanel, BorderLayout.SOUTH);
		actionsPanel.setOpaque(false);
		stackedPanel.add(actionsPanel);
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
}