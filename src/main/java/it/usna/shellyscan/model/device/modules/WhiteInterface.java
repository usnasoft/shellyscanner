package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface WhiteInterface extends DeviceModule {
//	public final static int MIN_BRIGHTNESS = 0;
	public final static int MAX_BRIGHTNESS = 100;

	boolean isOn();

	int getBrightness();

	boolean toggle() throws IOException;

	public void change(boolean on) throws IOException;

	void setBrightness(int b) throws IOException;
}
