package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.device.g1.modules.LightBulbRGB;
import it.usna.shellyscan.view.util.Kelvin2RGB;

public class DialogEditBulbRGB extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private JRadioButton colorButton;
	private JRadioButton whiteButton;
	private JToggleButton switchButton;
	private CardLayout cards = new CardLayout(0, 0);
	private JPanel panelCard = new JPanel(cards);
	
	private JSlider sliderBrightness;
	private JSlider sliderTemp;
	private JLabel labelBrightness = new JLabel();
	private JLabel labelTemp = new JLabel();
	
	private JSlider sliderGain;
	private JSlider sliderRed;
	private JSlider sliderGreen;
	private JSlider sliderBlue;
	private JLabel labelGain = new JLabel();
	private JLabel labelRed = new JLabel();
	private JLabel labelGreen = new JLabel();
	private JLabel labelBlue = new JLabel();
	private final JPanel previewColorPanel = new JPanel();
	private final JPanel previewWhitePanel = new JPanel();
	private static final Logger LOG = LoggerFactory.getLogger(DialogEditBulbRGB.class);
	private final JButton k3000 = new JButton("3000K");
	private final JButton k4500 = new JButton("4500K");
	private final JButton k6000 = new JButton("6000K");
	
	public DialogEditBulbRGB(final Window owner, LightBulbRGB light) {
		super(owner, light.getLabel(), Dialog.ModalityType.MODELESS);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 10));
		
		sliderBrightness = new JSlider(0, 100, light.getBrightness()); // set sliders value before "addChangeListener" to avoid call on initial change
		sliderTemp = new JSlider(LightBulbRGB.MIN_TEMP, LightBulbRGB.MAX_TEMP, light.getTemperature());
		sliderGain = new JSlider(0, 100, light.getGain());
		sliderRed = new JSlider(0, 255, light.getRed());
		sliderGreen = new JSlider(0, 255, light.getGreen());
		sliderBlue = new JSlider(0, 255, light.getBlue());

		JPanel colorPanel = pColor(light);
		panelCard.add(colorPanel, "ColorPanel");
		
		JPanel panelWhite = pWhite(light);
		panelCard.add(panelWhite, "WhitePanel");

		getContentPane().add(modePanel(light), BorderLayout.NORTH);
		getContentPane().add(panelCard, BorderLayout.CENTER);
		
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape_close");
		getRootPane().getActionMap().put("escape_close", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		pack();
		setLocationRelativeTo(owner);
		adjust(light);
		setVisible(true);
	}
	
	private JPanel modePanel(LightBulbRGB light) {
		JPanel typePanel = new JPanel();
		BoxLayout bl = new BoxLayout(typePanel, BoxLayout.X_AXIS);
		typePanel.setLayout(bl);
		typePanel.setBackground(Color.LIGHT_GRAY);

		whiteButton = new JRadioButton(LABELS.getString("labelWhite"));
		whiteButton.addActionListener(e -> {
			if(whiteButton.isSelected()) {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					light.setColorMode(false);
					adjust(light);
				} catch (IOException e1) {
					LOG.error("whiteButton", e1);
				}
				setCursor(Cursor.getDefaultCursor());
			}
		});
		colorButton = new JRadioButton(LABELS.getString("labelColor"));
		colorButton.addActionListener(e -> {
			if(colorButton.isSelected()) {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					light.setColorMode(true);
					adjust(light);
				} catch (IOException e1) {
					LOG.error("colorButton", e1);
				}
				setCursor(Cursor.getDefaultCursor());
			}
		});
		switchButton = new JToggleButton(new ImageIcon(DialogEditBulbRGB.class.getResource("/images/Standby24.png"))/*, light.isOn()*/);
		switchButton.setSelectedIcon(new ImageIcon(DialogEditBulbRGB.class.getResource("/images/StandbyOn24.png")));
		switchButton.setRolloverEnabled(false);
		switchButton.setContentAreaFilled(false);
		switchButton.addActionListener(e -> {
			try {
				light.toggle();
				adjust(light);
			} catch (IOException e1) {
				LOG.error("switchButton", e1);
			}
		});
		typePanel.add(Box.createHorizontalStrut(10));
		typePanel.add(whiteButton);
		typePanel.add(Box.createHorizontalStrut(10));
		typePanel.add(colorButton);
		typePanel.add(Box.createHorizontalGlue());
		typePanel.add(switchButton);

		ButtonGroup btnGroup = new ButtonGroup();
		btnGroup.add(whiteButton);
		btnGroup.add(colorButton);
		return typePanel;
	}
	
	/**
     * @wbp.parser.entryPoint
     */
	private JPanel pWhite(LightBulbRGB light) {
		JPanel panelWhite = new JPanel();
		GridBagLayout gbl_panelWhite = new GridBagLayout();
		gbl_panelWhite.columnWidths = new int[] {10, 0, 0, 10};
		gbl_panelWhite.rowHeights = new int[] {0, 0, 0, 0, 40};
		gbl_panelWhite.columnWeights = new double[]{0.0, 0, 0.0, 1.0};
		gbl_panelWhite.rowWeights = new double[]{0.0, 0.0, 1.0, 1.0, Double.MAX_VALUE};
		panelWhite.setLayout(gbl_panelWhite);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("labelBrightness"));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel.insets = new Insets(0, 10, 5, 10);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		panelWhite.add(lblNewLabel, gbc_lblNewLabel);
		
		labelBrightness.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelBrightness = new GridBagConstraints();
		gbc_labelBrightness.insets = new Insets(0, 0, 5, 5);
		gbc_labelBrightness.anchor = GridBagConstraints.NORTHEAST;
		gbc_labelBrightness.gridx = 1;
		gbc_labelBrightness.gridy = 0;
		panelWhite.add(labelBrightness, gbc_labelBrightness);
		
		sliderBrightness.addChangeListener(e -> {
			if(sliderBrightness.getValueIsAdjusting() == false) {
				try {
					light.setBrightness(sliderBrightness.getValue());
					adjust(light);
				} catch (IOException e1) {
					LOG.error("sliderBrightness", e1);
				}
			} else {
				adjustBrightness(sliderBrightness.getValue());
			}
		});
		GridBagConstraints gbc_slider = new GridBagConstraints();
		gbc_slider.anchor = GridBagConstraints.NORTH;
		gbc_slider.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider.insets = new Insets(0, 10, 5, 10);
		gbc_slider.gridx = 3;
		gbc_slider.gridy = 0;
		panelWhite.add(sliderBrightness, gbc_slider);
		
		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("labelTemperatureBulb"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_1.insets = new Insets(0, 10, 5, 10);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		panelWhite.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		labelTemp.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.anchor = GridBagConstraints.NORTHEAST;
		gbc_lblNewLabel_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_2.gridx = 1;
		gbc_lblNewLabel_2.gridy = 1;
		panelWhite.add(labelTemp, gbc_lblNewLabel_2);
		
		sliderTemp.addChangeListener(e -> {
			if(sliderTemp.getValueIsAdjusting() == false) {
				try {
					light.setTemperature(sliderTemp.getValue());
					adjust(light);
				} catch (IOException e1) {
					LOG.error("sliderTemp", e1);
				}
			} else {
				adjustLightTemp(sliderTemp.getValue());
			}
		});
		GridBagConstraints gbc_slider_1 = new GridBagConstraints();
		gbc_slider_1.anchor = GridBagConstraints.NORTH;
		gbc_slider_1.insets = new Insets(0, 10, 5, 10);
		gbc_slider_1.weightx = 10.0;
		gbc_slider_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_1.gridx = 3;
		gbc_slider_1.gridy = 1;
		panelWhite.add(sliderTemp, gbc_slider_1);
		
		JPanel kbuttonsPanel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 4;
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 2;
		panelWhite.add(kbuttonsPanel, gbc_panel);
		
		k3000.setBackground(Kelvin2RGB.kelvinToColor(3000));
		k4500.setBackground(Kelvin2RGB.kelvinToColor(4500));
		k6000.setBackground(Kelvin2RGB.kelvinToColor(6000));
		k3000.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		k4500.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		k6000.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
		kbuttonsPanel.add(k3000);
		kbuttonsPanel.add(k4500);
		kbuttonsPanel.add(k6000);
		k3000.addActionListener(e -> adjustLightTemp(3000)); // adjustLightTemp change slider value -> slider event change device setting
		k4500.addActionListener(e -> adjustLightTemp(4500));
		k6000.addActionListener(e -> adjustLightTemp(6000));
		
		GridBagConstraints gbc_previewWhitePanel = new GridBagConstraints();
		gbc_previewWhitePanel.gridwidth = 4;
		gbc_previewWhitePanel.insets = new Insets(0, 10, 0, 10);
		gbc_previewWhitePanel.fill = GridBagConstraints.BOTH;
		gbc_previewWhitePanel.gridx = 0;
		gbc_previewWhitePanel.gridy = 3;
		previewWhitePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		panelWhite.add(previewWhitePanel, gbc_previewWhitePanel);
		return panelWhite;
	}
	
	 /**
	  * @wbp.parser.entryPoint
	  */
	private JPanel pColor(LightBulbRGB light) {
		JPanel colorPanel = new JPanel();
		GridBagLayout gbl_panelC = new GridBagLayout();
		gbl_panelC.columnWidths = new int[] {0, 40, 0};
		gbl_panelC.rowHeights = new int[] {0, 0, 0, 0, 30};
		gbl_panelC.columnWeights = new double[]{0.0, 0.0, 1.0};
		gbl_panelC.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
		colorPanel.setLayout(gbl_panelC);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("labelBrightness"));
		lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel.insets = new Insets(0, 10, 5, 10);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		colorPanel.add(lblNewLabel, gbc_lblNewLabel);

		labelGain.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelBrightness = new GridBagConstraints();
		gbc_labelBrightness.insets = new Insets(0, 0, 2, 2);
		gbc_labelBrightness.anchor = GridBagConstraints.NORTHEAST;
		gbc_labelBrightness.gridx = 1;
		gbc_labelBrightness.gridy = 0;
		colorPanel.add(labelGain, gbc_labelBrightness);
		
		sliderGain.addChangeListener(e -> {
			if(sliderGain.getValueIsAdjusting() == false) {
				try {
					light.setGain(sliderGain.getValue());
					adjust(light);
				} catch (IOException e1) {
					LOG.error("sliderGain", e1);
				}
			} else {
				adjustGain(sliderGain.getValue());
			}
		});
		GridBagConstraints gbc_sliderB = new GridBagConstraints();
		gbc_sliderB.anchor = GridBagConstraints.NORTH;
		gbc_sliderB.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderB.insets = new Insets(0, 10, 5, 10);
		gbc_sliderB.gridx = 2;
		gbc_sliderB.gridy = 0;
		colorPanel.add(sliderGain, gbc_sliderB);
		
		JLabel lblNewLabel_2 = new JLabel(LABELS.getString("labelRed"));
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_2.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_2.insets = new Insets(0, 10, 5, 10);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 1;
		colorPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		labelRed.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelRed = new GridBagConstraints();
		gbc_labelRed.insets = new Insets(0, 0, 2, 2);
		gbc_labelRed.anchor = GridBagConstraints.NORTHEAST;
		gbc_labelRed.gridx = 1;
		gbc_labelRed.gridy = 1;
		colorPanel.add(labelRed, gbc_labelRed);

		GridBagConstraints gbc_sliderRed = new GridBagConstraints();
		gbc_sliderRed.insets = new Insets(0, 10, 5, 10);
		gbc_sliderRed.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderRed.anchor = GridBagConstraints.NORTH;
		gbc_sliderRed.gridx = 2;
		gbc_sliderRed.gridy = 1;
		colorPanel.add(sliderRed, gbc_sliderRed);
		
		JLabel lblNewLabel_3 = new JLabel(LABELS.getString("labelGreen"));
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_3.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 10, 5, 10);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 2;
		colorPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		labelRed.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelGreen = new GridBagConstraints();
		gbc_labelGreen.insets = new Insets(0, 0, 2, 2);
		gbc_labelGreen.anchor = GridBagConstraints.NORTHEAST;
		gbc_labelGreen.gridx = 1;
		gbc_labelGreen.gridy = 2;
		colorPanel.add(labelGreen, gbc_labelGreen);
		
		GridBagConstraints gbc_sliderGren = new GridBagConstraints();
		gbc_sliderGren.insets = new Insets(0, 10, 5, 10);
		gbc_sliderGren.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderGren.anchor = GridBagConstraints.NORTH;
		gbc_sliderGren.gridx = 2;
		gbc_sliderGren.gridy = 2;
		colorPanel.add(sliderGreen, gbc_sliderGren);
		
		JLabel lblNewLabel_4 = new JLabel(LABELS.getString("labelBlue"));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_4.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_4.insets = new Insets(0, 10, 5, 10);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 3;
		colorPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		labelBlue.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelBlue = new GridBagConstraints();
		gbc_labelBlue.insets = new Insets(0, 0, 2, 2);
		gbc_labelBlue.anchor = GridBagConstraints.NORTHEAST;
		gbc_labelBlue.gridx = 1;
		gbc_labelBlue.gridy = 3;
		colorPanel.add(labelBlue, gbc_labelBlue);
		
		GridBagConstraints gbc_sliderBlue = new GridBagConstraints();
		gbc_sliderBlue.insets = new Insets(0, 10, 5, 10);
		gbc_sliderBlue.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderBlue.anchor = GridBagConstraints.NORTH;
		gbc_sliderBlue.gridx = 2;
		gbc_sliderBlue.gridy = 3;
		colorPanel.add(sliderBlue, gbc_sliderBlue);
		
		GridBagConstraints gbc_previwPanel = new GridBagConstraints();
		gbc_previwPanel.insets = new Insets(0, 10, 5, 10);
		gbc_previwPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_previwPanel.gridx = 2;
		gbc_previwPanel.gridy = 4;
		previewColorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		colorPanel.add(previewColorPanel, gbc_previwPanel);

		ChangeListener scl = e -> {
			if(sliderRed.getValueIsAdjusting() == false && sliderGreen.getValueIsAdjusting() == false && sliderBlue.getValueIsAdjusting() == false) {
				try {
					light.setColor(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue()/*, light.getWhite()*/);
					adjust(light);
				} catch (IOException e1) {
					LOG.error("slideColor", e1);
				}
			} else {
				adjustLightRGB(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue());
			}
		};
		sliderRed.addChangeListener(scl);
		sliderGreen.addChangeListener(scl);
		sliderBlue.addChangeListener(scl);
		return colorPanel;
	}
	
	private void adjust(LightBulbRGB light) {
		if(light.isColorMode()) {
			colorButton.setSelected(true);
			adjustGain(light.getGain());
			adjustLightRGB(light.getRed(), light.getGreen(), light.getBlue());
			cards.show(panelCard, "ColorPanel");
		} else {
			whiteButton.setSelected(true);
			adjustBrightness(light.getBrightness());
			adjustLightTemp(light.getTemperature());
			cards.show(panelCard, "WhitePanel");
		}
		switchButton.setSelected(light.isOn());
//		
//		adjustBrightness(light.getBrightness());
//		adjustLightTemp(light.getTemp());
//		adjustGain(light.getGain());
//		adjustLightRGB(light.getRed(), light.getGreen(), light.getBlue());
	}
	
	private void adjustBrightness(int br) {
		sliderBrightness.setValue(br);
		labelBrightness.setText(br + "%");
	}
	
	private void adjustLightTemp(int temp) {
		sliderTemp.setValue(temp);
		labelTemp.setText(temp + "K");
		previewWhitePanel.setBackground(Kelvin2RGB.kelvinToColor(temp));
	}
	
	private void adjustGain(int g) {
		sliderGain.setValue(g);
		labelGain.setText(g + "%");
	}
	
	private void adjustLightRGB(int red, int green, int blue) {
		sliderRed.setValue(red);
		sliderGreen.setValue(green);
		sliderBlue.setValue(blue);
		labelRed.setText(red + "");
		labelGreen.setText(green + "");
		labelBlue.setText(blue + "");
		previewColorPanel.setBackground(new Color(red, green, blue));
	}
}