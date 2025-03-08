package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface CCTInterface extends WhiteInterface {
	void setTemperature(int ct) throws IOException;
	
	int getTemperature();
	
	default int getMinTemperature() {
		return 2700;
	}
	
	default int getMaxTemperature() {
		return 6500;
	}
}
