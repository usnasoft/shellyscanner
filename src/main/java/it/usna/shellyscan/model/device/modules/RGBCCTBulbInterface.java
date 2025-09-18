package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface RGBCCTBulbInterface extends CCTInterface, RGBInterface{
	boolean isColorMode();
	
	void setColorMode(boolean color) throws IOException;
}
