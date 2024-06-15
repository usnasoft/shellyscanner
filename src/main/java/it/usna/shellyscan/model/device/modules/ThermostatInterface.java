package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface ThermostatInterface extends DeviceModule {
	boolean isEnabled();
	
	boolean isRunning();
	
	void setEnabled(boolean enabled) throws IOException;
	
	float getTargetTemp();

	void setTargetTemp(float temp) throws IOException;
	
	float getMaxTargetTemp();
	
	float getMinTargetTemp();
}
