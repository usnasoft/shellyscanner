package it.usna.shellyscan.model.device.g1.modules;

public interface DeviceModule {
	public String getLabel();
	default String getLastSource() {return null;}
}
