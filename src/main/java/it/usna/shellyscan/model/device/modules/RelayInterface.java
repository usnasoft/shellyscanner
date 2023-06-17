package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

/**
 * Used by 1, 1PM, EM, 2, 2.5, 1+, ...
 */
public interface RelayInterface extends DeviceModule {
	String getName();

	boolean toggle() throws IOException;
	
	void change(boolean on) throws IOException;
	
	boolean isOn();
	
	boolean isInputOn();
}
