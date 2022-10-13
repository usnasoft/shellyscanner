package it.usna.shellyscan.model.device.g1.modules;

public interface LightBulbRGBCommander {
	
	public LightBulbRGB getLight(int index);
	
//	public void statusRefresh() throws IOException;
	
	default int getLightCount() {
		return 1;
	}
}
