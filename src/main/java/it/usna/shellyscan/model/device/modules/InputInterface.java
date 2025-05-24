package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface InputInterface extends DeviceModule {
	boolean isInputOn();
	
	boolean enabled(); // is in use
	
	int getRegisteredEventsCount();
	
	String getEvent(int eventIndex);
	
	boolean enabled(int eventIndex);
	
	void execute(int eventIndex) throws IOException;
}