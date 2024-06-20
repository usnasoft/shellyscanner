package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface WhiteInterface extends DeviceModule {
	boolean isOn();

	int getBrightness();
	
	int getMinBrightness();
	
	int getMaxBrightness();

	boolean toggle() throws IOException;

	public void change(boolean on) throws IOException;

	void setBrightness(int b) throws IOException;
	
	boolean isInputOn(); // input:0 switch
}
