package it.usna.shellyscan.view.lightsEditor;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.device.modules.WhiteInterface;

public class WhitePanel extends LightPanel {
	private final static Logger LOG = LoggerFactory.getLogger(WhitePanel.class);
	private static final long serialVersionUID = 1L;
	private final WhiteInterface light;
	private final JLabel label = new JLabel();
	private final UsnaToggleAction switchAction;
	private final JSlider brightnessSlider;
	private final ChangeListener brightenessSliderListener;
	
	public WhitePanel(final WhiteInterface light) {
		this.light = light;
		setBorder(BorderFactory.createEmptyBorder(4, 8, 10, 8));

		setLayout(new BorderLayout(10, 0));
		brightnessSlider = new JSlider(light.getMinBrightness(), light.getMaxBrightness(), light.getBrightness());
		add(label, BorderLayout.NORTH);
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
		add(switchButton, BorderLayout.EAST);
		brightenessSliderListener = e -> {
			if(brightnessSlider.getValueIsAdjusting() == false) {
				try {
					light.setBrightness(brightnessSlider.getValue());
				} catch (IOException e1) {
					LOG.error("brightness", e1);
				}
				adjust();
			} else {
				label.setText(light.getLabel() + " " + brightnessSlider.getValue() + "%");
			}
		};
		add(brightnessSlider, BorderLayout.CENTER);
		adjust();
	}
	
	@Override
	public void change(boolean on) throws IOException {
		light.change(on);
		adjust();
	}

	private void adjust() {
		brightnessSlider.removeChangeListener(brightenessSliderListener);
		
		switchAction.setSelected(light.isOn());
		label.setText(light.getLabel() + " " + light.getBrightness() + "%");
		brightnessSlider.setValue(light.getBrightness());
		
		brightnessSlider.addChangeListener(brightenessSliderListener);
	}
}