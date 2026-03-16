package it.usna.shellyscan.model.device.modules;

public interface PresenceInterface extends DeviceModule {
	int numObjects();
	
	@Override
	default String getLabel() {
		return "Presence: " + numObjects();
	}
}
