package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.PowerOnStateManager;
import tools.jackson.databind.JsonNode;

/**
 * "initial_state" (output state after power on) for switch, light, rgb, rgbw and cct components
 */
public class PowerOnStateManagerG2 implements PowerOnStateManager {
	private static final String[][] COMPONENTS = {
			{"switch:", "Switch.SetConfig"},
			{"light:", "Light.SetConfig"},
			{"rgb:", "RGB.SetConfig"},
			{"rgbw:", "RGBW.SetConfig"},
			{"cct:", "CCT.SetConfig"}};

	private final AbstractG2Device parent;
	private State state = State.NOT_APPLICABLE;
	private final ArrayList<Channel> channels = new ArrayList<>();

	private record Channel(String method, int id, boolean isSwitch) {}

	public PowerOnStateManagerG2(AbstractG2Device d) throws IOException {
		this(d, d.getJSON("/rpc/Shelly.GetConfig"));
	}

	public PowerOnStateManagerG2(AbstractG2Device d, JsonNode config) {
		this.parent = d;
		for(Entry<String, JsonNode> node: config.properties()) {
			final String name = node.getKey();
			for(String[] comp: COMPONENTS) {
				if(name.startsWith(comp[0])) {
					final JsonNode initial = node.getValue().path("initial_state");
					if(initial.isMissingNode() == false) {
						try {
							final int id = Integer.parseInt(name.substring(comp[0].length()));
							channels.add(new Channel(comp[1], id, comp[0].equals("switch:")));
							State current = toState(initial.asString(""));
							if(state == State.NOT_APPLICABLE) {
								state = current;
							} else if(state != current) {
								state = State.MIX;
							}
						} catch(NumberFormatException e) {}
					}
					break;
				}
			}
		}
	}

	private static State toState(String val) {
		return switch(val) {
			case "off" -> State.OFF;
			case "on" -> State.ON;
			case "restore_last" -> State.RESTORE_LAST;
			case "match_input" -> State.MATCH_INPUT;
			default -> State.MIX; // unknown value: no global selection
		};
	}

	@Override
	public State getVal() {
		return state;
	}

	@Override
	public String setState(State target) {
		if(state == State.NOT_APPLICABLE || channels.isEmpty()) {
			return "notApplicable";
		}
		final String val = switch(target) {
			case OFF -> "off";
			case ON -> "on";
			case RESTORE_LAST -> "restore_last";
			case MATCH_INPUT -> "match_input";
			default -> throw new IllegalArgumentException(target.name());
		};
		String msg = null;
		boolean applied = false;
		for(Channel c: channels) {
			if(target == State.MATCH_INPUT && c.isSwitch() == false) {
				continue; // "match_input" is defined for switches only
			}
			applied = true;
			String localMsg = parent.postCommand(c.method(), "{\"id\":" + c.id() + ",\"config\":{\"initial_state\":\"" + val + "\"}}");
			if(localMsg != null) {
				msg = localMsg;
			}
		}
		return applied ? msg : "notApplicable";
	}
}
