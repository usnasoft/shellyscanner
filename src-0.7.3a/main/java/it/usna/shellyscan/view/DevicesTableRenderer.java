package it.usna.shellyscan.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.MissingResourceException;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.g1.modules.Actions;
import it.usna.shellyscan.model.device.g1.modules.LightBulbRGB;
import it.usna.shellyscan.model.device.g1.modules.LightRGBW;
import it.usna.shellyscan.model.device.g1.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.RollerInterface;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class DevicesTableRenderer implements TableCellRenderer {
	// Dimmer
	private JPanel lightPanel = new JPanel(new BorderLayout());
	private JLabel lightLabel = new JLabel();
	private JButton lightButton = new JButton();
	private JSlider lightBrightness = new JSlider(1, 100);	
	
	// RGBW Bulbs
	private JPanel lightRGBBulbPanel = new JPanel(new BorderLayout());
	private JLabel lightRGBBulbLabel = new JLabel();
	private JButton lightRGBBulbButton = new JButton();
	private JSlider lightRGBBulbBrightness = new JSlider(0, 100);
	
	// RGBW color
	private JPanel colorRGBPanel = new JPanel(new BorderLayout());
	private JLabel colorRGBLabel = new JLabel();
	private JButton colorRGBButton = new JButton();
	private JSlider colorRGBWGain = new JSlider(0, 100);
	private JSlider colorRGBWWhite = new JSlider(0, 100);
	private JLabel colorRGBGainLabel = new JLabel();
	private JLabel colorRGBWhiteLabel = new JLabel();
	
	// Roller
	private JPanel rollerPanel = new JPanel(new BorderLayout());
	private JLabel rollerLabel = new JLabel();
	private JSlider rollerPerc = new JSlider(0, 100);

	private JPanel stackedPanel = new JPanel();
	
	private JLabel labelPlain = new JLabel();
	
	static final Color BUTTON_ON_COLOR = Color.cyan;
	static final Color BUTTON_OFF_COLOR = Color.white;
	private static final int BUTTON_MARGIN_H = 12;
	private static final int BUTTON_MARGIN_V = 1;
	static final int MAX_ACTIONS_SHOWN = 5; // if supported actions <= then show also disabled buttons
	
	final static Border BUTTON_BORDERS = new EmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H, BUTTON_MARGIN_V, BUTTON_MARGIN_H);
	final static Border BUTTON_BORDERS_SMALL = new EmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H-2, BUTTON_MARGIN_V, BUTTON_MARGIN_H-2);
//	final static Dimension BUTTON_DIMENSION = new Dimension(38, 18);
	final static String LABEL_ON = Main.LABELS.getString("btnOnLabel");
	final static String LABEL_OFF = Main.LABELS.getString("btnOffLabel");

	private ArrayList<Integer> rowH = new ArrayList<>();

	public DevicesTableRenderer() {
		// Dimmer
		lightButton.setBorder(/*new EmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H, BUTTON_MARGIN_V, BUTTON_MARGIN_H)*/BUTTON_BORDERS);
		lightPanel.add(lightLabel, BorderLayout.CENTER);
		lightPanel.add(lightButton, BorderLayout.EAST);
		lightPanel.add(lightBrightness, BorderLayout.SOUTH);
		
		// RGBW Bulbs
		JPanel lightRGBSouthPanel = new JPanel(new BorderLayout());
		JButton lightEditRGBButton = new JButton(new ImageIcon(getClass().getResource("/images/Write16.png")));
		lightRGBBulbButton.setBorder(/*new EmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H, BUTTON_MARGIN_V, BUTTON_MARGIN_H)*/BUTTON_BORDERS);
		lightRGBBulbPanel.add(lightRGBBulbLabel, BorderLayout.CENTER);
		lightRGBBulbPanel.add(lightRGBBulbButton, BorderLayout.EAST);
		lightRGBBulbPanel.add(lightRGBSouthPanel, BorderLayout.SOUTH);
		lightRGBSouthPanel.add(lightRGBBulbBrightness, BorderLayout.CENTER);
		lightEditRGBButton.setBorder(new EmptyBorder(0, 3, 0, 3));
		lightEditRGBButton.setContentAreaFilled(false);
		lightRGBSouthPanel.add(lightEditRGBButton, BorderLayout.EAST);
		lightRGBSouthPanel.setOpaque(false);
		
		// RGBW
		colorRGBButton.setBorder(/*new EmptyBorder(BUTTON_MARGIN_V, BUTTON_MARGIN_H, BUTTON_MARGIN_V, BUTTON_MARGIN_H)*/BUTTON_BORDERS);
		colorRGBPanel.add(colorRGBLabel, BorderLayout.CENTER);
		colorRGBPanel.add(colorRGBButton, BorderLayout.EAST);
		JPanel colorRGBSlidersPanel = new JPanel();
		BoxLayout colorRGBSlidersPanelLO = new BoxLayout(colorRGBSlidersPanel, BoxLayout.X_AXIS);
		colorRGBSlidersPanel.setLayout(colorRGBSlidersPanelLO);
		JPanel stackedLabels = new JPanel(new GridLayout(2, 1));
		stackedLabels.setOpaque(false);
		stackedLabels.add(colorRGBGainLabel);
		stackedLabels.add(colorRGBWhiteLabel);
		colorRGBSlidersPanel.add(stackedLabels);
		JPanel stackedSliders = new JPanel(new GridLayout(2, 1));
		stackedSliders.setOpaque(false);
		stackedSliders.add(colorRGBWGain);
		stackedSliders.add(colorRGBWWhite);
		colorRGBSlidersPanel.add(stackedSliders);
		JButton editRGBButton = new JButton(new ImageIcon(getClass().getResource("/images/Write16.png")));
		editRGBButton.setBorder(new EmptyBorder(0, 3, 0, 3));
		editRGBButton.setContentAreaFilled(false);
		colorRGBSlidersPanel.add(editRGBButton);
		colorRGBSlidersPanel.setOpaque(false);
		colorRGBPanel.add(colorRGBSlidersPanel, BorderLayout.SOUTH);
		
		// Roller
		JPanel rollerSouthPanel = new JPanel(new BorderLayout());
		JPanel rollerButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
		rollerPanel.add(rollerLabel, BorderLayout.CENTER);
		rollerSouthPanel.add(rollerPerc, BorderLayout.CENTER);
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
		
		BoxLayout stackedPanelLO = new BoxLayout(stackedPanel, BoxLayout.Y_AXIS);
		stackedPanel.setLayout(stackedPanelLO);
		
		labelPlain.setOpaque(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		final JComponent ret;
		final Color foregroundColor = isSelected ? table.getSelectionForeground() : table.getForeground();
		if(value instanceof RelayInterface[]) {
			stackedPanel.removeAll();
			for(RelayInterface rel: (RelayInterface[]) value) { // 1, 1PM, EM, 2.5
				JLabel relayLabel = new JLabel(rel.getLabel());
				JPanel relayPanel = new JPanel(new BorderLayout());
				JButton relayButton = new JButton();
				relayButton.setBorder(BUTTON_BORDERS);

				relayPanel.setOpaque(false);
				relayPanel.add(relayLabel, BorderLayout.CENTER);
				relayPanel.add(relayButton, BorderLayout.EAST);
				relayLabel.setForeground(foregroundColor);
				
				if(rel.isOn()) {
					relayButton.setText(LABEL_ON);
					relayButton.setBackground(BUTTON_ON_COLOR);
				} else {
					relayButton.setText(LABEL_OFF);
					relayButton.setBackground(BUTTON_OFF_COLOR);
				}
				stackedPanel.add(relayPanel);
			}
			ret = stackedPanel;
		} else if(value instanceof Roller) { // 2.5
			Roller roller = (Roller)value;
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
		} else if(value instanceof LightWhite) { // Dimmer
			LightWhite light = (LightWhite)value;
			if(light.isOn()) {
				lightButton.setText(LABEL_ON);
				lightButton.setBackground(BUTTON_ON_COLOR);
			} else {
				lightButton.setText(LABEL_OFF);
				lightButton.setBackground(BUTTON_OFF_COLOR);
			}
			lightBrightness.setValue(light.getBrightness());
			lightLabel.setText(light.getLabel() + " " + light.getBrightness() + "%");
			lightLabel.setForeground(foregroundColor);
			ret = lightPanel;
		} else if(value instanceof LightBulbRGB) { // RGBW Bulbs
			LightBulbRGB light = (LightBulbRGB)value;
			if(light.isOn()) {
				lightRGBBulbButton.setText(LABEL_ON);
				lightRGBBulbButton.setBackground(BUTTON_ON_COLOR);
			} else {
				lightRGBBulbButton.setText(LABEL_OFF);
				lightRGBBulbButton.setBackground(BUTTON_OFF_COLOR);
			}
			final int slider = light.isColorMode() ? light.getGain() : light.getBrightness();
			lightRGBBulbBrightness.setValue(slider);
			lightRGBBulbLabel.setText(light.getLabel() + " " + slider + "%");
			lightRGBBulbLabel.setForeground(foregroundColor);
			ret = lightRGBBulbPanel;
		} else if(value instanceof LightRGBW) { // RGBW2 color
			LightRGBW color = (LightRGBW)value;
			if(color.isOn()) {
				colorRGBButton.setText(LABEL_ON);
				colorRGBButton.setBackground(BUTTON_ON_COLOR);
			} else {
				colorRGBButton.setText(LABEL_OFF);
				colorRGBButton.setBackground(BUTTON_OFF_COLOR);
			}
			colorRGBWGain.setValue(color.getGain());
			colorRGBWWhite.setValue(color.getWhite());
			colorRGBLabel.setText(color.getLabel());
			colorRGBLabel.setForeground(foregroundColor);
			colorRGBGainLabel.setForeground(foregroundColor);
			colorRGBGainLabel.setText(Main.LABELS.getString("labelShortGain") + " " + color.getGain() + "% ");
			colorRGBWhiteLabel.setForeground(foregroundColor);
			colorRGBWhiteLabel.setText(Main.LABELS.getString("labelShortWhite") + " " + color.getWhite() + "% ");
			ret = colorRGBPanel;
		} else if(value instanceof LightWhite[]) { // RGBW2 white
			stackedPanel.removeAll();
			for(LightWhite light: (LightWhite[]) value) {
				JLabel relayLabel = new JLabel(light.getLabel());
				JPanel relayPanel = new JPanel(new BorderLayout());
				JButton relayButton = new JButton();

				relayPanel.setOpaque(false);
				relayButton.setBorder(BUTTON_BORDERS);
				relayPanel.add(relayLabel, BorderLayout.CENTER);
				relayPanel.add(relayButton, BorderLayout.EAST);
				relayLabel.setForeground(foregroundColor);
				
				if(light.isOn()) {
					relayButton.setText(LABEL_ON);
					relayButton.setBackground(BUTTON_ON_COLOR);
				} else {
					relayButton.setText(LABEL_OFF);
					relayButton.setBackground(BUTTON_OFF_COLOR);
				}
				stackedPanel.add(relayPanel);
			}
			ret = stackedPanel;
		} else if(value instanceof Actions.Input[]) { // Button1 - I3
			ret = actionButtonsPanel((Actions.Input[])value, foregroundColor);
		} else {
			labelPlain.setText(value == null ? "" : value.toString());
			labelPlain.setForeground(foregroundColor);
			ret = labelPlain;
		}
		// adapt row height
		computeRowHeigth(row, table, ret);

		if(isSelected) {
			ret.setBackground(table.getSelectionBackground());
		} else if(row % 2 == 0 ) {
			ret.setBackground(Main.TAB_LINE1);
		} else {
			ret.setBackground(Main.TAB_LINE2);
		}
		return ret;
	}
	
	private JPanel actionButtonsPanel(final Actions.Input[] inputs, Color foregroundColor) {
		stackedPanel.removeAll();
		for(Actions.Input act: inputs) {
			JPanel actionsPanel = new JPanel(new BorderLayout());
			JLabel actionsLabel = new JLabel(act.getLabel());
			actionsLabel.setForeground(foregroundColor);
			actionsPanel.add(actionsLabel, BorderLayout.CENTER);
			JPanel actionsSouthPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
			int numSupported = act.getTypesCount();
			for(String type: act.getSupportedTypes()) {
				boolean enabled = act.enabled(type);
				if(enabled || numSupported <= MAX_ACTIONS_SHOWN) {
					String label;
					try {
						label = Main.LABELS.getString(type);
					} catch( MissingResourceException e) {
						label = "x";
					}
					JButton b = new JButton(label);
					b.setBorder(BUTTON_BORDERS_SMALL);
					b.setEnabled(enabled);
					b.setBackground(BUTTON_OFF_COLOR);
					actionsSouthPanel.add(b);
				}
			}
			actionsSouthPanel.setOpaque(false);
			actionsPanel.add(actionsSouthPanel, BorderLayout.SOUTH);
			actionsPanel.setOpaque(false);
			stackedPanel.add(actionsPanel);
		}
		return stackedPanel;
	}
	
	
	public void resetRowsHeight() {
		Collections.fill(rowH, 1);
	}

	private void computeRowHeigth(int rowIndex, JTable table, Component callVal) {
		int currentH;
		if(rowIndex >= rowH.size()) {
			while(rowH.size() <= rowIndex) {
				rowH.add(1);
			}
			currentH = 1;
		} else {
			currentH = rowH.get(rowIndex);
		}
		int thisH = callVal.getPreferredSize().height;
		if(currentH < thisH) {
			table.setRowHeight(rowIndex, thisH);
			rowH.set(rowIndex, thisH);
		}
	}
}

//https://coderanch.com/t/610585/java/setting-cell-JTable-JButton