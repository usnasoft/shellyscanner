package it.usna.shellyscan.model.device.modules;

public interface DWInterface extends DeviceModule {
	boolean open();
	
	@Override
	default String getLabel() {
		return "Open: " + open();
	}
}
