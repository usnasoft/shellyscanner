package it.usna.shellyscan.view.lightsEditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.modules.CCTInterface;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.RGBCCTInterface;
import it.usna.shellyscan.model.device.modules.RGBInterface;
import it.usna.shellyscan.model.device.modules.WhiteInterface;
import it.usna.swing.VerticalFlowLayout;

public class DialogEditLights extends JDialog {
	private static final long serialVersionUID = 1L;
	private final ArrayList<LightPanel> commandPanels = new ArrayList<>(5);
	static final ImageIcon OFF_IMG = new ImageIcon(DialogEditLights.class.getResource("/images/Standby24.png"));
	private static final ImageIcon ON_IMG = new ImageIcon(DialogEditLights.class.getResource("/images/StandbyOn24.png"));
	private static final Logger LOG = LoggerFactory.getLogger(DialogEditLights.class);

	public DialogEditLights(final Window owner, DeviceModule[] lights) {
		super(owner, lights.length == 1 ? lights[0].getLabel() : LABELS.getString("dlgLightsEditorTitle"), Dialog.ModalityType.MODELESS);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel commandStackedPanel = commandPanel(lights);
		getContentPane().add(commandStackedPanel, BorderLayout.CENTER);
		if(commandPanels.size() > 1) {
			getContentPane().add(northPanel(lights), BorderLayout.NORTH);
		}
		
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape_close");
		rootPane.getActionMap().put("escape_close", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	private JPanel northPanel(DeviceModule[] lights) {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panel.setBackground(Color.LIGHT_GRAY);
		JButton offButton = new JButton(OFF_IMG);
		JButton onButton = new JButton(ON_IMG);
		offButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		onButton.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		offButton.setContentAreaFilled(false);
		onButton.setContentAreaFilled(false);
		
		JLabel lblNewLabel = new JLabel(LABELS.getString("labelSwitchAllChannels"));
		panel.add(lblNewLabel);
		panel.add(offButton);
		panel.add(onButton);
		offButton.addActionListener(e -> switchAll(lights, false));
		onButton.addActionListener(e -> switchAll(lights, true));
		return panel;
	}
	
	private void switchAll(DeviceModule[] lights, boolean on) {
		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			for(LightPanel panel: commandPanels) {
				panel.change(on);
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
		} catch (IOException | InterruptedException e1) {
			LOG.error("switchAll", e1);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}
	
	private JPanel commandPanel(DeviceModule[] lights) {
		JPanel stackedPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, VerticalFlowLayout.LEFT, 0, 0));
		for(int i = 0; i < lights.length; i++) {
			if(lights[i] instanceof RGBCCTInterface rgbcct) { // rgbcct extends rgb & cct
				commandPanels.add(new RGBCCTPanel(rgbcct));
			} else if(lights[i] instanceof RGBInterface rgb) { // rgbw extends rgb
				commandPanels.add(new RGBPanel(rgb));
			} else if(lights[i] instanceof CCTInterface cct) {
				commandPanels.add(new CCTPanel(cct));
			} else if(lights[i] instanceof WhiteInterface w) {
				commandPanels.add(new WhitePanel(w));
			}
		}
		for(int i = 0; i < commandPanels.size(); i++) {
			stackedPanel.add(commandPanels.get(i));
			commandPanels.get(i).setBackground(i % 2 == 0 ? Main.TAB_LINE1_COLOR : Main.TAB_LINE2_COLOR);
		}
		return stackedPanel;
	}
}

abstract class LightPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public abstract void change(boolean on) throws IOException;
}