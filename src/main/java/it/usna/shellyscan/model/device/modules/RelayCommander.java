package it.usna.shellyscan.model.device.modules;

public interface RelayCommander {
	RelayInterface getRelay(int index);
	
	RelayInterface[] getRelays();
	
	default int getRelayCount() {
		return 1;
	}
}
