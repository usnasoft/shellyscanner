package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import it.usna.shellyscan.model.device.g1.modules.LightRGBW;

public class DialogEditRGB extends JDialog {
	private static final long serialVersionUID = 1L;
	
	private JToggleButton switchButton;
	private JSlider sliderGain = new JSlider(0, 100/*, light.getGain()*/);
	private JSlider sliderRed = new JSlider(0, 255/*, light.getRed()*/);
	private JSlider sliderGreen = new JSlider(0, 255/*, light.getGreen()*/);
	private JSlider sliderBlue = new JSlider(0, 255/*, light.getBlue()*/);
	private JSlider sliderWhite = new JSlider(0, 255);
	private JLabel labelGain = new JLabel(/*light.getBrightness() + ""*/);
	private JLabel labelRed = new JLabel(/*light.getRed() + ""*/);
	private JLabel labelGreen = new JLabel(/*light.getGreen() + ""*/);
	private JLabel labelBlue = new JLabel(/*light.getBlue() + ""*/);
	private JLabel labelWhite = new JLabel(/*light.getBlue() + ""*/);
	private final JPanel previewColorPanel = new JPanel();
	
	/**
	 * @wbp.parser.constructor
	 */
	public DialogEditRGB(final Window owner, LightRGBW light) {
		super(owner, light.getLabel(), Dialog.ModalityType.MODELESS);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 10));

		JPanel colorPanel = pColor(light);

		getContentPane().add(modePanel(light), BorderLayout.NORTH);
		getContentPane().add(colorPanel, BorderLayout.CENTER);
		
		adjust(light);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	private JPanel modePanel(LightRGBW light) {
		JPanel typePanel = new JPanel();
		BoxLayout bl = new BoxLayout(typePanel, BoxLayout.X_AXIS);
		typePanel.setLayout(bl);
		typePanel.setBackground(Color.LIGHT_GRAY);

		switchButton = new JToggleButton(new ImageIcon(DialogEditRGB.class.getResource("/images/Standby24.png"))/*, light.isOn()*/);
		switchButton.setSelectedIcon(new ImageIcon(DialogEditRGB.class.getResource("/images/StandbyOn24.png")));
		switchButton.setRolloverIcon(new ImageIcon(DialogEditRGB.class.getResource("/images/Standby24.png")));
		switchButton.setRolloverSelectedIcon(new ImageIcon(DialogEditRGB.class.getResource("/images/StandbyOn24.png")));
		switchButton.setContentAreaFilled(false);
		switchButton.addActionListener(e -> {
			try {
				light.toggle();
				adjust(light);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		typePanel.add(Box.createHorizontalGlue());
		typePanel.add(switchButton);
		return typePanel;
	}
	
	 /**
	  * x@wbp.parser.entryPoint
	  */
	private JPanel pColor(LightRGBW light) {
		JPanel colorPanel = new JPanel();
		GridBagLayout gbl_panelC = new GridBagLayout();
		gbl_panelC.columnWidths = new int[] {0, 0, 30};
		gbl_panelC.rowHeights = new int[] {0, 0, 0, 0, 0, 30};
		gbl_panelC.columnWeights = new double[]{0.0, 0.0, 1.0};
		gbl_panelC.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0, 1.0};
		colorPanel.setLayout(gbl_panelC);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("labelGain"));
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
					e1.printStackTrace();
				}
			}
		});
		GridBagConstraints gbc_sliderB = new GridBagConstraints();
		gbc_sliderB.weightx = 10.0;
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
		
		JLabel lblNewLabel_1 = new JLabel(LABELS.getString("labelWhite"));
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel_1.insets = new Insets(0, 10, 5, 10);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 4;
		colorPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		labelWhite.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelWhite = new GridBagConstraints();
		gbc_labelWhite.insets = new Insets(0, 0, 2, 2);
		gbc_labelWhite.anchor = GridBagConstraints.NORTHEAST;
		gbc_labelWhite.gridx = 1;
		gbc_labelWhite.gridy = 4;
		colorPanel.add(labelWhite, gbc_labelWhite);
		
		GridBagConstraints gbc_slider = new GridBagConstraints();
		gbc_slider.insets = new Insets(0, 10, 5, 10);
		gbc_slider.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider.anchor = GridBagConstraints.NORTH;
		gbc_slider.gridx = 2;
		gbc_slider.gridy = 4;
		colorPanel.add(sliderWhite, gbc_slider);
		
		GridBagConstraints gbc_previewColorPanel = new GridBagConstraints();
		gbc_previewColorPanel.insets = new Insets(0, 10, 5, 10);
		gbc_previewColorPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_previewColorPanel.gridx = 2;
		gbc_previewColorPanel.gridy = 5;
		colorPanel.add(previewColorPanel, gbc_previewColorPanel);
		
		ChangeListener scl = e -> {
			if(sliderRed.getValueIsAdjusting() == false && sliderGreen.getValueIsAdjusting() == false && sliderBlue.getValueIsAdjusting() == false && sliderWhite.getValueIsAdjusting() == false) {
				try {
					light.setColor(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), sliderWhite.getValue());
					adjust(light);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		};
		sliderRed.addChangeListener(scl);
		sliderGreen.addChangeListener(scl);
		sliderBlue.addChangeListener(scl);
		sliderWhite.addChangeListener(scl);
		return colorPanel;
	}
	
	private void adjust(LightRGBW light) {
		int red = light.getRed();
		int green = light.getGreen();
		int blue = light.getBlue();
		
		switchButton.setSelected(light.isOn());
		sliderGain.setValue(light.getGain());
		sliderRed.setValue(red);
		sliderGreen.setValue(green);
		sliderBlue.setValue(blue);
		sliderWhite.setValue(light.getWhite());
		labelGain.setText(light.getGain() + "");
		labelRed.setText(red + "");
		labelGreen.setText(green + "");
		labelBlue.setText(blue + "");
		labelWhite.setText(light.getWhite() + "");
		
		previewColorPanel.setBackground(new Color(red, green, blue));
	}
}