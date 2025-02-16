package it.usna.shellyscan.model.device;

import it.usna.shellyscan.model.device.modules.DeviceModule;

public interface ModulesHolder {
	
//	DeviceModule getModule(int index);
	
	DeviceModule[] getModules();

	default int getModulesCount() {
		return 1;
	}
}
