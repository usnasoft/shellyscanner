package it.usna.shellyscan.model.device.modules;

import it.usna.shellyscan.model.device.g1.modules.LightWhite;

public interface WhiteCommander {
	
	LightWhite getWhite(int index);
	
	LightWhite[] getWhites();
	
	int getWhiteCount();
}
