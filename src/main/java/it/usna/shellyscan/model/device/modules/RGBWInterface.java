package it.usna.shellyscan.model.device.modules;

import java.io.IOException;

public interface RGBWInterface extends RGBInterface {

	int getWhite();

	void setWhite(int w) throws IOException;
	
	void setColor(int r, int g, int b, int w) throws IOException;
}
