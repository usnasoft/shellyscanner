package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.InputManager;
import tools.jackson.databind.JsonNode;

/**
 * InputManager model implementation for G2+
 * 
 * Input mode:
 * Input mode for:
 * - "switch:N" components paired with "input:N" (same id) of type "button" or "switch";
 * - "light:N", "rgb:N", "rgbw:N" and "cct:N" components (dimmers); all the device inputs of type "button" or "switch"
 *   not paired with a switch are considered dimmer inputs.
 *   rgb/rgbw do not support "dual_dim": "dim" is used (cct is managed only if the device reports in_mode).
 * Covers are not managed (different in_mode semantics).
 * Channels in a device specific mode (e.g. "cycle", "activate") are never changed.
 */

// todo unificare "switches" (Map<Integer, String>) e "lights" (ArrayList<Dimmer>)
public class InputManagerG2 implements InputManager {
	private final AbstractG2Device parent;
	private ResetMode resetMode = ResetMode.NOT_APPLICABLE;
	private ArrayList<InputReset> inputResetList = new ArrayList<>();
	private record InputReset(ResetMode mode, int id) {}

	/** basic relay modes: any other in_mode (e.g. "cycle", "activate") is device specific and is kept */
	private static final Set<String> SWITCH_BASIC_MODES = Set.of("momentary", "follow", "flip", "detached");
	/** basic dimmer modes (light, rgb, rgbw, cct): any other in_mode (e.g. "activate") is device specific and is kept */
	private static final Set<String> LIGHT_BASIC_MODES = Set.of("dim", "dual_dim", "follow", "flip", "detached");

	private InputMode typeMode = InputMode.NOT_APPLICABLE;
//	private final Map<Integer, String> switches = new HashMap<>(); // id -> current in_mode
	private final ArrayList<Switch> switches = new ArrayList<>(); // id -> current in_mode
	private final ArrayList<Output> lights = new ArrayList<>();
	private final Map<Integer, String> inputTypes = new HashMap<>(); // id -> "button" | "switch"
//	private final ArrayList<Integer> lightInputIds = new ArrayList<>();

	/** dimmable components: component prefix and "SetConfig" method */
	private static final String[][] OUTPUTS_TYPES = {
			{"light:", "Light.SetConfig"},
			{"rgb:", "RGB.SetConfig"},
			{"rgbw:", "RGBW.SetConfig"},
			{"cct:", "CCT.SetConfig"},
			/*{"rgbcct:", "RGBCCT.SetConfig"}*/};

	private record Switch(int id, String inMode) {}
	private record Output(String method, int id, String inMode) {}

	public InputManagerG2(AbstractG2Device d) throws IOException {
		this(d, d.getJSON("/rpc/Shelly.GetConfig"));
	}

	/**
	 * Use this constructor if "settings" is known in order to decrease the device effort
	 * @param d the device
	 * @param settings JsonNode configuration node
	 */
	public InputManagerG2(AbstractG2Device d, JsonNode config) {
		this.parent = d;
		ResetMode currentMode;
		for(Entry<String, JsonNode> node: config.properties()) {
			String name = node.getKey();
			// factory reset by input & input types (button/switch)
			int id;
			if(name.startsWith("input:") && (id = Integer.parseInt(name.substring(6))) < 100) {
				JsonNode reset = node.getValue().path("factory_reset");
				if(reset.isMissingNode()) {
					currentMode = ResetMode.NOT_APPLICABLE;
				} else if(reset.booleanValue(false)) {
					currentMode = ResetMode.TRUE;
				} else {
					currentMode = ResetMode.FALSE;
				}
				inputResetList.add(new InputReset(currentMode, id));

				// global reset mode
				if(resetMode == ResetMode.NOT_APPLICABLE) {
					resetMode = currentMode;
				} else if(resetMode != currentMode) {
					resetMode = ResetMode.MIX;
				}
				
				// input types button/switch
				final String type = node.getValue().path("type").asString("");
				if(type.equals("button") || type.equals("switch")) {
					inputTypes.put(id, type);
				}
			}
			
			// input mode (momentary, follow, flip, ...)
			final JsonNode inMode = node.getValue().path("in_mode");
			if(inMode.isMissingNode() == false) {
				if(name.startsWith("switch:")) {
					id = parseId(name, 7);
					if(id >= 0 && inputTypes.containsKey(id)) {
						switches.add(new Switch(id, inMode.asString("")));
						merge(toInModeMode(inMode.asString("")));
					}
				} else {
					for(String[] dim: OUTPUTS_TYPES) {
						if(name.startsWith(dim[0])) {
							id = parseId(name, dim[0].length());
							if(id >= 0) {
								lights.add(new Output(dim[1], id, inMode.asString("")));
								merge(toInModeMode(inMode.asString("")));
							}
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public ResetMode getResetMode() {
		return resetMode;
	}
	
	@Override
	public List<ResetMode> getResetModes() {
		return inputResetList.stream().map(ir -> ir.mode).toList();
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
		String msg = null;
		for(InputReset ir: inputResetList) {
			if(ir.mode != ResetMode.NOT_APPLICABLE) {
				String localMsg = parent.postCommand("Input.SetConfig", "{\"id\":" + ir.id + ",\"config\":{\"factory_reset\":" + enable + "}");
				if(localMsg != null) {
					msg = localMsg;
				}
			} else {
				msg = "notApplicable";
			}
		}
		return msg;
	}

	//***** Input mode *****
	private static int parseId(String name, int prefixLength) {
		try {
			return Integer.parseInt(name.substring(prefixLength));
		} catch(NumberFormatException e) {
			return -1;
		}
	}

	private void merge(InputMode current) {
		// todo add mode to array
		if(typeMode == InputMode.NOT_APPLICABLE) {
			typeMode = current;
		} else if(typeMode != current) {
			typeMode = InputMode.MIX;
		}
	}

	private static InputMode toInModeMode(String inMode) {
		return switch(inMode) {
		case "momentary" -> InputMode.BUTTON; // "switch:" specific
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
		// relays
		final String switchInMode = switch(target) {
		case BUTTON -> "momentary";
		case SWITCH_FLIP -> "flip";
		case SWITCH_FOLLOW -> "follow";
		default -> "detached";
		};
		String msg = null;
		boolean applied = false;
		//for(Map.Entry<Integer, String> sw: switches.entrySet()) {
		for(Switch sw: switches) {
			if(SWITCH_BASIC_MODES.contains(sw.inMode) == false) {
				continue; // device specific mode (e.g. "cycle"): keep it
			}
			applied = true;
			final int id = sw.id;
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
//			for(int id: lightInputIds) {
			for(int id: inputTypes.keySet()) {
				String localMsg = setInputType(id, inputType);
				if(localMsg != null) {
					msg = localMsg;
					inputsOk = false;
				}
			}
			if(inputsOk) { // do not change in_mode if an input type change failed
				for(Output light: lights) {
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
		String ret = parent.postCommand("Input.SetConfig", "{\"id\":" + id + ",\"config\":{\"type\":\"" + type + "\"}}");
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		return ret;
	}
}