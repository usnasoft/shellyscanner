package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface ThermostatInterface extends DeviceModule {
	boolean isEnabled();
	
	float getTargetTemp();

	void setTargetTemp(float temp) throws IOException;
}
