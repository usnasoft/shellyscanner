package it.usna.shellyscan.view.scheduler.walldisplay;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
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
		
		final G2SchedulerPanel schPanel = device == null ? new G2SchedulerPanel(this) : new G2SchedulerPanel(this, device);
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
//		final ProfilesPanel thermoPanel = new ProfilesPanel(this, device);
		final WDThermSchedulerPanel thermoPanel = new WDThermSchedulerPanel(this, device);
		
		JTabbedPane tabs = new JTabbedPane();
		getContentPane().add(tabs, BorderLayout.CENTER);

		tabs.add(LABELS.getString("schLblJobs"), schPanel);
		tabs.add(LABELS.getString("schLblProTherm"), thermoPanel);
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(new JButton(new UsnaAction("dlgApply", e -> {
			Component current = tabs.getSelectedComponent();
			if(current == schPanel) {
				schPanel.apply();
			} else {
				thermoPanel.apply();
			}
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction("dlgApplyClose", e -> {
			Component current = tabs.getSelectedComponent();
			boolean done = (current == schPanel) ? schPanel.apply() : thermoPanel.apply();
			if(done) dispose();
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction(this, "labelRefresh", e -> {
			schPanel.refresh();
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException ex) {}
			thermoPanel.refresh();
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction("lblLoadFile", e -> {
			Component current = tabs.getSelectedComponent();
			if(current == schPanel) {
				schPanel.loadFromBackup();
			} else {
				thermoPanel.loadFromBackup();
			}
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction("dlgClose", e -> dispose() )));
		
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