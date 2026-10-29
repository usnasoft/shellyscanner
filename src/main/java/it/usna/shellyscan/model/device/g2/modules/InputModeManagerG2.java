package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.InputModeManager;
import tools.jackson.databind.JsonNode;

/**
 * Input mode for:
 * - "switch:N" components paired with "input:N" (same id) of type "button" or "switch";
 * - "light:N" components (dimmers); all the device inputs of type "button" or "switch"
 *   not paired with a switch are considered dimmer inputs.
 * Covers are not managed (different in_mode semantics).
 */
public class InputModeManagerG2 implements InputModeManager {
	private final AbstractG2Device parent;
	private Mode mode = Mode.NOT_APPLICABLE;
	private final ArrayList<Integer> switchIds = new ArrayList<>();
	private final Map<Integer, String> lights = new HashMap<>(); // id -> current in_mode
	private final Map<Integer, String> inputTypes = new HashMap<>(); // id -> "button" | "switch"
	private final ArrayList<Integer> lightInputIds = new ArrayList<>();

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
					switchIds.add(id);
					merge(toSwitchMode(inputTypes.get(id), inMode.asString("")));
				}
			} else if(name.startsWith("light:")) {
				final int id = parseId(name, 6);
				if(id >= 0) {
					lights.put(id, inMode.asString(""));
					merge(toLightMode(inMode.asString("")));
				}
			}
		}
		if(lights.size() > 0) {
			for(Integer inId: inputTypes.keySet()) {
				if(switchIds.contains(inId) == false) {
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

	private void merge(Mode current) {
		if(mode == Mode.NOT_APPLICABLE) {
			mode = current;
		} else if(mode != current) {
			mode = Mode.MIX;
		}
	}

	private static Mode toSwitchMode(String inputType, String inMode) {
		if(inMode.equals("detached")) {
			return Mode.DETACHED;
		} else if(inputType.equals("button") && inMode.equals("momentary")) {
			return Mode.BUTTON;
		} else if(inputType.equals("switch") && inMode.equals("flip")) {
			return Mode.SWITCH_FLIP;
		} else if(inputType.equals("switch") && inMode.equals("follow")) {
			return Mode.SWITCH_FOLLOW;
		} else {
			return Mode.OTHER;
		}
	}

	private static Mode toLightMode(String inMode) {
		return switch(inMode) {
			case "dim", "dual_dim" -> Mode.BUTTON; // one or two buttons: not managed here
			case "flip" -> Mode.SWITCH_FLIP;
			case "follow" -> Mode.SWITCH_FOLLOW;
			case "detached" -> Mode.DETACHED;
			default -> Mode.OTHER; // "activate", ...
		};
	}

	@Override
	public Mode getVal() {
		return mode;
	}

	@Override
	public String setMode(Mode target) {
		if(mode == Mode.NOT_APPLICABLE) {
			return "notApplicable";
		}
		final String inputType = switch(target) {
			case BUTTON -> "button";
			case SWITCH_FLIP, SWITCH_FOLLOW -> "switch";
			case DETACHED -> null; // keep current input type
			default -> throw new IllegalArgumentException(target.name());
		};
		String msg = null;

		// relays
		final String switchInMode = switch(target) {
			case BUTTON -> "momentary";
			case SWITCH_FLIP -> "flip";
			case SWITCH_FOLLOW -> "follow";
			default -> "detached";
		};
		for(int id: switchIds) {
			String localMsg = setInputType(id, inputType);
			if(localMsg == null) { // do not change in_mode if input type change failed
				localMsg = parent.postCommand("Switch.SetConfig", "{\"id\":" + id + ",\"config\":{\"in_mode\":\"" + switchInMode + "\"}}");
			}
			if(localMsg != null) {
				msg = localMsg;
			}
		}

		// dimmers
		if(lights.size() > 0) {
			boolean inputsOk = true;
			for(int id: lightInputIds) {
				String localMsg = setInputType(id, inputType);
				if(localMsg != null) {
					msg = localMsg;
					inputsOk = false;
				}
			}
			if(inputsOk) { // do not change in_mode if an input type change failed
				for(Map.Entry<Integer, String> light: lights.entrySet()) {
					final String current = light.getValue();
					final String lightInMode = switch(target) {
						// keep one/two buttons mode if already a button mode, otherwise one button
						case BUTTON -> (current.equals("dim") || current.equals("dual_dim")) ? current : "dim";
						case SWITCH_FLIP -> "flip";
						case SWITCH_FOLLOW -> "follow";
						default -> "detached";
					};
					String localMsg = parent.postCommand("Light.SetConfig", "{\"id\":" + light.getKey() + ",\"config\":{\"in_mode\":\"" + lightInMode + "\"}}");
					if(localMsg != null) {
						msg = localMsg;
					}
				}
			}
		}
		return msg;
	}

	/** change the input type only if different from the current one */
	private String setInputType(int id, String type) {
		if(type == null || type.equals(inputTypes.get(id))) {
			return null;
		}
		return parent.postCommand("Input.SetConfig", "{\"id\":" + id + ",\"config\":{\"type\":\"" + type + "\"}}");
	}
}
