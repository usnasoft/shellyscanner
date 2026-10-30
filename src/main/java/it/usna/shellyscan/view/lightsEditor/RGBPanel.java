package it.usna.shellyscan.view.lightsEditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
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
 * RGB(W) Panel
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
		super(new BorderLayout(20, 0));
		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
		this.light = light;
		
		JPanel centerPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.VAlign.CENTER, VerticalFlowLayout.HAlign.CENTER, 0, 0));
		centerPanel.setOpaque(false);
		add(centerPanel, BorderLayout.CENTER);

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
		centerPanel.add(sliderGain);
		
		sliderRed = new SliderPanel(0, 255, () -> light.getRed(), "", LABELS.getString("labelRed"), val -> {
			try {
				light.setColor(val, light.getGreen(), light.getBlue());
			} catch(Exception e) {}
			adjust();
		}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(val, light.getGreen(), light.getBlue(), light instanceof RGBWInterface rgbw ? rgbw.getWhite() : -1)));
		centerPanel.add(sliderRed);
		
		sliderGreen = new SliderPanel(0, 255, () -> light.getGreen(), "", LABELS.getString("labelGreen"), val -> {
			try {
				light.setColor(light.getRed(), val, light.getBlue());
			} catch(Exception e) {}
			adjust();
		}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(light.getRed(), val, light.getBlue(), light instanceof RGBWInterface rgbw ? rgbw.getWhite() : -1)));
		centerPanel.add(sliderGreen);
		
		sliderBlue = new SliderPanel(0, 255, () -> light.getBlue(), "", LABELS.getString("labelBlue"), val -> {
			try {
				light.setColor(light.getRed(), light.getGreen(), val);
			} catch(Exception e) {}
			adjust();
		}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(light.getRed(), light.getGreen(), val, light instanceof RGBWInterface rgbw ? rgbw.getWhite() : -1)));
		centerPanel.add(sliderBlue);

		if(light instanceof RGBWInterface rgbw) {
			sliderWhite = new SliderPanel(0, 255, () -> rgbw.getWhite(), "", LABELS.getString("labelWhite"), val -> {
				try {
					rgbw.setWhite(val);
				} catch(Exception e) {}
				adjust();
			}, val -> previewColorPanel.setBackground(ColorUtil.rgbwColor(light.getRed(), light.getGreen(), light.getBlue(), val)));
			centerPanel.add(sliderWhite);
		}

		switchAction = new UsnaToggleAction(null, "/images/Standby24.png", "/images/StandbyOn24.png", e -> {
			try {
				light.toggle();
				adjust();
			} catch (IOException e1) {
				LOG.error("toggle", e1);
			}
		});
		
		JPanel switchButtonPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.VAlign.TOP, VerticalFlowLayout.HAlign.LEFT, 0, 0));
		JButton switchButton = new JButton(switchAction);
		switchButton.setContentAreaFilled(false);
		switchButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		switchButtonPanel.setOpaque(false);
		switchButtonPanel.add(switchButton);
		add(switchButtonPanel, BorderLayout.EAST);

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
		colorsPanel.add(colorButton(Color.BLUE));
		colorsPanel.add(colorButton(Color.YELLOW));
		colorsPanel.add(colorButton(new Color(255, 0, 255))); // violet
		colorsPanel.add(whiteButton);
		centerPanel.add(colorsPanel);

		previewColorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		previewColorPanel.setPreferredSize(new Dimension(190, 10));
		centerPanel.add(previewColorPanel);

		adjust();
	}
	
	private JButton colorButton(Color color) {
		JButton btn = new JButton();
		btn.addActionListener(e -> {
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
			previewColorPanel.setBackground(ColorUtil.rgbwColor(red, green, blue, rgbw.getWhite()));
			
			sliderWhite.adjust();
		} else {
			previewColorPanel.setBackground(new Color(red, green, blue));
		}

		sliderGain.adjust();
		sliderRed.adjust();
		sliderGreen.adjust();
		sliderBlue.adjust();
	}
}