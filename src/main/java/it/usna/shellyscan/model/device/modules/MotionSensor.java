package it.usna.shellyscan.model.device.modules;

public interface MotionSensor extends DeviceModule {
	boolean motion();
	
	default String getLabel() {
		return "Motion: " + motion();
	}
}
