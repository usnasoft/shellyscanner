package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface CBreakerInterface extends DeviceModule {
	
	boolean toggle() throws IOException;

	void change(boolean on) throws IOException;
	
	boolean isOn();
	
	boolean isLocked();
}
