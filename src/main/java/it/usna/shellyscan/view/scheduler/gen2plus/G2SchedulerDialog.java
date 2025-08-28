package it.usna.shellyscan.view.scheduler.gen2plus;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.UsnaSwingUtils;

public class G2SchedulerDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public G2SchedulerDialog(Window owner, AbstractG2Device device) {
		super(owner, Main.LABELS.getString("schTitle") + " - " + UtilMiscellaneous.getExtendedHostName(device), Dialog.ModalityType.MODELESS);
		init(device);
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	/** test & design */
	public G2SchedulerDialog() {
		init(null);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void init(AbstractG2Device device) {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		final G2SchedulerPanel scPanel = device == null ? new G2SchedulerPanel(this) : new G2SchedulerPanel(this, device);
		getContentPane().add(scPanel, BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		buttonsPanel.add(new JButton(new UsnaAction(this, "dlgApply", e -> scPanel.apply() )));
		buttonsPanel.add( new JButton(new UsnaAction(this, "dlgApplyClose", e -> {
			if(scPanel.apply()) dispose();
		}) ));
		buttonsPanel.add(new JButton(new UsnaAction(this, "labelRefresh", e -> scPanel.refresh() )));
		buttonsPanel.add(new JButton(new UsnaAction("lblLoadFile", e -> scPanel.loadFromBackup() )));
		buttonsPanel.add(new JButton(new UsnaAction("dlgClose", e -> dispose() )));

		pack();
		setSize(getWidth(), 512);
	}

	public static void main(final String ... args) throws Exception {
		UsnaSwingUtils.setLookAndFeel(UsnaSwingUtils.LF_NIMBUS);
		new G2SchedulerDialog();
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