package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.device.blu.AbstractBTHomeDevice;
import it.usna.shellyscan.model.device.g1.modules.ThermostatG1;
import it.usna.shellyscan.model.device.modules.CBreakerInterface;
import it.usna.shellyscan.model.device.modules.CCTInterface;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.InputInterface;
import it.usna.shellyscan.model.device.modules.RGBCCTInterface;
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.RGBWInterface;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.shellyscan.model.device.modules.RollerInterface;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;
import it.usna.shellyscan.model.device.modules.WhiteInterface;
import it.usna.shellyscan.view.lightsEditor.DialogEditLights;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.VerticalFlowLayout;

public class DevicesCommandCellEditor extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(DevicesCommandCellEditor.class);
	private Object edited;
	
	// Generic
	private JButton editDialogButton = new JButton(DevicesCommandCellRenderer.EDIT_IMG);
	private JPanel stackedPanelContainer = new JPanel(new BorderLayout(0, 0));
	private JPanel stackedPanel = new JPanel();
	
	private final Color selBackground;
	private final Color selForeground;
	
	private boolean tempUnitCelsius;

	public DevicesCommandCellEditor(JTable table, boolean celsius) {
		this.tempUnitCelsius = celsius;
		this.selBackground = table.getSelectionBackground();
		this.selForeground = table.getSelectionForeground();
		
		// Generic
		editDialogButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		editDialogButton.setContentAreaFilled(false);
		editDialogButton.addActionListener(e -> {
			if(edited instanceof DeviceModule[] modules) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				win.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				new DialogEditLights(win, modules);
				win.setCursor(Cursor.getDefaultCursor());
			}
			cancelCellEditing();
		});

		BoxLayout stackedPanelLO = new BoxLayout(stackedPanel, BoxLayout.Y_AXIS);
		stackedPanel.setLayout(stackedPanelLO);
		stackedPanel.setOpaque(false);
		stackedPanelContainer.add(stackedPanel, BorderLayout.NORTH);
		stackedPanelContainer.setBackground(selBackground);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if(value instanceof RelayInterface[] riArray) {
			stackedPanel.removeAll();
			for(RelayInterface rel: riArray) {
				stackedPanel.add(getRelayPanel(rel));
			}
			edited = riArray;
			return stackedPanelContainer;
		} else if(value instanceof RollerInterface[] rollersArray) {
			stackedPanel.removeAll();
			for(RollerInterface rol: rollersArray) {
				stackedPanel.add(getRollerPanel(rol));
			}
			edited = rollersArray;
			return stackedPanelContainer;
		} else if(value instanceof RGBCCTInterface[] bulbsArray) { // RGB/CCT Bulbs
			JPanel panel = getRGBCCTPanel(bulbsArray[0], true);
			edited = bulbsArray;
			return panel;
		} else if(value instanceof RGBWInterface[] rgbws) {
			JPanel panel = getRGBWPanel(rgbws[0], true);
			edited = rgbws;
			return panel;
		} else if(value instanceof RGBInterface[] rgbs) {
			JPanel panel = getRGBPanel(rgbs[0], true);
			edited = rgbs;
			return panel;
		} else if(value instanceof ThermostatG1 th) { // TRV Gen1
			JPanel panel = getThermostatG1Panel(th);
			edited = th;
			return panel;
		} else if(value instanceof ThermostatInterface[] ths) {
			JPanel panel = getThermostatPanel(ths[0], table);
			edited = ths;
			return panel;
		} else if(value instanceof CBreakerInterface[] cbs) {
			JPanel panel = getCBSwitchPanel(cbs[0], table);
			edited = cbs;
			return panel;
		} else if(value instanceof DeviceModule[] modArray) { // mixed
			stackedPanel.removeAll();
			
			DeviceModule module;
			int indEditButton = modArray.length - 1;
			while(indEditButton >= 0 && ! ((module = modArray[indEditButton]) instanceof CCTInterface || module instanceof RGBInterface || (module instanceof WhiteInterface && modArray.length > 2))) {
				indEditButton--;
			}
			
			for(int i = 0; i < modArray.length; i++) {
				module = modArray[i];
				if(module instanceof RelayInterface rel) {
					stackedPanel.add(getRelayPanel(rel));
				} else if(module instanceof InputInterface input) {
					if(input.enabled()) {
						stackedPanel.add(getInputPanel(input, table));
					}
				} else if(module instanceof RollerInterface rol) {
					stackedPanel.add(getRollerPanel(rol));
				} else if(module instanceof WhiteInterface white && modArray.length <= 2) {
					stackedPanel.add(getWhitePanel(white, i == indEditButton));
				} else if(module instanceof WhiteInterface white /*&& modArray.length > 2*/) {
					stackedPanel.add(getWhiteSyntheticPanel(white, i == indEditButton));
				} else if(module instanceof RGBInterface rgb) {
					stackedPanel.add(getRGBSyntheticPanel(rgb, i == indEditButton));
				}
			}
			edited = modArray;
			return stackedPanel.getComponentCount() > 0 ? stackedPanelContainer : null;
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
//			if(edited != null) {
				try {
					rel.toggle();
				} catch (IOException ex) {
					LOG.error("getRelaysPanel {}", rel, ex);
				}
				cancelCellEditing();
//			}
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
	
	private JPanel getCBSwitchPanel(CBreakerInterface sw, JTable table) {
		if(sw.isLocked() == false) {
			JLabel relayLabel = new JLabel(sw.getLabel());
			relayLabel.setForeground(selForeground);
			JPanel relayPanel = new JPanel(new BorderLayout());
			relayPanel.setBackground(selBackground);
			JButton relayButton = new JButton();
			relayButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
			relayButton.addActionListener(e -> {
				table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					final String cancel = UIManager.getString("OptionPane.cancelButtonText");
					if(JOptionPane.showOptionDialog(
							table, LABELS.getString("action_CBSwitch_confirm"), LABELS.getString("action_toggle"),
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
							new Object[] {LABELS.getString("action_toggle"), cancel}, cancel) == 0) {
					sw.toggle();
					}
				} catch (Exception ex) {
					LOG.error("getCBSwitchPanel {}", sw, ex);
				}
				table.setCursor(Cursor.getDefaultCursor());
				cancelCellEditing();
			});

			JPanel relayButtonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));
			relayButtonPanel.setOpaque(false);
			relayButtonPanel.add(relayButton);

			relayPanel.add(relayLabel, BorderLayout.CENTER);
			relayPanel.add(relayButtonPanel, BorderLayout.EAST);

			if(sw.isOn()) {
				relayButton.setText(DevicesCommandCellRenderer.LABEL_ON);
				relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
			} else {
				relayButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
				relayButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
			}
			return relayPanel;
		} else {
			return null;
		}
	}
	
	private Component getRollerPanel(RollerInterface roller) {
		JPanel rollerPanel = new JPanel(new BorderLayout());
		rollerPanel.setOpaque(false);
		JLabel rollerLabel = new JLabel();
		
//		rollerPanel.setBackground(selBackground);
		rollerLabel.setForeground(selForeground);
		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
		rollerSouthPanel.setOpaque(false);
		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		rollerButtonPanel.setOpaque(false);
		rollerSouthPanel.add(rollerButtonPanel, BorderLayout.EAST);
		JButton rollerButtonUp = new JButton(roller.isInputOn0() ? DevicesCommandCellRenderer.UP_ON_IMG : DevicesCommandCellRenderer.UP_IMG);
		JButton rollerButtonDown = new JButton(roller.isInputOn1() ? DevicesCommandCellRenderer.DOWN_ON_IMG : DevicesCommandCellRenderer.DOWN_IMG);
		JButton rollerButtonStop = new JButton(DevicesCommandCellRenderer.STOP_IMG);
		rollerButtonUp.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonStop.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonDown.setBorder(BorderFactory.createEmptyBorder());
		rollerButtonPanel.add(rollerButtonUp);
		rollerButtonPanel.add(rollerButtonStop);
		rollerButtonPanel.add(rollerButtonDown);
		rollerPanel.add(rollerSouthPanel, BorderLayout.SOUTH);
		rollerButtonUp.addActionListener(e -> {
//			if(edited instanceof RollerInterface[]) {
				try {
					roller.open();
				} catch (IOException ex) {
					LOG.error("rollerButtonUp", ex);
				}
				cancelCellEditing();
//			}
		});
		rollerButtonStop.addActionListener(e -> {
//			if(edited instanceof RollerInterface[]) {
				try {
					roller.stop();
				} catch (IOException ex) {
					LOG.error("rollerButtonStop", ex);
				}
				cancelCellEditing();
//			}
		});
		rollerButtonDown.addActionListener(e -> {
//			if(edited instanceof RollerInterface[]) {
				try {
					roller.close();
				} catch (IOException ex) {
					LOG.error("rollerButtonDown", ex);
				}
				cancelCellEditing();
//			}
		});

		if(roller.isCalibrated()) {
			rollerLabel.setText(roller.getLabel() + " " + roller.getPosition() + "%");
			
			JSlider rollerPerc = new JSlider(0, 100, roller.getPosition());
			rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
			rollerPerc.addChangeListener(e -> {
//				if(edited instanceof RollerInterface[]) {
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
//				}
			});
		} else {
			rollerLabel.setText(roller.getLabel());
		}
		return rollerPanel;
	}
	
	private JPanel getRGBCCTPanel(RGBCCTInterface light, boolean addEditButton) {
		JLabel lightLabel = new JLabel();
		JPanel lightPanel = new JPanel(new BorderLayout());
		JButton lightButton = new JButton();
		JSlider lightBrightness = new JSlider();
		final int sliderValue = light.isColorMode() ? light.getGain() : light.getBrightness();
		
		lightPanel.setBackground(selBackground);
		lightLabel.setForeground(selForeground);
		lightPanel.add(lightLabel, BorderLayout.WEST);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		lightButton.addActionListener(e -> {
//			if(edited instanceof WhiteInterface[]) {
				try {
					light.toggle();
				} catch (IOException ex) {
					LOG.error("lightButton", ex);
				}
				cancelCellEditing();
//			}
		});
		lightPanel.add(lightBrightness, BorderLayout.SOUTH);
		lightBrightness.addChangeListener(e -> {
//			if(edited instanceof WhiteInterface[]) {
				if(lightBrightness.getValueIsAdjusting()) {
					lightLabel.setText(light.getLabel() + " " + lightBrightness.getValue() + "%");
				} else {
					try {
						if(light.isColorMode()) {
							light.setGain(lightBrightness.getValue());
						} else {
							light.setBrightness(lightBrightness.getValue());
						}
					} catch (IOException ex) {
						LOG.error("lightBrightness", ex);
					}
					cancelCellEditing();
				}
//			}
		});
		lightLabel.setText(light.getLabel() + " " + sliderValue + "%");
		lightBrightness.setMinimum(light.getMinBrightness());
		lightBrightness.setMaximum(light.getMaxBrightness());
		lightBrightness.setValue(sliderValue);
		if(light.isOn()) {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		lightButton.setForeground(light.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
//			editSwitchPanel.removeAll();
			editSwitchPanel.add(lightButton, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			lightPanel.add(editSwitchPanel, BorderLayout.EAST);
			lightPanel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			lightPanel.add(lightButton, BorderLayout.EAST);
		}
		return lightPanel;
	}
	
	private JPanel getRGBPanel(RGBInterface rgb, boolean addEditButton) {
		JLabel lightLabel = new JLabel();
		JPanel lightPanel = new JPanel(new BorderLayout());
		JButton lightButton = new JButton();
		JSlider lightBrightness = new JSlider(0/*light.getMinBrightness()*/, 100/*light.getMaxBrightness()*/, rgb.getGain());
		
		lightPanel.setBackground(selBackground);
		lightLabel.setForeground(selForeground);
		lightPanel.add(lightLabel, BorderLayout.WEST);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		lightButton.addActionListener(e -> {
//			if(edited instanceof RGBInterface[]) {
				try {
					rgb.toggle();
				} catch (IOException ex) {
					LOG.error("lightButton", ex);
				}
				cancelCellEditing();
//			}
		});
		lightPanel.add(lightBrightness, BorderLayout.SOUTH);
		lightBrightness.addChangeListener(e -> {
//			if(edited instanceof RGBInterface[]) {
				if(lightBrightness.getValueIsAdjusting()) {
					lightLabel.setText(rgb.getLabel() + " " + lightBrightness.getValue() + "%");
				} else {
					try {
						rgb.setGain(lightBrightness.getValue());
					} catch (IOException ex) {
						LOG.error("lightBrightness", ex);
					}
					cancelCellEditing();
				}
//			}
		});
		lightLabel.setText(rgb.getLabel() + " " + rgb.getGain() + "%");
		if(rgb.isOn()) {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		lightButton.setForeground(rgb.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
//			editSwitchPanel.removeAll();
			editSwitchPanel.add(lightButton, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			lightPanel.add(editSwitchPanel, BorderLayout.EAST);
			lightPanel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			lightPanel.add(lightButton, BorderLayout.EAST);
		}
		return lightPanel;
	}
	
	private JPanel getRGBWPanel(RGBWInterface rgbw, boolean addEditButton) {
		JLabel lightLabel = new JLabel();
		JPanel lightPanel = new JPanel(new BorderLayout(0, 0));
		JButton lightButton = new JButton();

		lightPanel.setBackground(selBackground);
		lightLabel.setForeground(selForeground);
		lightPanel.add(lightLabel, BorderLayout.WEST);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		lightButton.addActionListener(e -> {
//			if(edited instanceof RGBInterface[]) {
				try {
					rgbw.toggle();
				} catch (IOException ex) {
					LOG.error("lightButton", ex);
				}
				cancelCellEditing();
//			}
		});
		
		JSlider lightBrightness = new JSlider(0/*light.getMinBrightness()*/, 100/*light.getMaxBrightness()*/, rgbw.getGain());
		lightBrightness.addChangeListener(e -> {
//			if(edited instanceof RGBInterface[]) {
				if(lightBrightness.getValueIsAdjusting()) {
					lightLabel.setText(rgbw.getLabel() + " " + lightBrightness.getValue() + "%");
				} else {
					try {
						rgbw.setGain(lightBrightness.getValue());
					} catch (IOException ex) {
						LOG.error("lightBrightness", ex);
					}
					cancelCellEditing();
				}
//			}
		});
		JSlider lightWhite = new JSlider(0, 255, rgbw.getWhite());
		lightWhite.addChangeListener(e -> {
//			if(edited instanceof RGBInterface[]) {
				if(lightWhite.getValueIsAdjusting() == false) {
					try {
						rgbw.setWhite(lightWhite.getValue());
					} catch (IOException ex) {
						LOG.error("lightWhite", ex);
					}
					cancelCellEditing();
				}
//			}
		});
		final JPanel sliderPanel = new JPanel(new BorderLayout(0, 0));
		sliderPanel.setOpaque(false);
		sliderPanel.add(lightBrightness, BorderLayout.NORTH);
		sliderPanel.add(lightWhite, BorderLayout.SOUTH);
		lightPanel.add(sliderPanel, BorderLayout.SOUTH);

		lightLabel.setText(rgbw.getLabel() + " " + rgbw.getGain() + "%");
		if(rgbw.isOn()) {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		lightButton.setForeground(rgbw.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
//			editSwitchPanel.removeAll();
			editSwitchPanel.add(lightButton, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			lightPanel.add(editSwitchPanel, BorderLayout.EAST);
			lightPanel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			lightPanel.add(lightButton, BorderLayout.EAST);
		}
		return lightPanel;
	}

	private JPanel getRGBSyntheticPanel(RGBInterface rgb, boolean addEditButton) {
		JPanel relayPanel = new JPanel(new BorderLayout());
		JLabel label = new JLabel(rgb.getLabel() + " " + rgb.getGain() + "%");
		label.setForeground(selForeground);
		JButton relayButton = new JButton();
		relayButton.addActionListener(e -> {
//			if(edited != null) {
				try {
					rgb.toggle();
				} catch (IOException ex) {
					LOG.error("getRGBSyntheticPanel {}", rgb, ex);
				}
				cancelCellEditing();
//			}
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
//			editSwitchPanel.removeAll();
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
		JPanel panel = new JPanel(new BorderLayout());
		JButton relayButton = new JButton();
		relayButton.addActionListener(e -> {
//			if(edited != null) {
				try {
					light.toggle();
				} catch (IOException ex) {
					LOG.error("getWhiteSyntheticPanel {}", light, ex);
				}
				cancelCellEditing();
//			}
		});
		panel.setOpaque(false);
		relayButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		panel.add(label, BorderLayout.CENTER);
		panel.add(relayButton, BorderLayout.EAST);

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
//			editSwitchPanel.removeAll();
			editSwitchPanel.add(relayButton, BorderLayout.EAST);
			editSwitchPanel.add(BorderLayout.WEST, editDialogButton);
			panel.add(editSwitchPanel, BorderLayout.EAST);
		} else {
			panel.add(relayButton, BorderLayout.EAST);
		}
		return panel;
	}
	
	private JPanel getWhitePanel(WhiteInterface light, boolean addEditButton) {
		JLabel lightLabel = new JLabel();
		JPanel lightPanel = new JPanel(new BorderLayout());
		lightPanel.setOpaque(false);
		JButton lightButton = new JButton();
		JSlider lightBrightness = new JSlider(light.getMinBrightness(), light.getMaxBrightness(), light.getBrightness());
		
//		lightPanel.setBackground(selBackground);
		lightLabel.setForeground(selForeground);
		lightPanel.add(lightLabel, BorderLayout.WEST);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightButton.setBorder(DevicesCommandCellRenderer.BUTTON_BORDERS);
		lightButton.addActionListener(e -> {
//			if(edited instanceof WhiteInterface[]) {
				try {
					light.toggle();
				} catch (IOException ex) {
					LOG.error("lightButton", ex);
				}
				cancelCellEditing();
//			}
		});
		lightPanel.add(lightBrightness, BorderLayout.SOUTH);
		lightBrightness.addChangeListener(e -> {
//			if(edited instanceof WhiteInterface[]) {
				if(lightBrightness.getValueIsAdjusting()) {
					lightLabel.setText(light.getLabel() + " " + lightBrightness.getValue() + "%");
				} else {
					try {
						light.setBrightness(lightBrightness.getValue());
					} catch (IOException ex) {
						LOG.error("lightBrightness", ex);
					}
					cancelCellEditing();
				}
//			}
		});
		lightLabel.setText(light.getLabel() + " " + light.getBrightness() + "%");
		if(light.isOn()) {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
		} else {
			lightButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			lightButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
		}
		lightButton.setForeground(light.isInputOn() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		if(addEditButton) {
			JPanel editSwitchPanel = new JPanel(new BorderLayout());
			editSwitchPanel.setOpaque(false);
//			editSwitchPanel.removeAll();
			editSwitchPanel.add(lightButton, BorderLayout.EAST);
			editSwitchPanel.add(editDialogButton, BorderLayout.WEST);
			lightPanel.add(editSwitchPanel, BorderLayout.EAST);
			lightPanel.setComponentZOrder(editSwitchPanel, 0);
		} else {
			lightPanel.add(lightButton, BorderLayout.EAST);
		}
		return lightPanel;
	}

	private JPanel getThermostatPanel(ThermostatInterface therm, JTable table) {
		JPanel thermPanel = new JPanel(new BorderLayout());
		JLabel thermProfileLabel = new JLabel();
		JSlider thermSlider = new JSlider(
				(int)(therm.getMinTargetTemp() * therm.getUnitDivision()),
				(int)(therm.getMaxTargetTemp() * therm.getUnitDivision()),
				(int)(therm.getTargetTemp() * therm.getUnitDivision()));

		JButton thermActiveButton = new JButton();
		
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
//			if(edited instanceof ThermostatInterface[] th) {
				if(thermSlider.getValueIsAdjusting()) {
					if(tempUnitCelsius) {
						thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ ((float)thermSlider.getValue()) / therm.getUnitDivision() + "°C");
					} else {
						thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ (Math.round((((float)thermSlider.getValue()) / therm.getUnitDivision()) * 18f + 320f) / 10f) + "°F");
					}
				} else {
					if(therm instanceof AbstractBTHomeDevice) {
						table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
					try {
						therm.setTargetTemp(((float)thermSlider.getValue()) / therm.getUnitDivision());
					} catch (/*IO*/Exception ex) {
						LOG.error("thermSlider", ex);
					}
					table.setCursor(Cursor.getDefaultCursor());
					cancelCellEditing();
				}
//			}
		});
		thermActiveButton.addActionListener(e -> {
//			if(edited instanceof ThermostatInterface[] th) {
				if(therm instanceof AbstractBTHomeDevice) {
					table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				try {
					therm.setEnabled(therm.isEnabled() == false); // toggle
				} catch (/*IO*/Exception ex) {
					LOG.error("thermActiveButton", ex);
				}
				table.setCursor(Cursor.getDefaultCursor());
				cancelCellEditing();
//			}
		});
		thermButtonUp.addActionListener(e -> {
			if(/*edited instanceof ThermostatInterface[] th &&*/ therm.getTargetTemp() < therm.getMaxTargetTemp()) {
				if(therm instanceof AbstractBTHomeDevice) {
					table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				try {
					therm.setTargetTemp(Math.round(10 * therm.getTargetTemp() + 10f / therm.getUnitDivision()) / 10f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonUp", ex);
				}
				table.setCursor(Cursor.getDefaultCursor());
				cancelCellEditing();
			}
		});
		thermButtonDown.addActionListener(e -> {
			if(/*edited instanceof ThermostatInterface[] th &&*/ therm.getTargetTemp() > therm.getMinTargetTemp()) {
				if(therm instanceof AbstractBTHomeDevice) {
					table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				try {
					therm.setTargetTemp(Math.round(10 * therm.getTargetTemp() - 10f / therm.getUnitDivision()) / 10f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonDown", ex);
				}
				table.setCursor(Cursor.getDefaultCursor());
				cancelCellEditing();
			}
		});

		if(tempUnitCelsius) {
			thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ therm.getTargetTemp() + "°C");
		} else {
			thermProfileLabel.setText(/*thermostat.getCurrentProfile() + " " +*/ (Math.round(therm.getTargetTemp() * 18f + 320f) / 10f) + "°F");
		}
		if(therm.isEnabled()) {
			thermActiveButton.setText(DevicesCommandCellRenderer.LABEL_ON);
			thermActiveButton.setBackground(DevicesCommandCellRenderer.BUTTON_ON_BG_COLOR);
			thermProfileLabel.setEnabled(true);
			thermActiveButton.setForeground(therm.isRunning() ? DevicesCommandCellRenderer.BUTTON_ON_FG_COLOR : null);
		} else {
			thermActiveButton.setText(DevicesCommandCellRenderer.LABEL_OFF);
			thermActiveButton.setBackground(DevicesCommandCellRenderer.BUTTON_OFF_BG_COLOR);
			thermProfileLabel.setEnabled(false);
			thermActiveButton.setForeground(null);
		}
		return thermPanel;
	}
	
	private JPanel getThermostatG1Panel(ThermostatG1 thermostat) { // TRV Gen1
		JPanel trvPanel = new JPanel(new BorderLayout());
		JLabel trvProfileLabel = new JLabel();
		JSlider trvSlider = new JSlider((int)(ThermostatG1.TARGET_MIN * 2), (int)(ThermostatG1.TARGET_MAX * 2), (int)(thermostat.getTargetTemp() * 2f));
		
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
//			if(edited instanceof ThermostatG1) {
				if(trvSlider.getValueIsAdjusting()) {
					trvProfileLabel.setText(thermostat.getCurrentProfile() + " " + trvSlider.getValue()/2f + "°C");
				} else {
					try {
						thermostat.setTargetTemp(trvSlider.getValue()/2f);
					} catch (/*IO*/Exception ex) {
						LOG.error("thermSlider", ex);
					}
					cancelCellEditing();
				}
//			}
		});
		trvButtonUp.addActionListener(e -> {
//			if(edited instanceof ThermostatG1) {
				try {
					thermostat.targetTempUp(0.5f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonUp", ex);
				}
				cancelCellEditing();
//			}
		});
		trvButtonDown.addActionListener(e -> {
//			if(edited instanceof ThermostatG1) {
				try {
					thermostat.targetTempDown(0.5f);
				} catch (/*IO*/Exception ex) {
					LOG.error("thermButtonDown", ex);
				}
				cancelCellEditing();
//			}
		});

		trvProfileLabel.setText(thermostat.getCurrentProfile() + " " + thermostat.getTargetTemp() + "°C");
		trvProfileLabel.setEnabled(thermostat.isScheduleActive());
		return trvPanel;
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
//							if(edited != null /*&& edited instanceof InputInterface[]*/) {
								table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
								try {
									new Thread(() -> {
										try {
											inp.execute(index);
										} catch (IOException ex) {
											Msg.errorMsg(null, ex);
										}
									}).start();
									Thread.sleep(200);
								} catch (InterruptedException e1) {}
								table.setCursor(Cursor.getDefaultCursor());
								cancelCellEditing();
//							}
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