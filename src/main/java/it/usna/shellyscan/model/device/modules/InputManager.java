package it.usna.shellyscan.model.device.modules;

import java.util.List;

/**
 * Inputs configurations interface.
 * Currently manages "factory_reset_from_switch" / "factory_reset"
 * and
 * input modes (excluding some device specific modes) 
 */
public interface InputManager {
	// Reset
	enum ResetMode {TRUE, FALSE, NOT_APPLICABLE, MIX};
	
	/**
	 * @return in case of multiple channels with different values return MIX; the value instead
	 */
	ResetMode getResetMode();
	
	/**
	 * @return the list (devices with multiple channels; gen1 devices always have a single value)
	 */
	List<ResetMode> getResetModes();
	
	
	Boolean getResetModeAsBoolean();
	
	/**
	 * @return null if ok or error description in case of error
	 */
	String enableReset(boolean enable);
	
	// Input mode
	enum InputMode {
		BUTTON,			// relay G2+: input "button" + in_mode "momentary"; G1: btn_type "momentary"
						// dimmer G2+: input "button" + in_mode "dim" or "dual_dim" (kept if already set);
						//        G1: btn_type "one_button" or "dual_button" (kept if already set)
		SWITCH_FLIP,	// G2+: input "switch" + in_mode "flip";   G1: btn_type "edge"
		SWITCH_FOLLOW,	// G2+: input "switch" + in_mode "follow"; G1: btn_type "toggle"
		DETACHED,		// G2+: in_mode "detached";                G1: btn_type "detached"
		OTHER,			// any other mode (e.g. "activate", "cycle", "action", ...)
		MIX,			// different modes on different channels
		NOT_APPLICABLE
	};

	InputMode getInputMode();
	
//	List<InputMode> getInputModes();

	/**
	 * @return null if ok or error description in case of error
	 */
	String setInputMode(InputMode mode);
}