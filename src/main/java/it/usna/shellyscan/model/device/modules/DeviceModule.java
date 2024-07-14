package it.usna.shellyscan.model.device.modules;

public interface DeviceModule /*extends LabelHolder*/ {
	String getLabel();
	
	default String getLastSource() { return null; }
}
