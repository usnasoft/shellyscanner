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
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.modules.CCTInterface;
import it.usna.shellyscan.view.util.ColorUtil;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.VerticalFlowLayout;

public class CCTPanel extends LightPanel {
	private static final Logger LOG = LoggerFactory.getLogger(CCTPanel.class);
	private static final long serialVersionUID = 1L;
	private final CCTInterface light;
	private final UsnaToggleAction switchAction;
	private final SliderPanel brightnessSlider;
	private final SliderPanel temperatureSlider;
	private final JPanel previewWhitePanel = new JPanel();

	public CCTPanel(final CCTInterface light) {
		super(new BorderLayout(20, 0));
		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
		this.light = light;
		
		JPanel centerPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.VAlign.CENTER, VerticalFlowLayout.HAlign.CENTER, 0, 0));
		centerPanel.setOpaque(false);
		add(centerPanel, BorderLayout.CENTER);

		brightnessSlider = new SliderPanel(light.getMinBrightness(), light.getMaxBrightness(), () -> light.getBrightness(), "%", () -> {
			String label = light.getLabel();
			return (label != null && label.isBlank() == false) ? label : Main.LABELS.getString("labelBrightness");
			}, val -> {
			try {
				light.setBrightness(val);
			} catch (IOException e1) {
				LOG.error("brightness", e1);
			}
			adjust();
		});
		centerPanel.add(brightnessSlider);
		
		temperatureSlider = new SliderPanel(light.getMinTemperature(), light.getMaxTemperature(), () -> light.getTemperature(), "K", () -> LABELS.getString("labelTemperature"), val -> {
			try {
				light.setTemperature(val);
			} catch (IOException e1) {
				LOG.error("brightness", e1);
			}
			adjust();
		}, val -> previewWhitePanel.setBackground(ColorUtil.kelvinToColor(val)));
		centerPanel.add(temperatureSlider);

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
		
		JPanel kbuttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		kbuttonsPanel.setOpaque(false);
		kbuttonsPanel.add(tempButton(3000));
		kbuttonsPanel.add(tempButton(4500));
		kbuttonsPanel.add(tempButton(6000));
		centerPanel.add(kbuttonsPanel);
		
		previewWhitePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		previewWhitePanel.setPreferredSize(new Dimension(190, 10));
		centerPanel.add(previewWhitePanel);
		
		adjust();
	}
	
	private JButton tempButton(int temp) {
		final JButton btn = new JButton(temp + "K");
		btn.setBackground(ColorUtil.kelvinToColor(temp));
		btn.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
		btn.addActionListener(e -> {
			try {
				light.setTemperature(UtilMiscellaneous.clamp(temp, light.getMinTemperature(), light.getMaxTemperature()));
			} catch (IOException ex) {
				LOG.error("tempButton set", ex);
			}
			adjust();
		});
		return btn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
		light.change(on);
		adjust();
	}

	void adjust() {
		switchAction.setSelected(light.isOn());
		previewWhitePanel.setBackground(ColorUtil.kelvinToColor(light.getTemperature()));

		brightnessSlider.adjust();
		temperatureSlider.adjust();
	}
}