package it.usna.shellyscan.model.device.modules;

/**
 * Manage the input mode of relay and dimmer outputs (button / switch / detached)
 */
public interface InputModeManager {
	enum Mode {
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

	Mode getVal();

	/**
	 * @return null if ok or error description in case of error
	 */
	String setMode(Mode mode);
}
