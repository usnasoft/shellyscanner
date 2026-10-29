package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.ArrayList;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.InputModeManager;
import tools.jackson.databind.JsonNode;

/**
 * "btn_type" for relays (momentary / edge / toggle / detached)
 * and dimmers (one_button / dual_button / edge / toggle / detached); roller mode is not managed
 */
public class InputModeManagerG1 implements InputModeManager {
	private final AbstractG1Device parent;
	private Mode mode = Mode.NOT_APPLICABLE;
	private final ArrayList<Channel> channels = new ArrayList<>();

	private record Channel(String endpoint, boolean isLight, String btnType) {}

	public InputModeManagerG1(AbstractG1Device d) throws IOException {
		this(d, d.getJSON("/settings"));
	}

	public InputModeManagerG1(AbstractG1Device d, JsonNode settings) {
		this.parent = d;
		if(settings.path("mode").asString("").equals("roller") == false) {
			JsonNode relays = settings.path("relays");
			for(int i = 0; i < relays.size(); i++) {
				JsonNode btnType = relays.get(i).path("btn_type");
				if(btnType.isMissingNode() == false) {
					channels.add(new Channel("/settings/relay/" + i, false, btnType.asString("")));
					merge(toRelayMode(btnType.asString("")));
				}
			}
		}
		JsonNode lights = settings.path("lights");
		for(int i = 0; i < lights.size(); i++) {
			JsonNode btnType = lights.get(i).path("btn_type");
			if(btnType.isMissingNode() == false) {
				channels.add(new Channel("/settings/light/" + i, true, btnType.asString("")));
				merge(toLightMode(btnType.asString("")));
			}
		}
	}

	private void merge(Mode current) {
		if(mode == Mode.NOT_APPLICABLE) {
			mode = current;
		} else if(mode != current) {
			mode = Mode.MIX;
		}
	}

	private static Mode toRelayMode(String btnType) {
		return switch(btnType) {
			case "momentary" -> Mode.BUTTON;
			case "edge" -> Mode.SWITCH_FLIP;
			case "toggle" -> Mode.SWITCH_FOLLOW;
			case "detached" -> Mode.DETACHED;
			default -> Mode.OTHER; // "action", "cycle", "momentary_on_release", ...
		};
	}

	private static Mode toLightMode(String btnType) {
		return switch(btnType) {
			case "one_button", "dual_button" -> Mode.BUTTON; // one or two buttons: not managed here
			case "edge" -> Mode.SWITCH_FLIP;
			case "toggle" -> Mode.SWITCH_FOLLOW;
			case "detached" -> Mode.DETACHED;
			default -> Mode.OTHER; // "action", ...
		};
	}

	@Override
	public Mode getVal() {
		return mode;
	}

	@Override
	public String setMode(Mode target) {
		if(mode == Mode.NOT_APPLICABLE || channels.isEmpty()) {
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
		for(Channel c: channels) {
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
		return msg;
	}
}
