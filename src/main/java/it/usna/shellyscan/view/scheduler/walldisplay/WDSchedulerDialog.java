package it.usna.shellyscan.view.scheduler.walldisplay;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.WallDisplay;
import it.usna.shellyscan.view.scheduler.gen2plus.G2SchedulerPanel;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.UsnaSwingUtils;

public class WDSchedulerDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public WDSchedulerDialog(Window owner, WallDisplay device) {
		super(owner, Main.LABELS.getString("schTitle") + " - " + UtilMiscellaneous.getExtendedHostName(device), Dialog.ModalityType.MODELESS);
		init(device);
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	/** test & design */
	public WDSchedulerDialog() {
		init(null);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void init(WallDisplay device) {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		final G2SchedulerPanel scPanel = device == null ? new G2SchedulerPanel() : new G2SchedulerPanel(this, device);
		
		JTabbedPane tabs = new JTabbedPane();
		getContentPane().add(tabs, BorderLayout.CENTER);

		tabs.add(LABELS.getString("schLblJobs"), scPanel);
		tabs.add(LABELS.getString("schLblProTherm"), new ProfilesPanel(this, device));
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(new JButton(new UsnaAction("dlgApply", e -> scPanel.apply())));
		buttonsPanel.add( new JButton(new UsnaAction("dlgApplyClose", e -> {
			if(scPanel.apply()) dispose();
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction("lblLoadFile", e -> scPanel.loadFromBackup())));
		buttonsPanel.add(new JButton(new UsnaAction("dlgClose", e -> dispose())));
		
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		pack();
		setSize(getWidth(), 500);
	}
	
	public static void main(final String ... args) throws Exception {
		UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NIMBUS);
		new WDSchedulerDialog();
	}
}

// https://next-api-docs.shelly.cloud/gen2/ComponentsAndServices/Schedule
// https://github.com/mongoose-os-libs/cron
// https://crontab.guru/
// https://regex101.com/
// https://www.freeformatter.com/regex-tester.html

// http://<ip>/rpc/Schedule.DeleteAll
// http://<ip>/rpc/Schedule.Create?timespec="0 0 22 * * FRI"&calls=[{"method":"Shelly.GetDeviceInfo"}]
// http://<ip>/rpc/Schedule.Create?timespec="10/100 * * * * *"&calls=[{"method":"light.toggle?id=0"}]

// notes: 10 not working (do 0); 100 not working (do 60)