package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.InputManager;
import tools.jackson.databind.JsonNode;

/**
 * InputManager model implementation for G1
 * 
 * Input mode:
 * "btn_type" for relays (momentary / edge / toggle / detached)
 * and dimmers (one_button / dual_button / edge / toggle / detached); roller mode is not managed.
 * Channels in a device specific mode (e.g. "action", "cycle") are never changed.
 */
public class InputManagerG1 implements InputManager {
	private final AbstractG1Device parent;
	private ResetMode resetMode;
	
	/** basic modes: any other btn_type (e.g. "action", "cycle", "momentary_on_release") is device specific and is kept */
	private static final Set<String> RELAY_BASIC_MODES = Set.of("momentary", "edge", "toggle", "detached");
	private static final Set<String> LIGHT_BASIC_MODES = Set.of("one_button", "dual_button", "edge", "toggle", "detached");
	
	private InputMode typeMode = InputMode.NOT_APPLICABLE;
	private final ArrayList<Channel> channels = new ArrayList<>();

	private record Channel(String endpoint, boolean isLight, String btnType) {}
	
	public InputManagerG1(AbstractG1Device d) throws IOException {
		this(d, d.getJSON("/settings"));
	}

	/**
	 *  use this constructor if "settings" is known in order to decrease the device effort
	 * @param d the device
	 * @param settings JsonNode configuration node
	 */
	public InputManagerG1(AbstractG1Device d, JsonNode config) {
		this.parent = d;
		JsonNode reset = config.path("factory_reset_from_switch");
		if(reset.isMissingNode()) {
			resetMode = ResetMode.NOT_APPLICABLE;
		} else if(reset.booleanValue(false)) {
			resetMode = ResetMode.TRUE;
		} else {
			resetMode = ResetMode.FALSE;
		}
		
		if(config.path("mode").asString("").equals("roller") == false) {
			JsonNode relays = config.path("relays");
			for(int i = 0; i < relays.size(); i++) {
				JsonNode btnType = relays.get(i).path("btn_type");
				if(btnType.isMissingNode() == false) {
					channels.add(new Channel("/settings/relay/" + i, false, btnType.asString("")));
					merge(toRelayMode(btnType.asString("")));
				}
			}
		}
		JsonNode lights = config.path("lights");
		for(int i = 0; i < lights.size(); i++) {
			JsonNode btnType = lights.get(i).path("btn_type");
			if(btnType.isMissingNode() == false) {
				channels.add(new Channel("/settings/light/" + i, true, btnType.asString("")));
				merge(toLightMode(btnType.asString("")));
			}
		}
	}
	
	@Override
	public ResetMode getResetMode() {
		return resetMode;
	}

	@Override
	public Boolean getResetModeAsBoolean() {
		if(resetMode == ResetMode.TRUE) {
			return true;
		} else if(resetMode == ResetMode.FALSE) {
			return false;
		} else {
			return null;
		}
	}
	
	@Override
	public String enableReset(boolean enable) {
		if(resetMode == ResetMode.NOT_APPLICABLE) {
			return "notApplicable";
		} else {
			return parent.sendCommand("/settings?factory_reset_from_switch=" + enable);
		}
	}
	
	//***** Input mode
	private void merge(InputMode current) {
		if(typeMode == InputMode.NOT_APPLICABLE) {
			typeMode = current;
		} else if(typeMode != current) {
			typeMode = InputMode.MIX;
		}
	}

	private static InputMode toRelayMode(String btnType) {
		return switch(btnType) {
			case "momentary" -> InputMode.BUTTON;
			case "edge" -> InputMode.SWITCH_FLIP;
			case "toggle" -> InputMode.SWITCH_FOLLOW;
			case "detached" -> InputMode.DETACHED;
			default -> InputMode.OTHER; // "action", "cycle", "momentary_on_release", ...
		};
	}

	private static InputMode toLightMode(String btnType) {
		return switch(btnType) {
			case "one_button", "dual_button" -> InputMode.BUTTON; // one or two buttons: not managed here
			case "edge" -> InputMode.SWITCH_FLIP;
			case "toggle" -> InputMode.SWITCH_FOLLOW;
			case "detached" -> InputMode.DETACHED;
			default -> InputMode.OTHER; // "action", ...
		};
	}

	@Override
	public InputMode getInputMode() {
		return typeMode;
	}

	@Override
	public String setInputMode(InputMode target) {
		if(typeMode == InputMode.NOT_APPLICABLE || channels.isEmpty()) {
			return "notApplicable";
		}
		final String relayType = switch(target) {
			case BUTTON -> "momentary";
			case SWITCH_FLIP -> "edge";
			case SWITCH_FOLLOW -> "toggle";
			case DETACHED -> "detached";
			default -> throw new IllegalArgumentException(target.name());
		};
		String msg = null;
		boolean applied = false;
		for(Channel c: channels) {
			if((c.isLight() ? LIGHT_BASIC_MODES : RELAY_BASIC_MODES).contains(c.btnType()) == false) {
				continue; // device specific mode: keep it
			}
			applied = true;
			final String btnType;
			if(c.isLight()) {
				btnType = switch(target) {
					// keep one/two buttons mode if already a button mode, otherwise one button
					case BUTTON -> (c.btnType().equals("one_button") || c.btnType().equals("dual_button")) ? c.btnType() : "one_button";
					case SWITCH_FLIP -> "edge";
					case SWITCH_FOLLOW -> "toggle";
					default -> "detached";
				};
			} else {
				btnType = relayType;
			}
			String localMsg = parent.sendCommand(c.endpoint() + "?btn_type=" + btnType);
			if(localMsg != null) {
				msg = localMsg;
			}
		}
		return applied ? msg : "notApplicable";
	}
}