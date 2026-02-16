package it.usna.shellyscan.model.device.modules;

public interface FloodInterface extends DeviceModule {
	boolean flood();
	
	@Override
	default String getLabel() {
		return null;
	}
}
