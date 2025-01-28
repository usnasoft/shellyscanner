package it.usna.shellyscan.view.lightsEditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.modules.CCTInterface;
import it.usna.shellyscan.view.util.Kelvin2RGB;
import it.usna.swing.VerticalFlowLayout;

public class CCTPanel extends LightPanel {
	private final static Logger LOG = LoggerFactory.getLogger(CCTPanel.class);
	private static final long serialVersionUID = 1L;
	private final CCTInterface light;
	private final JLabel labelBrighteness = new JLabel();
	private final UsnaToggleAction switchAction;
	private final JSlider brightnessSlider;
	private final JLabel labelTemperature = new JLabel();
	private final JSlider temperatureSlider;
	private final JPanel previewWhitePanel = new JPanel();
	
	public CCTPanel(final CCTInterface light) {
		this.light = light;
		setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
		setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));
		// set initial values to avoid listeners to call the device in initial adjust
		brightnessSlider = new JSlider(light.getMinBrightness(), light.getMaxBrightness(), light.getBrightness());
		temperatureSlider = new JSlider(light.getMinTemperature(), light.getMaxTemperature(), light.getTemperature());

		JPanel switchPanel = new JPanel(new BorderLayout(10, 0));
		switchPanel.setOpaque(false);
		switchPanel.add(labelBrighteness, BorderLayout.NORTH);
		switchAction = new UsnaToggleAction(this, "/images/Standby24.png", "/images/StandbyOn24.png", e -> {
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
		switchPanel.add(brightnessSlider, BorderLayout.CENTER);
		brightnessSlider.addChangeListener(e -> {
			if(brightnessSlider.getValueIsAdjusting() == false) {
				try {
					light.setBrightness(brightnessSlider.getValue());
					adjust();
				} catch (IOException e1) {
					LOG.error("sliderGain", e1);
				}
			} else {
				labelBrighteness.setText(light.getLabel() + " " + brightnessSlider.getValue() + "%");
			}
		});
		this.add(switchPanel);

		JPanel temperaturePanel = new JPanel(new BorderLayout(10, 0));
		temperaturePanel.setOpaque(false);
		temperaturePanel.add(labelTemperature, BorderLayout.NORTH);
		temperaturePanel.add(temperatureSlider, BorderLayout.CENTER);
		temperaturePanel.add(Box.createHorizontalStrut(DialogEditLights.offImg.getIconWidth()), BorderLayout.EAST);
		temperatureSlider.addChangeListener(e -> {
			if(temperatureSlider.getValueIsAdjusting() == false) {
				try {
					light.setTemperature(temperatureSlider.getValue());
					adjust();
				} catch (IOException e1) {
					LOG.error("sliderTemp", e1);
				}
			} else {
				labelTemperature.setText(LABELS.getString("labelTemperature") + ": " + temperatureSlider.getValue() + "K");
				previewWhitePanel.setBackground(Kelvin2RGB.kelvinToColor(temperatureSlider.getValue()));
			}
		});
		this.add(temperaturePanel);
		
		JPanel kbuttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		kbuttonsPanel.setOpaque(false);
		final JButton k3000 = new JButton("3000K");
		final JButton k4500 = new JButton("4500K");
		final JButton k6000 = new JButton("6000K");
		k3000.setBackground(Kelvin2RGB.kelvinToColor(3000));
		k4500.setBackground(Kelvin2RGB.kelvinToColor(4500));
		k6000.setBackground(Kelvin2RGB.kelvinToColor(6000));
		k3000.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
		k4500.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
		k6000.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
		k3000.addActionListener(e -> {
			temperatureSlider.setValue(3000); // slider listener will call the device
			adjust();
		});
		k4500.addActionListener(e -> {
			temperatureSlider.setValue(4500); // slider listener will call the device
			adjust();
		});
		k6000.addActionListener(e -> {
			temperatureSlider.setValue(6000); // slider listener will call the device
			adjust();
		});
		kbuttonsPanel.add(k3000);
		kbuttonsPanel.add(k4500);
		kbuttonsPanel.add(k6000);
		this.add(kbuttonsPanel);
		
		previewWhitePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		previewWhitePanel.setPreferredSize(new Dimension(200, 10));
		this.add(previewWhitePanel);
		
		adjust();
	}
	
	@Override
	public void change(boolean on) throws IOException {
		light.change(on);
		adjust();
	}

	private void adjust() {
		switchAction.setSelected(light.isOn());
		labelBrighteness.setText(light.getLabel() + " " + light.getBrightness() + "%");
		brightnessSlider.setValue(light.getBrightness());
		labelTemperature.setText(LABELS.getString("labelTemperature") + ": " + light.getTemperature() + "K");
		temperatureSlider.setValue(light.getTemperature());
		previewWhitePanel.setBackground(Kelvin2RGB.kelvinToColor(light.getTemperature()));
	}
}