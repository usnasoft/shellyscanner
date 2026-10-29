package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.ArrayList;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.PowerOnStateManager;
import tools.jackson.databind.JsonNode;

/**
 * "default_state" (output state after power on) for relays and lights; roller mode is not managed
 */
public class PowerOnStateManagerG1 implements PowerOnStateManager {
	private final AbstractG1Device parent;
	private State state = State.NOT_APPLICABLE;
	private final ArrayList<Channel> channels = new ArrayList<>();

	private record Channel(String endpoint, boolean isRelay) {}

	public PowerOnStateManagerG1(AbstractG1Device d) throws IOException {
		this(d, d.getJSON("/settings"));
	}

	public PowerOnStateManagerG1(AbstractG1Device d, JsonNode settings) {
		this.parent = d;
		final String mode = settings.path("mode").asString("");
		if(mode.equals("roller") == false) {
			JsonNode relays = settings.path("relays");
			for(int i = 0; i < relays.size(); i++) {
				add(relays.get(i), "/settings/relay/" + i, true);
			}
		}
		final String lightPath;
		if(settings.path("device").path("type").asString("").equals("SHRGBW2")) {
			lightPath = mode.equals("color") ? "/settings/color/" : "/settings/white/";
		} else {
			lightPath = "/settings/light/";
		}
		JsonNode lights = settings.path("lights");
		for(int i = 0; i < lights.size(); i++) {
			add(lights.get(i), lightPath + i, false);
		}
	}

	private void add(JsonNode channel, String endpoint, boolean isRelay) {
		JsonNode def = channel.path("default_state");
		if(def.isMissingNode() == false) {
			channels.add(new Channel(endpoint, isRelay));
			State current = toState(def.asString(""));
			if(state == State.NOT_APPLICABLE) {
				state = current;
			} else if(state != current) {
				state = State.MIX;
			}
		}
	}

	private static State toState(String val) {
		return switch(val) {
			case "off" -> State.OFF;
			case "on" -> State.ON;
			case "last" -> State.RESTORE_LAST;
			case "switch" -> State.MATCH_INPUT;
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
			case RESTORE_LAST -> "last";
			case MATCH_INPUT -> "switch";
			default -> throw new IllegalArgumentException(target.name());
		};
		String msg = null;
		boolean applied = false;
		for(Channel c: channels) {
			if(target == State.MATCH_INPUT && c.isRelay() == false) {
				continue; // "switch" is defined for relays only
			}
			applied = true;
			String localMsg = parent.sendCommand(c.endpoint() + "?default_state=" + val);
			if(localMsg != null) {
				msg = localMsg;
			}
		}
		return applied ? msg : "notApplicable";
	}
}
