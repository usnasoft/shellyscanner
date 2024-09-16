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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.RGBWInterface;

public class DialogEditRGBW extends JDialog {
	private static final long serialVersionUID = 1L;

	private UsnaToggleAction toggleAction;
	private JSlider sliderGain;
	private JSlider sliderRed;
	private JSlider sliderGreen;
	private JSlider sliderBlue;
	private JSlider sliderWhite;
	private JLabel labelGain = new JLabel();
	private JLabel labelRed = new JLabel();
	private JLabel labelGreen = new JLabel();
	private JLabel labelBlue = new JLabel();
	private JLabel labelWhite = new JLabel();
	private final JPanel previewColorPanel = new JPanel();
	private final static Logger LOG = LoggerFactory.getLogger(DialogEditRGBW.class);

	/**
	 * @wbp.parser.constructor
	 */
	public DialogEditRGBW(final Window owner, RGBInterface light) {
		super(owner, light.getLabel(), Dialog.ModalityType.MODELESS);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 10));

		sliderGain = new JSlider(0, 100, light.getGain()); // set sliders value before "addChangeListener" to avoid call on initial change
		sliderRed = new JSlider(0, 255, light.getRed());
		sliderGreen = new JSlider(0, 255, light.getGreen());
		sliderBlue = new JSlider(0, 255, light.getBlue());
		if(light instanceof RGBWInterface rgbw) {
			sliderWhite = new JSlider(0, 255, rgbw.getWhite());
		}

		JPanel colorPanel = pColor(light);

		getContentPane().add(buttonsPanel(light), BorderLayout.NORTH);
		getContentPane().add(colorPanel, BorderLayout.CENTER);

		adjust(light);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	private JPanel buttonsPanel(RGBInterface light) {
		JPanel panel = new JPanel();
		BoxLayout bl = new BoxLayout(panel, BoxLayout.X_AXIS);
		panel.setLayout(bl);
		panel.setBackground(Color.LIGHT_GRAY);

		toggleAction = new UsnaToggleAction(null, "/images/Standby24.png", "/images/StandbyOn24.png", e -> {
			try {
				light.toggle();
				adjust(light);
			} catch (IOException e1) {
				LOG.error("switchButton", e1);
			}
		});
		JButton switchButton = new JButton(toggleAction);
		switchButton.setContentAreaFilled(false);

		panel.add(Box.createHorizontalGlue());
		panel.add(switchButton);
		return panel;
	}

	/**
	 * x@wbp.parser.entryPoint
	 */
	private JPanel pColor(RGBInterface light) {
		JPanel colorPanel = new JPanel();
		GridBagLayout gbl_panelC = new GridBagLayout();
		gbl_panelC.columnWidths = new int[] {0, 30, 30};
		gbl_panelC.rowHeights = new int[] {0, 0, 0, 0, 0, 0, 20};
		//		gbl_panelC.columnWeights = new double[]{0.0, 1.0, 1.0};
		//		gbl_panelC.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0, 1.0, 1.0};
		colorPanel.setLayout(gbl_panelC);

		JLabel lblNewLabel = new JLabel(LABELS.getString("labelGain"));
		lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblNewLabel.insets = new Insets(0, 10, 5, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		colorPanel.add(lblNewLabel, gbc_lblNewLabel);

		labelGain.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelBrightness = new GridBagConstraints();
		gbc_labelBrightness.insets = new Insets(0, 0, 5, 5);
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
		gbc_lblNewLabel_2.insets = new Insets(0, 10, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 1;
		colorPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);

		labelRed.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelRed = new GridBagConstraints();
		gbc_labelRed.insets = new Insets(0, 0, 5, 5);
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
		gbc_lblNewLabel_3.insets = new Insets(0, 10, 5, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 2;
		colorPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);

		labelRed.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelGreen = new GridBagConstraints();
		gbc_labelGreen.insets = new Insets(0, 0, 5, 5);
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
		gbc_lblNewLabel_4.insets = new Insets(0, 10, 5, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 3;
		colorPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);

		labelBlue.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_labelBlue = new GridBagConstraints();
		gbc_labelBlue.insets = new Insets(0, 0, 5, 5);
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
		
		if(light instanceof RGBWInterface ) {
			JLabel lblNewLabel_1 = new JLabel(LABELS.getString("labelWhite"));
			GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
			gbc_lblNewLabel_1.fill = GridBagConstraints.HORIZONTAL;
			gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
			gbc_lblNewLabel_1.insets = new Insets(0, 10, 5, 5);
			gbc_lblNewLabel_1.gridx = 0;
			gbc_lblNewLabel_1.gridy = 4;
			colorPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

			labelWhite.setHorizontalAlignment(SwingConstants.RIGHT);
			GridBagConstraints gbc_labelWhite = new GridBagConstraints();
			gbc_labelWhite.insets = new Insets(0, 0, 5, 5);
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
		}

		JPanel colorsPanel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.gridwidth = 3;
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 5;
		colorPanel.add(colorsPanel, gbc_panel);

		JButton redButton = new JButton(new UsnaAction(e -> {
			try {
				if(light instanceof RGBWInterface rgbw) {
					rgbw.setColor(255, 0, 0, 0);
				} else {
					light.setColor(255, 0, 0);
				}
				adjust(light);
			} catch (IOException e1) {
				LOG.error("color button", e1);
			}
		}));
		redButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		redButton.setBackground(Color.RED);
		JButton greenButton = new JButton(new UsnaAction(e -> {
			try {
				if(light instanceof RGBWInterface rgbw) {
					rgbw.setColor(0, 255, 0, 0);
				} else {
					light.setColor(0, 255, 0);
				}
				adjust(light);
			} catch (IOException e1) {
				LOG.error("color button", e1);
			}
		}));
		greenButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		greenButton.setBackground(Color.GREEN);
		JButton blueButton = new JButton(new UsnaAction(e -> {
			try {
				if(light instanceof RGBWInterface rgbw) {
					rgbw.setColor(0, 0, 255, 0);
				} else {
					light.setColor(0, 0, 255);
				}
				adjust(light);
			} catch (IOException e1) {
				LOG.error("color button", e1);
			}
		}));
		blueButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		blueButton.setBackground(Color.BLUE);

		JButton whiteButton = new JButton(new UsnaAction(e -> {
			try {
				if(light instanceof RGBWInterface rgbw) {
					rgbw.setColor(0, 0, 0, 255);
				} else {
					light.setColor(255, 255, 255);
				}
				adjust(light);
			} catch (IOException e1) {
				LOG.error("color button", e1);
			}
		}));
		whiteButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		whiteButton.setBackground(Color.WHITE);

		colorsPanel.add(redButton);
		colorsPanel.add(greenButton);
		colorsPanel.add(blueButton);
		colorsPanel.add(whiteButton);

		GridBagConstraints gbc_previewColorPanel = new GridBagConstraints();
		gbc_previewColorPanel.anchor = GridBagConstraints.NORTH;
		gbc_previewColorPanel.gridwidth = 3;
		gbc_previewColorPanel.insets = new Insets(0, 40, 0, 40);
		gbc_previewColorPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_previewColorPanel.gridx = 0;
		gbc_previewColorPanel.gridy = 6;
		colorPanel.add(previewColorPanel, gbc_previewColorPanel);
		previewColorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		ChangeListener sclRGB = e -> {
			if(sliderRed.getValueIsAdjusting() == false && sliderGreen.getValueIsAdjusting() == false && sliderBlue.getValueIsAdjusting() == false) {
				try {
					light.setColor(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue());
					adjust(light);
				} catch (IOException e1) {
					LOG.error("sliderColor", e1);
				}
			} else {
				adjustLightRGBW(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), sliderWhite.getValue());
			}
		};
		sliderRed.addChangeListener(sclRGB);
		sliderGreen.addChangeListener(sclRGB);
		sliderBlue.addChangeListener(sclRGB);
		if(light instanceof RGBWInterface rgbw) {
			sliderWhite.addChangeListener( e -> {
				if(sliderWhite.getValueIsAdjusting() == false) {
					try {
						rgbw.setWhite( sliderWhite.getValue());
						adjust(light);
					} catch (IOException e1) {
						LOG.error("sliderColor", e1);
					}
				} else {
					adjustLightRGBW(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), sliderWhite.getValue());
				}
			});
		}
		return colorPanel;
	}

	private void adjust(RGBInterface light) {
		toggleAction.setSelected(light.isOn());
		adjustGain(light.getGain());
		if(light instanceof RGBWInterface rgbw) {
			adjustLightRGBW(rgbw.getRed(), rgbw.getGreen(), rgbw.getBlue(), rgbw.getWhite());
		} else {
			adjustLightRGB(light.getRed(), light.getGreen(), light.getBlue());
		}
	}

	private void adjustGain(int g) {
		sliderGain.setValue(g);
		labelGain.setText(g + "");
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

	private void adjustLightRGBW(int red, int green, int blue, int white) {
		sliderRed.setValue(red);
		sliderGreen.setValue(green);
		sliderBlue.setValue(blue);
		sliderWhite.setValue(white);
		labelRed.setText(red + "");
		labelGreen.setText(green + "");
		labelBlue.setText(blue + "");
		labelWhite.setText(white + "");

		// rgbw -> rgb
		int rr = red + white * 2;
		int gg = green + white * 2;
		int bb = blue + white * 2;
		int max = rr;
		if(gg > max) max = gg;
		if(bb > max) max = bb;
		if(max > 255) {
			rr = (int)(rr * 255f / max);
			gg = (int)(gg * 255f / max);
			bb = (int)(bb * 255f / max);
		}
		previewColorPanel.setBackground(new Color(rr, gg, bb));
	}
}