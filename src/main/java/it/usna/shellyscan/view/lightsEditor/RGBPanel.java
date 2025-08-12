package it.usna.shellyscan.view.lightsEditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.RGBWInterface;
import it.usna.swing.VerticalFlowLayout;

/**
 * RGB(W)Panel
 */
public class RGBPanel extends LightPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(RGBPanel.class);
	
	private final RGBInterface light;
	private JLabel labelGain = new JLabel();
	private JLabel labelRed = new JLabel();
	private JLabel labelGreen = new JLabel();
	private JLabel labelBlue = new JLabel();
	private JLabel labelWhite = new JLabel();
	private final JSlider sliderGain;
	private final JSlider sliderRed;
	private final JSlider sliderGreen;
	private final JSlider sliderBlue;
	private JSlider sliderWhite;
	private final UsnaToggleAction switchAction;
	private final JPanel previewColorPanel = new JPanel();
	private final ChangeListener rgbSliderListener;
	private ChangeListener whiteSliderListener;

	public RGBPanel(final RGBInterface light) {
		this.light = light;
		setBorder(BorderFactory.createEmptyBorder(4, 8, 10, 8));
		setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));

		// set initial values to avoid listeners to call the device in initial adjust
		sliderGain = new JSlider(0, 100, light.getGain());
		sliderRed = new JSlider(0, 255, light.getRed());
		sliderGreen = new JSlider(0, 255, light.getGreen());
		sliderBlue = new JSlider(0, 255, light.getBlue());
		if(light instanceof RGBWInterface rgbw) {
			sliderWhite = new JSlider(0, 255, rgbw.getWhite());
		}

		rgbSliderListener = e -> {
			if(sliderRed.getValueIsAdjusting() == false && sliderGreen.getValueIsAdjusting() == false && sliderBlue.getValueIsAdjusting() == false) {
				try {
					light.setColor(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue());
					adjust();
				} catch (IOException e1) {
					LOG.error("sliderColor", e1);
				}
			} else {
				labelRed.setText(LABELS.getString("labelRed") + ": " + sliderRed.getValue());
				labelGreen.setText(LABELS.getString("labelGreen") + ": " + sliderGreen.getValue());
				labelBlue.setText(LABELS.getString("labelBlue") + ": " + sliderBlue.getValue());
				colorPanel(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), light instanceof RGBWInterface ? sliderWhite.getValue() : 0);
			}
		};

		JPanel switchPanel = new JPanel(new BorderLayout(10, 0));
		switchPanel.setOpaque(false);
		switchPanel.add(labelGain, BorderLayout.NORTH);
		switchAction = new UsnaToggleAction(null, "/images/Standby24.png", "/images/StandbyOn24.png", e -> {
			try {
				light.toggle();
				adjust();
			} catch (IOException e1) {
				LOG.error("toggle", e1);
			}
		});
		JButton switchButton = new JButton(switchAction);
		switchButton.setContentAreaFilled(false);
		switchButton.setBorder(BorderFactory.createEmptyBorder());
		switchPanel.add(switchButton, BorderLayout.EAST);
		switchPanel.add(sliderGain, BorderLayout.CENTER);
		sliderGain.addChangeListener(e -> {
			if(sliderGain.getValueIsAdjusting() == false) {
				try {
					light.setGain(sliderGain.getValue());
					adjust();
				} catch (IOException e1) {
					LOG.error("sliderGain", e1);
				}
			} else {
				labelGain.setText(light.getLabel() + " " + sliderGain.getValue() + "%");
			}
		});
		this.add(switchPanel);
		JPanel redPanel = new JPanel(new BorderLayout(10, 0));
		redPanel.setOpaque(false);
		redPanel.add(labelRed, BorderLayout.NORTH);
		redPanel.add(sliderRed, BorderLayout.CENTER);
		redPanel.add(Box.createHorizontalStrut(DialogEditLights.offImg.getIconWidth()), BorderLayout.EAST);
		this.add(redPanel);

		JPanel greenPanel = new JPanel(new BorderLayout(10, 0));
		greenPanel.setOpaque(false);
		greenPanel.add(labelGreen, BorderLayout.NORTH);
		greenPanel.add(sliderGreen, BorderLayout.CENTER);
		greenPanel.add(Box.createHorizontalStrut(DialogEditLights.offImg.getIconWidth()), BorderLayout.EAST);
		this.add(greenPanel);

		JPanel bluePanel = new JPanel(new BorderLayout(10, 0));
		bluePanel.setOpaque(false);
		bluePanel.add(labelBlue, BorderLayout.NORTH);
		bluePanel.add(sliderBlue, BorderLayout.CENTER);
		bluePanel.add(Box.createHorizontalStrut(DialogEditLights.offImg.getIconWidth()), BorderLayout.EAST);
		this.add(bluePanel);
		
		if(light instanceof RGBWInterface rgbw) {
			whiteSliderListener = e -> {
				if(sliderWhite.getValueIsAdjusting() == false) {
					try {
						rgbw.setWhite(sliderWhite.getValue());
						adjust();
					} catch (IOException e1) {
						LOG.error("sliderWhite", e1);
					}
				} else {
					labelWhite.setText(LABELS.getString("labelWhite") + ": " + sliderWhite.getValue());
					colorPanel(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), sliderWhite.getValue());
				}
			};
			JPanel whitePanel = new JPanel(new BorderLayout(10, 0));
			whitePanel.setOpaque(false);
			whitePanel.add(labelWhite, BorderLayout.NORTH);
			whitePanel.add(sliderWhite, BorderLayout.CENTER);
			whitePanel.add(Box.createHorizontalStrut(DialogEditLights.offImg.getIconWidth()), BorderLayout.EAST);
			this.add(whitePanel);
		}

		JPanel colorsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		colorsPanel.setOpaque(false);
		JButton redButton = new JButton(new RGBButtonAction(255, 0, 0));
		redButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		redButton.setBackground(Color.RED);
		JButton greenButton = new JButton(new RGBButtonAction(0, 255, 0));
		greenButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		greenButton.setBackground(Color.GREEN);
		JButton yellowButton = new JButton(new RGBButtonAction(255, 255, 0));
		yellowButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		yellowButton.setBackground(Color.YELLOW);
		JButton blueButton = new JButton(new RGBButtonAction(0, 0, 255));
		blueButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		blueButton.setBackground(Color.BLUE);
		JButton violetButton = new JButton(new RGBButtonAction(255, 0, 255));
		violetButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		violetButton.setBackground(new Color(255, 0, 255));
		JButton whiteButton = new JButton(new UsnaAction(e -> {
			try {
				if(light instanceof RGBWInterface rgbw) {
					rgbw.setColor(0, 0, 0, 255);
				} else {
					light.setColor(255, 255, 255);
				}
				adjust();
			} catch (IOException e1) {
				LOG.error("color button", e1);
			}
		}));
		whiteButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		whiteButton.setBackground(Color.WHITE);

		colorsPanel.add(redButton);
		colorsPanel.add(greenButton);
		colorsPanel.add(yellowButton);
		colorsPanel.add(blueButton);
		colorsPanel.add(violetButton);
		colorsPanel.add(whiteButton);
		this.add(colorsPanel);

		previewColorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		previewColorPanel.setPreferredSize(new Dimension(200, 10));
		this.add(previewColorPanel);

		adjust();
	}

	private class RGBButtonAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final int red, green, blue;

		private RGBButtonAction(int red, int green, int blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if(light instanceof RGBWInterface rgbw) {
					rgbw.setColor(red, green, blue, 0);
				} else {
					light.setColor(red, green, blue);
				}
				adjust();
			} catch (IOException e1) {
				LOG.error("color button", e1);
			}
		}
	}

	@Override
	public void change(boolean on) throws IOException {
		light.change(on);
		adjust();
	}

	private void adjust() {
		sliderRed.removeChangeListener(rgbSliderListener);
		sliderGreen.removeChangeListener(rgbSliderListener);
		sliderBlue.removeChangeListener(rgbSliderListener);
		if(light instanceof RGBWInterface) {
			sliderWhite.removeChangeListener(whiteSliderListener);
		}

		switchAction.setSelected(light.isOn());
		int red = light.getRed();
		int green = light.getGreen();
		int blue = light.getBlue();
		int gain = light.getGain();

		sliderRed.setValue(red);
		sliderGreen.setValue(green);
		sliderBlue.setValue(blue);	
		sliderGain.setValue(gain);
		labelRed.setText(LABELS.getString("labelRed") + ": " + red);
		labelGreen.setText(LABELS.getString("labelGreen") + ": " + green);
		labelBlue.setText(LABELS.getString("labelBlue") + ": " + blue);
		labelGain.setText(light.getLabel() + " " + gain + "%");

		if(light instanceof RGBWInterface rgbw) {
			int white = rgbw.getWhite();
			labelWhite.setText(LABELS.getString("labelWhite") + ": " + white);
			sliderWhite.setValue(white);
			colorPanel(red, green, blue, white);
		} else {
			colorPanel(red, green, blue, 0);
		}

		sliderRed.addChangeListener(rgbSliderListener);
		sliderGreen.addChangeListener(rgbSliderListener);
		sliderBlue.addChangeListener(rgbSliderListener);
		if(light instanceof RGBWInterface) {
			sliderWhite.addChangeListener(whiteSliderListener);
		}
	}
	
	private void colorPanel(int red, int green, int blue, int white) {
		if(light instanceof RGBWInterface) {
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
		} else {
			previewColorPanel.setBackground(new Color(red, green, blue));
		}
	}
}