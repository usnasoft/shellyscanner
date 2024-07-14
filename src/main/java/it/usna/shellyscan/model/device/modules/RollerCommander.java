package it.usna.shellyscan.model.device.modules;

public interface RollerCommander {
	RollerInterface getRoller(int index);
	
	default int getRollersCount() {
		return 1;
	}
}
