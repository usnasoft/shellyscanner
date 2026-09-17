package it.usna.shellyscan.model.device.modules;

public interface PresenceZoneInterface extends DeviceModule {
	int numObjects();
	
	@Override
	default String getLabel() {
		return "Presence: " + numObjects();
	}
}
