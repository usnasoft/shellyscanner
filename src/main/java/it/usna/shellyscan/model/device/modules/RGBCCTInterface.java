package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface RGBCCTInterface extends CCTInterface, RGBInterface{
	boolean isColorMode();
	
	void setColorMode(boolean color) throws IOException;
}
