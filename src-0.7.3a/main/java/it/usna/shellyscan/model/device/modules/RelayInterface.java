package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Used by 1, 1PM, EM, 2, 2.5, 1+
 */
public interface RelayInterface extends DeviceModule {
	public String getName();

	public boolean toggle() throws IOException;
	
	public void change(boolean on) throws IOException;
	
	public boolean isOn();
	
	public String getLastSource();
}
