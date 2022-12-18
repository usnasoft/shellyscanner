package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g1.modules.LightWhite;

public class DialogEditLightsArray extends JDialog {
	private static final long serialVersionUID = 1L;
	private JLabel labels[];
	private JToggleButton buttons[];
	private JSlider sliders[];
	private final static Logger LOG = LoggerFactory.getLogger(DialogEditLightsArray.class);

	public DialogEditLightsArray(final Window owner, LightWhite[] lights) {
		super(owner, LABELS.getString("dlgELATitle"), Dialog.ModalityType.MODELESS);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 5));
		
		labels = new JLabel[lights.length];
		buttons = new JToggleButton[lights.length];
		sliders = new JSlider[lights.length];

		getContentPane().add(northPanel(lights), BorderLayout.NORTH);
		getContentPane().add(commandPanel(lights), BorderLayout.CENTER);
		
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	private JPanel northPanel(LightWhite[] lights) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.setBackground(Color.LIGHT_GRAY);
		JButton offButton = new JButton(new ImageIcon(DialogEditLightsArray.class.getResource("/images/Standby24.png"))/*, light.isOn()*/);
		JButton onButton = new JButton(new ImageIcon(DialogEditLightsArray.class.getResource("/images/StandbyOn24.png")));
		offButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		onButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		offButton.setContentAreaFilled(false);
		onButton.setContentAreaFilled(false);
		panel.add(offButton);
		panel.add(onButton);
		offButton.addActionListener(e -> {
			try {
				for(LightWhite l: lights) {
					l.change(false);
					adjust(lights);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			} catch (IOException | InterruptedException e1) {
				LOG.error("off", e1);
			}
		});
		onButton.addActionListener(e -> {
			try {
				for(LightWhite l: lights) {
					l.change(true);
					adjust(lights);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			} catch (IOException | InterruptedException e1) {
				LOG.error("on", e1);
			}
		});
		return panel;
	}
	
	private JPanel commandPanel(LightWhite[] lights) {
		JPanel stackedPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		BoxLayout stackedPanelLO = new BoxLayout(stackedPanel, BoxLayout.Y_AXIS);
		stackedPanel.setLayout(stackedPanelLO);
		stackedPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 10, 8));

		for(int i = 0; i < lights.length; i++) {
			final LightWhite light = lights[i];
			JPanel lp = new JPanel(new BorderLayout(10, 0));
			JLabel label = new JLabel();
			labels[i] = label;
			lp.add(label, BorderLayout.NORTH);
			JToggleButton switchButton = new JToggleButton(new ImageIcon(DialogEditRGB.class.getResource("/images/Standby24.png")));
			buttons[i] = switchButton;
			switchButton.setSelectedIcon(new ImageIcon(DialogEditRGB.class.getResource("/images/StandbyOn24.png")));
			switchButton.setRolloverIcon(new ImageIcon(DialogEditRGB.class.getResource("/images/Standby24.png")));
			switchButton.setRolloverSelectedIcon(new ImageIcon(DialogEditRGB.class.getResource("/images/StandbyOn24.png")));
			switchButton.setContentAreaFilled(false);
			switchButton.setBorder(BorderFactory.createEmptyBorder());
			switchButton.addActionListener(e -> {
				try {
					light.toggle();
					adjust(lights);
				} catch (IOException e1) {
					LOG.error("toggle", e1);
				}
			});
			lp.add(switchButton, BorderLayout.EAST);
			JSlider brightness = new JSlider(/*LightWhite.MIN_BRIGHTNESS*/0, LightWhite.MAX_BRIGHTNESS, light.getBrightness());
			sliders[i] = brightness;
			brightness.addChangeListener(e -> {
				if(brightness.getValueIsAdjusting() == false) {
					try {
						light.setBrightness(brightness.getValue());
						adjust(lights);
					} catch (IOException e1) {
						LOG.error("brightness", e1);
					}
				} else {
					label.setText(light.getLabel() + " " + brightness.getValue() + "%");
				}
			});
			lp.add(brightness, BorderLayout.CENTER);
			stackedPanel.add(lp);
		}
		adjust(lights);
		return stackedPanel;
	}
	
	private void adjust(LightWhite[] lights) {
		for(int i = 0; i < lights.length; i++) {
			final LightWhite light = lights[i];
			labels[i].setText(light.getLabel() + " " + light.getBrightness() + "%");
			buttons[i].setSelected(light.isOn());
			sliders[i].setValue(light.getBrightness());
		}
	}
}