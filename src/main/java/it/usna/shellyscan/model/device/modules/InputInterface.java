package it.usna.shellyscan.model.device.modules;

import java.io.IOException;
import java.util.Collection;

public interface InputInterface extends DeviceModule {

	boolean isInputOn();
	
	/* Actions/webhooks */
	
	int getRegisteredEventsCount();
	
	Collection<String> getRegisteredEvents();
	
	boolean enabled(); // is in use
	
	boolean enabled(String type);
	
	void execute(String type) throws IOException;
}
