package it.usna.shellyscan.model.device.modules;

public interface InputCommander {
	
	InputInterface getActionsGroup(int index);
	
	InputInterface[] getActionsGroups();
	
	default int getGroupsCount() {
		return 1;
	}
}
