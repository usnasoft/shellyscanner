package it.usna.shellyscan.view.lightsEditor;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.modules.WhiteInterface;

public class WhitePanel extends LightPanel {
	private static final Logger LOG = LoggerFactory.getLogger(WhitePanel.class);
	private static final long serialVersionUID = 1L;
	private final WhiteInterface light;
	private final UsnaToggleAction switchAction;
	private SliderPanel brightnessSlider;
	
	public WhitePanel(final WhiteInterface light) {
		super(new BorderLayout(20, 0));
		this.light = light;
		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
		
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
		add(switchButton, BorderLayout.EAST);

		add(brightnessSlider, BorderLayout.CENTER);
		adjust();
	}

	@Override
	public void change(boolean on) throws IOException {
		light.change(on);
		adjust();
	}

	private void adjust() {
		switchAction.setSelected(light.isOn());
		brightnessSlider.adjust();
	}
}