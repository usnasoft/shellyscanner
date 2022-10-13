package it.usna.shellyscan.view;

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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.g1.modules.Actions;
import it.usna.shellyscan.model.device.g1.modules.LightBulbRGB;
import it.usna.shellyscan.model.device.g1.modules.LightRGBW;
import it.usna.shellyscan.model.device.g1.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.RollerInterface;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class DeviceTableCellEditor extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 1L;
	private Object edited;

	private JLabel lightLabel = new JLabel();
	private JPanel lightPanel = new JPanel(new BorderLayout());
	private JButton lightButton = new JButton();
	private JSlider lightBrightness = new JSlider(1, 100);
	
	private JLabel lightRGBLabel = new JLabel();
	private JPanel lightRGBPanel = new JPanel(new BorderLayout());
	private JButton lightRGBButton = new JButton();
	private JSlider lightRGBBrightness = new JSlider(0, 100);
	
	// RGBW
	private JPanel colorRGBPanel = new JPanel(new BorderLayout());
	private JLabel colorRGBLabel = new JLabel();
	private JButton colorRGBButton = new JButton();
	private JSlider colorRGBGain = new JSlider(0, 100);
	private JSlider colorRGBBWhite = new JSlider(0, 100);
	private JLabel colorRGBGainLabel = new JLabel();
	private JLabel colorRGBWhiteLabel = new JLabel();
	
	private JPanel rollerPanel = new JPanel(new BorderLayout());
	private JLabel rollerLabel = new JLabel();
	private JSlider rollerPerc = new JSlider(0, 100);
	
	private JPanel stackedPanel = new JPanel();

	public DeviceTableCellEditor(JTable table) {
		// Dimmer
		lightPanel.setBackground(table.getSelectionBackground());
		lightLabel.setForeground(table.getSelectionForeground());
		lightPanel.add(lightLabel, BorderLayout.CENTER);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightButton.setBorder(DevicesTableRenderer.BUTTON_BORDERS);
		lightButton.addActionListener(e -> {
			try {
				if(edited != null && edited instanceof LightWhite) {
					((LightWhite)edited).toggle();
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		lightPanel.add(lightBrightness, BorderLayout.SOUTH);
		lightBrightness.addChangeListener(e -> {
			try {
				if(edited != null && edited instanceof LightWhite && lightBrightness.getValueIsAdjusting() == false) {
					((LightWhite)edited).setBrightness(lightBrightness.getValue());
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		
		// RGBW Bulbs
		lightRGBPanel.setBackground(table.getSelectionBackground());
		lightRGBLabel.setForeground(table.getSelectionForeground());
		lightRGBPanel.add(lightRGBLabel, BorderLayout.CENTER);
		lightRGBPanel.add(lightRGBButton, BorderLayout.EAST);
		lightRGBButton.setBorder(DevicesTableRenderer.BUTTON_BORDERS);
		lightRGBButton.addActionListener(e -> {
			try {
				if(edited != null && edited instanceof LightBulbRGB) {
					((LightBulbRGB)edited).toggle();
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		JPanel lightRGBSouthPanel = new JPanel(new BorderLayout());
		JButton lightEditRGBButton = new JButton(new ImageIcon(getClass().getResource("/images/Write16.png")));
		lightRGBPanel.add(lightRGBSouthPanel, BorderLayout.SOUTH);
		lightRGBSouthPanel.setOpaque(false);
		lightRGBSouthPanel.add(lightRGBBrightness, BorderLayout.CENTER);
		lightEditRGBButton.setBorder(new EmptyBorder(0, 3, 0, 3));
		lightEditRGBButton.setContentAreaFilled(false);
		lightEditRGBButton.addActionListener(e -> {
			if(edited != null && edited instanceof LightBulbRGB) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				new DialogEditBulbRGB(win, (LightBulbRGB)edited);
				cancelCellEditing();
			}
		});
		lightRGBSouthPanel.add(lightEditRGBButton, BorderLayout.EAST);
		lightRGBBrightness.addChangeListener(e -> {
			try {
				if(edited != null && edited instanceof LightBulbRGB && lightRGBBrightness.getValueIsAdjusting() == false) {
					if(((LightBulbRGB)edited).isColorMode()) {
						((LightBulbRGB)edited).setGain(lightRGBBrightness.getValue());
					} else {
						((LightBulbRGB)edited).setBrightness(lightRGBBrightness.getValue());
					}
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		
		// RGBW (color)
		colorRGBPanel.setBackground(table.getSelectionBackground());
		colorRGBLabel.setForeground(table.getSelectionForeground());
		colorRGBPanel.add(colorRGBLabel, BorderLayout.CENTER);
		colorRGBPanel.add(colorRGBButton, BorderLayout.EAST);
		colorRGBButton.setBorder(DevicesTableRenderer.BUTTON_BORDERS);
		colorRGBButton.addActionListener(e -> {
			try {
				if(edited != null && edited instanceof LightRGBW) {
					((LightRGBW)edited).toggle();
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		colorRGBGain.addChangeListener(e -> {
			try {
				if(edited != null && edited instanceof LightRGBW && colorRGBGain.getValueIsAdjusting() == false) {
					((LightRGBW)edited).setGain(colorRGBGain.getValue());
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		colorRGBBWhite.addChangeListener(e -> {
			try {
				if(edited != null && edited instanceof LightRGBW && colorRGBBWhite.getValueIsAdjusting() == false) {
					((LightRGBW)edited).setWhite(colorRGBBWhite.getValue());
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		JPanel colorRGBSlidersPanel = new JPanel();
		BoxLayout colorRGBSlidersPanelLO = new BoxLayout(colorRGBSlidersPanel, BoxLayout.X_AXIS);
		colorRGBSlidersPanel.setLayout(colorRGBSlidersPanelLO);
		JPanel stackedLabels = new JPanel(new GridLayout(2, 1));
		stackedLabels.setOpaque(false);
		colorRGBGainLabel.setForeground(table.getSelectionForeground());
		colorRGBWhiteLabel.setForeground(table.getSelectionForeground());
		stackedLabels.add(colorRGBGainLabel);
		stackedLabels.add(colorRGBWhiteLabel);
		colorRGBSlidersPanel.add(stackedLabels);
		JPanel stackedSliders = new JPanel(new GridLayout(2, 1));
		stackedSliders.setOpaque(false);
		stackedSliders.add(colorRGBGain);
		stackedSliders.add(colorRGBBWhite);
		colorRGBSlidersPanel.add(stackedSliders);
		JButton editRGBButton = new JButton(new ImageIcon(getClass().getResource("/images/Write16.png")));
		editRGBButton.setBorder(new EmptyBorder(0, 3, 0, 3));
		editRGBButton.setContentAreaFilled(false);
		editRGBButton.addActionListener(e -> {
			if(edited != null && edited instanceof LightRGBW) {
				final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
				new DialogEditRGB(win, (LightRGBW)edited);
				cancelCellEditing();
			}
		});
		colorRGBSlidersPanel.add(editRGBButton);
		colorRGBSlidersPanel.setOpaque(false);
		colorRGBPanel.add(colorRGBSlidersPanel, BorderLayout.SOUTH);
		
		// Roller
		rollerPanel.setBackground(table.getSelectionBackground());
		rollerLabel.setForeground(table.getSelectionForeground());
		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
		rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		rollerSouthPanel.add(rollerButtonPanel, BorderLayout.EAST);
		JButton rollerButtonUp = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16up.png")));
		JButton rollerButtonDown = new JButton(new ImageIcon(getClass().getResource("/images/Arrow16down.png")));
		JButton rollerButtonStop = new JButton(new ImageIcon(getClass().getResource("/images/PlayerStop.png")));
		rollerButtonUp.setBorder(new EmptyBorder(0, 0, 0, 0));
		rollerButtonStop.setBorder(new EmptyBorder(0, 0, 0, 0));
		rollerButtonDown.setBorder(new EmptyBorder(0, 0, 0, 0));
		rollerButtonPanel.add(rollerButtonUp);
		rollerButtonPanel.add(rollerButtonStop);
		rollerButtonPanel.add(rollerButtonDown);
		rollerSouthPanel.setOpaque(false);
		rollerButtonPanel.setOpaque(false);
		rollerPanel.add(rollerSouthPanel, BorderLayout.SOUTH);
		rollerButtonUp.addActionListener(e -> {
			try {
				if(edited != null && edited instanceof Roller) {
					((Roller)edited).open();
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		rollerButtonStop.addActionListener(e -> {
			try {
				if(edited != null && edited instanceof Roller) {
					((Roller)edited).stop();
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		rollerButtonDown.addActionListener(e -> {
			try {
				if(edited != null && edited instanceof Roller) {
					((Roller)edited).close();
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		rollerPerc.addChangeListener(e -> {
			try {
				if(edited != null && edited instanceof Roller && rollerPerc.getValueIsAdjusting() == false) {
					((Roller)edited).setPosition(rollerPerc.getValue());
					cancelCellEditing();
				}
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
		});
		
		// used for Relay[]
		BoxLayout stackedPanelLO = new BoxLayout(stackedPanel, BoxLayout.Y_AXIS);
		stackedPanel.setLayout(stackedPanelLO);
		stackedPanel.setBackground(table.getSelectionBackground());
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if(value instanceof RelayInterface[]) {
			return getRelaysPanel((RelayInterface[]) value, table.getSelectionForeground());
		} else if(value instanceof Roller) {
			return getRollerPanel((Roller) value);
		} else if(value instanceof LightWhite) {
			return getLightPanel((LightWhite)value);
		} else if(value instanceof LightBulbRGB) { // RGBW Bulbs
			return getLightRGBWPanel((LightBulbRGB)value);
		} else if(value instanceof LightRGBW) { // RGBW2 (color mode)
			return getRGBWColorPanel((LightRGBW)value);
		} else if(value instanceof LightWhite[]) { // RGBW2 (white mode)
			return getRGBWWhitePanel((LightWhite[])value, table.getSelectionForeground());
		} else if(value instanceof Actions.Input[]) {
			return getActionsPanel((Actions.Input[])value, table);
		}
		return null;
	}
	
	private Component getRelaysPanel(RelayInterface[] relays, Color selColor) {
		stackedPanel.removeAll();
		for(RelayInterface rel: relays) {
			JLabel relayLabel = new JLabel(rel.getLabel());
			relayLabel.setForeground(selColor);
			JPanel relayPanel = new JPanel(new BorderLayout());
			JButton relayButton = new JButton();
			relayButton.addActionListener(e -> {
				try {
					if(edited != null) {
						rel.toggle();
						cancelCellEditing();
					}
				} catch (IOException ex) {
					Main.errorMsg(ex);
				}
			});
			relayButton.setBorder(DevicesTableRenderer.BUTTON_BORDERS);
			relayPanel.setOpaque(false);
			relayPanel.add(relayLabel, BorderLayout.CENTER);
			relayPanel.add(relayButton, BorderLayout.EAST);
			
			if(rel.isOn()) {
				relayButton.setText(DevicesTableRenderer.LABEL_ON);
				relayButton.setBackground(DevicesTableRenderer.BUTTON_ON_COLOR);
			} else {
				relayButton.setText(DevicesTableRenderer.LABEL_OFF);
				relayButton.setBackground(DevicesTableRenderer.BUTTON_OFF_COLOR);
			}
			stackedPanel.add(relayPanel);
		}
		edited = relays;
		return stackedPanel;
	}
	
	private Component getRollerPanel(Roller roller) {
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
		edited = roller;
		return rollerPanel;
	}
	
	private Component getLightPanel(LightWhite light) { // single
		lightLabel.setText(light.getLabel() + " " + light.getBrightness() + "%");
		lightBrightness.setValue(light.getBrightness());
		if(light.isOn()) {
			lightButton.setText(DevicesTableRenderer.LABEL_ON);
			lightButton.setBackground(DevicesTableRenderer.BUTTON_ON_COLOR);
		} else {
			lightButton.setText(DevicesTableRenderer.LABEL_OFF);
			lightButton.setBackground(DevicesTableRenderer.BUTTON_OFF_COLOR);
		}
		edited = light;
		return lightPanel;
	}
	
	private Component getLightRGBWPanel(LightBulbRGB light) {
		final int slider = light.isColorMode() ? light.getGain() : light.getBrightness();
		lightRGBLabel.setText(light.getLabel() + " " + slider + "%");
		lightRGBBrightness.setValue(slider);
		if(light.isOn()) {
			lightRGBButton.setText(DevicesTableRenderer.LABEL_ON);
			lightRGBButton.setBackground(DevicesTableRenderer.BUTTON_ON_COLOR);
		} else {
			lightRGBButton.setText(DevicesTableRenderer.LABEL_OFF);
			lightRGBButton.setBackground(DevicesTableRenderer.BUTTON_OFF_COLOR);
		}
		edited = light;
		return lightRGBPanel;
	}
	
	private Component getRGBWColorPanel(LightRGBW color) {
		colorRGBLabel.setText(color.getLabel());
		colorRGBGain.setValue(color.getGain());
		if(color.isOn()) {
			colorRGBButton.setText(DevicesTableRenderer.LABEL_ON);
			colorRGBButton.setBackground(DevicesTableRenderer.BUTTON_ON_COLOR);
		} else {
			colorRGBButton.setText(DevicesTableRenderer.LABEL_OFF);
			colorRGBButton.setBackground(DevicesTableRenderer.BUTTON_OFF_COLOR);
		}
		colorRGBGainLabel.setText(Main.LABELS.getString("labelShortGain") + " " + color.getGain() + "% ");
		colorRGBWhiteLabel.setText(Main.LABELS.getString("labelShortWhite") + " " + color.getWhite() + "% ");
		edited = color;
		return colorRGBPanel;
	}
	
	private Component getRGBWWhitePanel(LightWhite[] ligths, Color selColor) {
		stackedPanel.removeAll();
		for(LightWhite light: ligths) {
			JLabel relayLabel = new JLabel(light.getLabel());
			relayLabel.setForeground(selColor);
			JPanel relayPanel = new JPanel(new BorderLayout());
			JButton relayButton = new JButton();
			relayButton.addActionListener(e -> {
				try {
					if(edited != null) {
						light.toggle();
						cancelCellEditing();
					}
				} catch (IOException ex) {
					Main.errorMsg(ex);
				}
			});
			relayPanel.setOpaque(false);
			relayButton.setBorder(DevicesTableRenderer.BUTTON_BORDERS);
			relayPanel.add(relayLabel, BorderLayout.CENTER);
			relayPanel.add(relayButton, BorderLayout.EAST);
			
			if(light.isOn()) {
				relayButton.setText(DevicesTableRenderer.LABEL_ON);
				relayButton.setBackground(DevicesTableRenderer.BUTTON_ON_COLOR);
			} else {
				relayButton.setText(DevicesTableRenderer.LABEL_OFF);
				relayButton.setBackground(DevicesTableRenderer.BUTTON_OFF_COLOR);
			}
			stackedPanel.add(relayPanel);
		}
		edited = ligths;
		return stackedPanel;
	}
	
	private Component getActionsPanel(final Actions.Input[] inputs, JTable table) {
		stackedPanel.removeAll();
		for(Actions.Input act: inputs) {
			JPanel actionsPanel = new JPanel(new BorderLayout());
			JLabel actionsLabel = new JLabel(act.getLabel());
			actionsPanel.setBackground(table.getSelectionBackground());
			actionsLabel.setForeground(table.getSelectionForeground());
			actionsPanel.add(actionsLabel, BorderLayout.CENTER);
			JPanel actionsSouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			int numSupported = act.getTypesCount();
			for(String type: act.getSupportedTypes()) {
				boolean enabled = act.enabled(type);
				if(enabled || numSupported <= DevicesTableRenderer.MAX_ACTIONS_SHOWN) {
					String label;
					try {
						label = Main.LABELS.getString(type);
					} catch( MissingResourceException e) {
						label = "x";
					}
					JButton b = new JButton(label);
					if(enabled) {
						b.addActionListener(e -> {
							if(edited != null && edited instanceof Actions.Input[]) {
								table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
								try {
									new Thread(() -> {
										try {
											act.execute(type);
										} catch (IOException ex) {
											Main.errorMsg(ex);
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
					b.setBorder(DevicesTableRenderer.BUTTON_BORDERS_SMALL/*new EmptyBorder(DevicesTableRenderer.BUTTON_MARGIN_V, DevicesTableRenderer.BUTTON_MARGIN_H-2, DevicesTableRenderer.BUTTON_MARGIN_V, DevicesTableRenderer.BUTTON_MARGIN_H-2)*/);
					b.setBackground(DevicesTableRenderer.BUTTON_OFF_COLOR);
					actionsSouthPanel.add(b);
				}
			}
			actionsSouthPanel.setOpaque(false);
			actionsPanel.add(actionsSouthPanel, BorderLayout.SOUTH);
			actionsPanel.setOpaque(false);
			stackedPanel.add(actionsPanel);
		}
		edited = inputs;
		return stackedPanel;
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