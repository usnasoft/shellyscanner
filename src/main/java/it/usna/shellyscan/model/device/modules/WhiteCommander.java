package it.usna.shellyscan.model.device.modules;

public interface WhiteCommander {
	
	WhiteInterface getWhite(int index);
	
	WhiteInterface[] getWhites();
	
	int getWhitesCount();
}
