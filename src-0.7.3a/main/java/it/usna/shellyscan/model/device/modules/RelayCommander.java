package it.usna.shellyscan.model.device.modules;

public interface RelayCommander {
	public RelayInterface getRelay(int index);
	
	public RelayInterface[] getRelays();
	
	default int getRelayCount() {
		return 1;
	}
}
