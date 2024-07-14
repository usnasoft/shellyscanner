package it.usna.shellyscan.model.device.modules;

public interface MixedModuleHolder {
	DeviceModule getModule(int index);
	
	DeviceModule[] getModules();
	
	int getModuleCount();
}
