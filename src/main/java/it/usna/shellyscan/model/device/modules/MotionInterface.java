package it.usna.shellyscan.model.device.modules;

public interface MotionInterface extends DeviceModule {
	boolean motion();
	
	default String getLabel() {
		return "Motion: " + motion();
	}
}
