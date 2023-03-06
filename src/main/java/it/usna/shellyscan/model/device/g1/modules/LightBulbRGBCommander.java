package it.usna.shellyscan.model.device.g1.modules;

public interface LightBulbRGBCommander {
	
	public LightBulbRGB getLight(int index);
	
	default int getLightCount() {
		return 1;
	}
}
