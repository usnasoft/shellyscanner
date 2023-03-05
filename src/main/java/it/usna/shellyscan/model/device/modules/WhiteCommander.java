package it.usna.shellyscan.model.device.modules;

public interface WhiteCommander {
	
	LightWhiteInterface getWhite(int index);
	
	LightWhiteInterface[] getWhites();
	
	int getWhiteCount();
}
