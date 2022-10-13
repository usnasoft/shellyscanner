package it.usna.shellyscan.model.device.modules;

import it.usna.shellyscan.model.device.modules.RollerInterface;

public interface RollerCommander {
	public Roller getRoller(int index);
	
	default int getRollerCount() {
		return 1;
	}
}
