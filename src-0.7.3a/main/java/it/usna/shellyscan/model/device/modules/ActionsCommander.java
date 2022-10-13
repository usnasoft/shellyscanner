package it.usna.shellyscan.model.device.modules;

import it.usna.shellyscan.model.device.g1.modules.Actions;

public interface ActionsCommander {
	
	public Actions.Input getActionsGroup(int index);
	
	public Actions.Input[] getActionsGroups();
	
	default int getGroupsCount() {
		return 1;
	}
}
