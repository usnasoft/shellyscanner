package it.usna.shellyscan.model.device.modules;

public interface SmokeInterface extends DeviceModule {
	boolean smoke();
	
	@Override
	default String getLabel() {
		return "Smoke: " + smoke();
	}
}
