package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

/**
 * Used by 2; 2.5; +2PM
 */
public interface RollerInterface extends DeviceModule {
	boolean isCalibrated();
	
	int getPosition();
	
	void setPosition(int pos) throws IOException;
	
	void open() throws IOException;
	
	void close() throws IOException;
	
	void stop() throws IOException;
}