package it.usna.shellyscan.view.devsettings;

import java.util.List;

import javax.swing.JPanel;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public abstract class AbstractSettingsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	protected final List<ShellyAbstractDevice> devices;
	
	protected AbstractSettingsPanel(List<ShellyAbstractDevice> devices) {
		this.devices = devices;
	}
	
	protected int getIndex(ShellyAbstractDevice device) {
		for(int i = 0; i < devices.size(); i++) {
			if(devices.get(i) == device) {
				return i;
			}
		}
		return -1;
	}
	
	public abstract String showing() throws InterruptedException;
	
	public void hiding() {}

	public abstract String apply();
	
//	public static String getExtendedName(ShellyAbstractDevice d) {
//		final String dName = d.getName();
//		return d.getHostname() + " - " + (dName.length() > 0 ? dName : d.getTypeName());
//	}
}