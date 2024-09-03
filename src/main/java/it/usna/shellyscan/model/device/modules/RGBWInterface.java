package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface RGBWInterface extends DeviceModule {
	boolean toggle() throws IOException;
	
	void change(boolean on) throws IOException;
	
	boolean isOn();
	
	int getWhite();
	
	int getRed();
	
	int getGreen();
	
	int getBlue();
	
	void setWhite(int w) throws IOException; // todo remove?
	
	void setColor(int r, int g, int b, int w) throws IOException;
	
	void setGain(int b) throws IOException;
	
	int getGain();
}
