package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public interface WhiteInterface extends DeviceModule {
	boolean isOn();

	int getBrightness();
	
	int getMinBrightness();
	
	default int getMaxBrightness() {
		return 100;
	}

	boolean toggle() throws IOException;

	public void change(boolean on) throws IOException;

	void setBrightness(int b) throws IOException;
	
	boolean isInputOn(); // input:0 switch
	
	ShellyAbstractDevice getParent();
}
