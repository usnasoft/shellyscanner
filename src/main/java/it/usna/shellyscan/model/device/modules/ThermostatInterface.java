package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface ThermostatInterface {
	
	float getTargetTemp();
	
	void setTargetTemp(float temp) throws IOException;
}
