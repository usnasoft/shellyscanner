package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface RGBInterface extends DeviceModule {
	boolean toggle() throws IOException;
	
	void change(boolean on) throws IOException;
	
	boolean isOn();
	
	int getWhite();
	
	int getRed();
	
	int getGreen();
	
	int getBlue();
	
	void setColor(int r, int g, int b) throws IOException;
	
	void setGain(int b) throws IOException;
	
	int getGain();
}
