package it.usna.shellyscan.model.device.modules;

import it.usna.shellyscan.model.device.g1.modules.LightWhite;

public interface WhiteCommander {
	
	public LightWhite getWhite(int index);
	
	public LightWhite[] getWhites();
	
	public int getWhiteCount();
}
