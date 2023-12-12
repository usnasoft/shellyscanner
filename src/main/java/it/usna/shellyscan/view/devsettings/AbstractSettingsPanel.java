package it.usna.shellyscan.view.devsettings;

import javax.swing.JPanel;

public abstract class AbstractSettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	protected DialogDeviceSettings parent;
	
	protected AbstractSettingsPanel(DialogDeviceSettings parent) {
		this.parent = parent;
	}
	
	abstract String showing() throws InterruptedException;
	
	void hiding() {}

	abstract String apply();
}