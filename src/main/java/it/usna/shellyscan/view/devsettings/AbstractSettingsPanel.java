package it.usna.shellyscan.view.devsettings;

import javax.swing.JPanel;

public abstract class AbstractSettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	protected DialogDeviceSettingsMain parentDlg;
	
	protected AbstractSettingsPanel(DialogDeviceSettingsMain parent) {
		this.parentDlg = parent;
	}
	
	abstract String showing() throws InterruptedException;
	
	void hiding() {}

	abstract String apply();
}