package it.usna.shellyscan.model.device.modules;

public interface DeviceModule {
	String getLabel();
	
	default String getLastSource() { return null; }
}
