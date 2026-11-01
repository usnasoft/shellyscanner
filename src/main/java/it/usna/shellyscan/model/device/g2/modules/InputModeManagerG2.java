package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.InputModeManager;
import tools.jackson.databind.JsonNode;

/**
 * Input mode for:
 * - "switch:N" components paired with "input:N" (same id) of type "button" or "switch";
 * - "light:N", "rgb:N", "rgbw:N" and "cct:N" components (dimmers); all the device inputs of type "button" or "switch"
 *   not paired with a switch are considered dimmer inputs.
 *   rgb/rgbw do not support "dual_dim": "dim" is used (cct is managed only if the device reports in_mode).
 * Covers are not managed (different in_mode semantics).
 * Channels in a device specific mode (e.g. "cycle", "activate") are never changed.
 */
public class InputModeManagerG2 implements InputModeManager {
	/** basic relay modes: any other in_mode (e.g. "cycle", "activate") is device specific and is kept */
	private static final Set<String> SWITCH_BASIC_MODES = Set.of("momentary", "follow", "flip", "detached");
	/** basic dimmer modes (light, rgb, rgbw, cct): any other in_mode (e.g. "activate") is device specific and is kept */
	private static final Set<String> LIGHT_BASIC_MODES = Set.of("dim", "dual_dim", "follow", "flip", "detached");

	private final AbstractG2Device parent;
	private InputMode typeMode = InputMode.NOT_APPLICABLE;
	private final Map<Integer, String> switches = new HashMap<>(); // id -> current in_mode
	private final ArrayList<Dimmer> lights = new ArrayList<>();
	private final Map<Integer, String> inputTypes = new HashMap<>(); // id -> "button" | "switch"
	private final ArrayList<Integer> lightInputIds = new ArrayList<>();
	
	/** dimmable components: component prefix and "SetConfig" method */
	private static final String[][] DIMMERS = {
			{"light:", "Light.SetConfig"},
			{"rgb:", "RGB.SetConfig"},
			{"rgbw:", "RGBW.SetConfig"},
			{"cct:", "CCT.SetConfig"}};
	
	private record Dimmer(String method, int id, String inMode) {}

	public InputModeManagerG2(AbstractG2Device d) throws IOException {
		this(d, d.getJSON("/rpc/Shelly.GetConfig"));
	}

	public InputModeManagerG2(AbstractG2Device d, JsonNode config) {
		this.parent = d;
		for(Entry<String, JsonNode> node: config.properties()) {
			final String name = node.getKey();
			if(name.startsWith("input:")) {
				final String type = node.getValue().path("type").asString("");
				final int id = parseId(name, 6);
				if(id >= 0 && id < 100 && (type.equals("button") || type.equals("switch"))) {
					inputTypes.put(id, type);
				}
			}
		}
		for(Entry<String, JsonNode> node: config.properties()) {
			final String name = node.getKey();
			final JsonNode inMode = node.getValue().path("in_mode");
			if(inMode.isMissingNode()) {
				continue;
			}
			if(name.startsWith("switch:")) {
				final int id = parseId(name, 7);
				if(id >= 0 && inputTypes.containsKey(id)) {
					switches.put(id, inMode.asString(""));
					merge(toSwitchMode(inputTypes.get(id), inMode.asString("")));
				}
			} else {
				for(String[] dim: DIMMERS) {
					if(name.startsWith(dim[0])) {
						final int id = parseId(name, dim[0].length());
						if(id >= 0) {
							lights.add(new Dimmer(dim[1], id, inMode.asString("")));
							merge(toLightMode(inMode.asString("")));
						}
						break;
					}
				}
			}
		}
		if(lights.size() > 0) {
			for(Integer inId: inputTypes.keySet()) {
				if(switches.containsKey(inId) == false) {
					lightInputIds.add(inId);
				}
			}
		}
	}

	private static int parseId(String name, int prefixLength) {
		try {
			return Integer.parseInt(name.substring(prefixLength));
		} catch(NumberFormatException e) {
			return -1;
		}
	}

	private void merge(InputMode current) {
		if(typeMode == InputMode.NOT_APPLICABLE) {
			typeMode = current;
		} else if(typeMode != current) {
			typeMode = InputMode.MIX;
		}
	}

	private static InputMode toSwitchMode(String inputType, String inMode) {
		if(inMode.equals("detached")) {
			return InputMode.DETACHED;
		} else if(inputType.equals("button") && inMode.equals("momentary")) {
			return InputMode.BUTTON;
		} else if(inputType.equals("switch") && inMode.equals("flip")) {
			return InputMode.SWITCH_FLIP;
		} else if(inputType.equals("switch") && inMode.equals("follow")) {
			return InputMode.SWITCH_FOLLOW;
		} else {
			return InputMode.OTHER;
		}
	}

	private static InputMode toLightMode(String inMode) {
		return switch(inMode) {
			case "dim", "dual_dim" -> InputMode.BUTTON; // one or two buttons: not managed here
			case "flip" -> InputMode.SWITCH_FLIP;
			case "follow" -> InputMode.SWITCH_FOLLOW;
			case "detached" -> InputMode.DETACHED;
			default -> InputMode.OTHER; // "activate", ...
		};
	}

	@Override
	public InputMode getInputMode() {
		return typeMode;
	}

	/**
	 * @return null if ok, "notApplicable" if nothing to change (no relay/dimmer or only device specific modes), error description otherwise
	 */
	@Override
	public String setInputMode(InputMode target) {
		if(typeMode == InputMode.NOT_APPLICABLE) {
			return "notApplicable";
		}
		final String inputType = switch(target) {
			case BUTTON -> "button";
			case SWITCH_FLIP, SWITCH_FOLLOW -> "switch";
			case DETACHED -> null; // keep current input type
			default -> throw new IllegalArgumentException(target.name());
		};
		String msg = null;
		boolean applied = false;

		// relays
		final String switchInMode = switch(target) {
			case BUTTON -> "momentary";
			case SWITCH_FLIP -> "flip";
			case SWITCH_FOLLOW -> "follow";
			default -> "detached";
		};
		for(Map.Entry<Integer, String> sw: switches.entrySet()) {
			if(SWITCH_BASIC_MODES.contains(sw.getValue()) == false) {
				continue; // device specific mode (e.g. "cycle"): keep it
			}
			applied = true;
			final int id = sw.getKey();
			String localMsg = setInputType(id, inputType);
			if(localMsg == null) { // do not change in_mode if input type change failed
				localMsg = parent.postCommand("Switch.SetConfig", "{\"id\":" + id + ",\"config\":{\"in_mode\":\"" + switchInMode + "\"}}");
			}
			if(localMsg != null) {
				msg = localMsg;
			}
		}

		// dimmers - inputs could be shared by channels: if any channel is in a device specific mode the dimmer is not changed
		if(lights.size() > 0 && lights.stream().allMatch(l -> LIGHT_BASIC_MODES.contains(l.inMode()))) {
			applied = true;
			boolean inputsOk = true;
			for(int id: lightInputIds) {
				String localMsg = setInputType(id, inputType);
				if(localMsg != null) {
					msg = localMsg;
					inputsOk = false;
				}
			}
			if(inputsOk) { // do not change in_mode if an input type change failed
				for(Dimmer light: lights) {
					final String current = light.inMode();
					final String lightInMode = switch(target) {
						// keep one/two buttons mode if already a button mode, otherwise one button (rgb, rgbw: "dim" only)
						case BUTTON -> (current.equals("dim") || current.equals("dual_dim")) ? current : "dim";
						case SWITCH_FLIP -> "flip";
						case SWITCH_FOLLOW -> "follow";
						default -> "detached";
					};
					String localMsg = parent.postCommand(light.method(), "{\"id\":" + light.id() + ",\"config\":{\"in_mode\":\"" + lightInMode + "\"}}");
					if(localMsg != null) {
						msg = localMsg;
					}
				}
			}
		}
		return applied ? msg : "notApplicable";
	}

	/** change the input type only if different from the current one */
	private String setInputType(int id, String type) {
		if(type == null || type.equals(inputTypes.get(id))) {
			return null;
		}
		return parent.postCommand("Input.SetConfig", "{\"id\":" + id + ",\"config\":{\"type\":\"" + type + "\"}}");
	}
}