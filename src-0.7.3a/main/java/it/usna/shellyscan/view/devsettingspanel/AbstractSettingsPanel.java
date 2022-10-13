package it.usna.shellyscan.view.devsettingspanel;

import static it.usna.shellyscan.Main.LABELS;

import java.util.List;

import javax.swing.JPanel;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public abstract class AbstractSettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	protected final List<ShellyAbstractDevice> devices;
	
	protected AbstractSettingsPanel(List<ShellyAbstractDevice> devices) {
		this.devices = devices;
	}
	
	public abstract String showing();

	public abstract String apply();
	
	protected String getExtendedName(ShellyAbstractDevice d) {
		final String dName = d.getName();
		return d.getHostname() + " - " + (dName.length() > 0 ? dName : LABELS.getString("dlgSetUnknownName"));
	}
}
