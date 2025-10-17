package it.usna.shellyscan.model.device.modules;

public interface MotionInterface extends DeviceModule {
	boolean motion();
	
	@Override
	default String getLabel() {
		return "Motion: " + motion();
	}
}
