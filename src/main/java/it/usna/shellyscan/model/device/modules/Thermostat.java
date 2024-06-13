package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface Thermostat extends DeviceModule {
	float getTargetTemp();
	
	boolean isEnabled();
	
	void setTargetTemp(float temp) throws IOException;
}
