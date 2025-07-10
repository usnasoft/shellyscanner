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
		
		final G2SchedulerPanel schPanel = (device == null) ? new G2SchedulerPanel(this) : new G2SchedulerPanel(this, device);
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		final WDThermSchedulerPanel thermPanel = new WDThermSchedulerPanel(this, device);
		
		JTabbedPane tabs = new JTabbedPane();
		getContentPane().add(tabs, BorderLayout.CENTER);

		tabs.add(LABELS.getString("schLblJobs"), schPanel);
		tabs.add(LABELS.getString("schLblProTherm"), thermPanel);
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(new JButton(new UsnaAction(this, "dlgApply", e -> {
			Component current = tabs.getSelectedComponent();
			if(current == schPanel) {
				schPanel.apply();
			} else {
				thermPanel.apply();
			}
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction(this, "dlgApplyClose", e -> {
			Component current = tabs.getSelectedComponent();
			boolean done = (current == schPanel) ? schPanel.apply() : thermPanel.apply();
			if(done) dispose();
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction(this, "labelRefresh", e -> {
			schPanel.refresh();
			try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException ex) {}
			thermPanel.refresh();
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction("lblLoadFile", e -> {
			Component current = tabs.getSelectedComponent();
			if(current == schPanel) {
				schPanel.loadFromBackup();
			} else {
				thermPanel.loadFromBackup();
			}
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction("dlgClose", e -> dispose() )));
		
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		pack();
		setSize(getWidth(), 500);
	}
	
	static void lineColors(JPanel container) {
		Component[] list = container.getComponents();
		for(int i = 0; i < list.length; i++) {
			list[i].setBackground((i % 2 == 1) ? Main.TAB_LINE2_COLOR : Main.TAB_LINE1_COLOR);
		}
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

// todo warning on profile remove (if rules exist)