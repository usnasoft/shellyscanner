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
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.RGBWInterface;
import it.usna.shellyscan.view.util.ColorUtil;
import it.usna.swing.VerticalFlowLayout;

/**
 * RGB(W)Panel
 */
public class RGBPanel extends LightPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(RGBPanel.class);
	
	private final RGBInterface light;
	private final SliderPanel sliderGain;
	private final SliderPanel sliderRed;
	private final SliderPanel sliderGreen;
	private final SliderPanel sliderBlue;
	private SliderPanel sliderWhite;
	private final UsnaToggleAction switchAction;
	private final JPanel previewColorPanel = new JPanel();

	public RGBPanel(final RGBInterface light) {
		super(new VerticalFlowLayout(VerticalFlowLayout.VAlign.CENTER, VerticalFlowLayout.HAlign.CENTER, 0, 0));
		this.light = light;
		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));

		sliderGain = new SliderPanel(0, 100, () -> light.getGain(), "%", () -> {
			String label = light.getLabel();
			return (label != null && label.isBlank() == false) ? label : Main.LABELS.getString("labelBrightness");
			}, val -> {
			try {
				light.setGain(val);
			} catch (IOException e1) {
				LOG.error("brightness", e1);
			}
			adjust();
		});
		
		sliderRed = new SliderPanel(0, 255, () -> light.getRed(), "", LABELS.getString("labelRed"), val -> {
			try {
				light.setColor(val, light.getGreen(), light.getBlue());
			} catch(Exception e) {}
			adjust();
		}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(val, light.getGreen(), light.getBlue(), light instanceof RGBWInterface rgbw ? rgbw.getWhite() : -1)));
		
		sliderGreen = new SliderPanel(0, 255, () -> light.getGreen(), "", LABELS.getString("labelGreen"), val -> {
			try {
				light.setColor(light.getRed(), val, light.getBlue());
			} catch(Exception e) {}
			adjust();
		}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(light.getRed(), val, light.getBlue(), light instanceof RGBWInterface rgbw ? rgbw.getWhite() : -1)));
		
		sliderBlue = new SliderPanel(0, 255, () -> light.getBlue(), "", LABELS.getString("labelBlue"), val -> {
			try {
				light.setColor(light.getRed(), light.getGreen(), val);
			} catch(Exception e) {}
			adjust();
		}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(light.getRed(), light.getGreen(), val, light instanceof RGBWInterface rgbw ? rgbw.getWhite() : -1)));

		if(light instanceof RGBWInterface rgbw) {
			sliderWhite = new SliderPanel(0, 255, () -> rgbw.getWhite(), "", LABELS.getString("labelWhite"), val -> {
				try {
					rgbw.setWhite(val);
				} catch(Exception e) {}
				adjust();
			}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(light.getRed(), light.getGreen(), light.getBlue(), val)));
		}

		JPanel switchPanel = new JPanel(new BorderLayout(20, 0));
		switchPanel.setOpaque(false);
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
		switchButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		switchPanel.add(switchButton, BorderLayout.EAST);
		switchPanel.add(sliderGain, BorderLayout.CENTER);
		this.add(switchPanel);
		JPanel redPanel = new JPanel(new BorderLayout(20, 0));
		redPanel.setOpaque(false);
		redPanel.add(sliderRed, BorderLayout.CENTER);
		redPanel.add(Box.createHorizontalStrut(DialogEditLights.OFF_IMG.getIconWidth()), BorderLayout.EAST);
		this.add(redPanel);

		JPanel greenPanel = new JPanel(new BorderLayout(20, 0));
		greenPanel.setOpaque(false);
		greenPanel.add(sliderGreen, BorderLayout.CENTER);
		greenPanel.add(Box.createHorizontalStrut(DialogEditLights.OFF_IMG.getIconWidth()), BorderLayout.EAST);
		this.add(greenPanel);

		JPanel bluePanel = new JPanel(new BorderLayout(20, 0));
		bluePanel.setOpaque(false);
		bluePanel.add(sliderBlue, BorderLayout.CENTER);
		bluePanel.add(Box.createHorizontalStrut(DialogEditLights.OFF_IMG.getIconWidth()), BorderLayout.EAST);
		this.add(bluePanel);
		
		if(light instanceof RGBWInterface) {
			JPanel whitePanel = new JPanel(new BorderLayout(20, 0));
			whitePanel.setOpaque(false);
			whitePanel.add(sliderWhite, BorderLayout.CENTER);
			whitePanel.add(Box.createHorizontalStrut(DialogEditLights.OFF_IMG.getIconWidth()), BorderLayout.EAST);
			this.add(whitePanel);
		}

		JPanel colorsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		colorsPanel.setOpaque(false);
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

		colorsPanel.add(colorButton(Color.RED));
		colorsPanel.add(colorButton(Color.GREEN));
		colorsPanel.add(colorButton(Color.YELLOW));
		colorsPanel.add(colorButton(Color.BLUE));
		colorsPanel.add(colorButton(new Color(255, 0, 255))); // violet
		colorsPanel.add(whiteButton);
		this.add(colorsPanel);

		previewColorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		previewColorPanel.setPreferredSize(new Dimension(200, 10));
		this.add(previewColorPanel);

		adjust();
	}
	
	private JButton colorButton(Color color) {
		JButton btn = new JButton(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(light instanceof RGBWInterface rgbw) {
						rgbw.setColor(color.getRed(), color.getGreen(), color.getBlue(), 0);
					} else {
						light.setColor(color.getRed(), color.getGreen(), color.getBlue());
					}
					adjust();
				} catch (IOException e1) {
					LOG.error("color button", e1);
				}
			}
		});
		btn.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		btn.setBackground(color);
		return btn;
	}

	@Override
	public void change(boolean on) throws IOException {
		light.change(on);
		adjust();
	}

	void adjust() {
		switchAction.setSelected(light.isOn());
		int red = light.getRed();
		int green = light.getGreen();
		int blue = light.getBlue();

		if(light instanceof RGBWInterface rgbw) {
			int white = rgbw.getWhite();
			previewColorPanel.setBackground(ColorUtil.rgbwColor(red, green, blue, white));
			
			sliderWhite.adjust();
		} else {
			previewColorPanel.setBackground(ColorUtil.rgbwColor(red, green, blue, -1));
		}

		sliderGain.adjust();
		sliderRed.adjust();
		sliderGreen.adjust();
		sliderBlue.adjust();
	}
}
