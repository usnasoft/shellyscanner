package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.g2.AbstractBatteryG2Device;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.CheckList;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;

public class DialogDeviceScripts extends JDialog {
	private final static Logger LOG = LoggerFactory.getLogger(CheckList.class);
	private static final long serialVersionUID = 1L;
	public final static String FILE_EXTENSION = "js";
	private JPanel kvsPanel;

	public DialogDeviceScripts(final Frame owner, Devices model, int modelIndex) {
		super(owner, false);
		init(owner, model, modelIndex);
	}
	
	public DialogDeviceScripts(final JDialog owner, Devices model, int modelIndex) {
		super(owner, false);
		init(owner, model, modelIndex);
	}
		
	private void init(final Window owner, Devices model, int modelIndex) {
		AbstractG2Device device = (AbstractG2Device) model.get(modelIndex);
		setTitle(String.format(LABELS.getString("dlgScriptTitle"), UtilMiscellaneous.getExtendedHostName(device)));
		setDefaultCloseOperation(/* DO_NOTHING_ON_CLOSE */DISPOSE_ON_CLOSE);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton jButtonClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		buttonsPanel.add(jButtonClose);

		JTabbedPane tabs = new JTabbedPane();

		// battery operated devices do not support scripts
		if (device instanceof AbstractBatteryG2Device == false) {
			try {
				JPanel scriptsPanel = new ScriptsPanel(this, model, modelIndex);
				tabs.addTab(LABELS.getString("lblScriptsTab"), scriptsPanel);
				try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
			} catch (/*IO*/Exception e) { // XT1 (and ...?) do not support scripts
//				Msg.errorMsg(owner, e);
				LOG.debug("ScriptsPanel", e);
			}
		}

		try {
			kvsPanel = new KVSPanel(device);
			tabs.addTab(LABELS.getString("lblKVSTab"), kvsPanel);
		} catch (/*IO*/Exception e) {
			//Msg.errorMsg(owner, e);
			LOG.debug("KVSPanel", e);
		}

		if(tabs.getComponentCount() > 0) {
			getContentPane().add(tabs, BorderLayout.CENTER);
			setSize(550, 360);
			setLocationRelativeTo(owner);
			setVisible(true);
		} else {
			Msg.errorMsg(owner, LABELS.getString(device.getStatus() == Status.OFF_LINE ? "Status-OFFLINE" : "msgScriptKVSNotSupported"));
			dispose();
		}
	}
	
	@Override
	public void dispose() {
		if(kvsPanel != null) {
			kvsPanel.setVisible(false);
		}
		super.dispose();
		firePropertyChange("S_CLOSE", null, null);
	}
}