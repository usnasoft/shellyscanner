package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractBatteryG2Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;

public class DialogDeviceScriptsG2 extends JDialog {
	private static final long serialVersionUID = 1L;
	public final static String FILE_EXTENSION = "js";

	public DialogDeviceScriptsG2(final MainView owner, Devices model, int modelIndex) {
		super(owner, false);
		try {
			AbstractG2Device device = (AbstractG2Device) model.get(modelIndex);
			setTitle(String.format(LABELS.getString("dlgScriptTitle"), UtilMiscellaneous.getExtendedHostName(device)));
			setDefaultCloseOperation(/*DO_NOTHING_ON_CLOSE*/DISPOSE_ON_CLOSE);
			
			JPanel buttonsPanel = new JPanel();
			getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

			JButton jButtonClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
			buttonsPanel.add(jButtonClose);
			
			JTabbedPane tabs = new JTabbedPane();

			// battery operated devices do not support scripts
			if(device instanceof AbstractBatteryG2Device == false) {
				JPanel scriptsPanel = new ScriptsPanel(this, model, modelIndex);
				tabs.addTab(LABELS.getString("lblScriptsTab"), scriptsPanel);
				try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
			}
			
			JPanel kvsPanel = new KVSPanel(device);
			tabs.addTab(LABELS.getString("lblKVSTab"), kvsPanel);

			getContentPane().add(tabs, BorderLayout.CENTER);

			setSize(600, 360);
			setLocationRelativeTo(owner);
			setVisible(true);
		} catch (IOException e) {
			Msg.errorMsg(e);
		}
	}
}