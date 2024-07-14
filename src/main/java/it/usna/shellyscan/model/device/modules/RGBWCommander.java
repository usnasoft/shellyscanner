package it.usna.shellyscan.model.device.modules;

import it.usna.shellyscan.model.device.g1.modules.LightRGBW;

public interface RGBWCommander {
	
	LightRGBW getColor(int index);
	
	int getColorsCount();
}
