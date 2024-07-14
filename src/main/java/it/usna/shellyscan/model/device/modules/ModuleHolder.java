package it.usna.shellyscan.model.device.modules;

public interface ModuleHolder {
	
	DeviceModule getModule(int index);
	
	DeviceModule[] getModules();

	default int getModulesCount() {
		return 1;
	}
}
