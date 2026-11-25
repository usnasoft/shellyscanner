package it.usna.shellyscan.model.device.modules;

import java.util.List;

/**
 * Manage the output state after a power on (power supply restored)
 */
public interface PowerOnStateManager {
	enum State {
		OFF,
		ON,
		RESTORE_LAST,
		MATCH_INPUT,	// relays only (G2+ "match_input", G1 "switch")
		MIX,			// different states on different channels
		NOT_APPLICABLE
	};
	
	/**
	 * @return the list (devices with multiple channels)
	 */
	List<State> getValues();

	/**
	 * @return in case of multiple channels with different values return MIX; the value instead
	 */
	State getVal();

	/**
	 * @return null if ok or error description in case of error
	 */
	String setState(State state);
}