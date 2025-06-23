package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.InputActionInterface;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;

public class InputOnDevice implements InputActionInterface {
	private final static Pattern BUTTON_ID_PATTERN = Pattern.compile("ev.idx\\s*===?\\s*(\\d+)");
	private final static Pattern CHANNEL_ID_PATTERN = Pattern.compile("ev.sensors\\[96\\]\\[0\\]\\.value\\s*===?\\s*(\\d+)");
	// "condition" : "ev.sensors[96][0].value === 1 && ev.idx === 0"
	
	private final Input input;
	private final String condition;
	private final String hookId;
	private String name;

	public InputOnDevice(String cond, String parentId, SensorsCollection sensors) {
		if(cond.isEmpty()) {
			this.name = null;
			this.condition = null;
		} else { // "condition" : "ev.idx == 0" / "ev.idx === 0"
			Matcher buttonMatcher = BUTTON_ID_PATTERN.matcher(cond);
			Matcher channelMatcher = CHANNEL_ID_PATTERN.matcher(cond);
			this.name = "";
			if(channelMatcher.find()) {
				name /*+*/= "ch " + channelMatcher.group(1);
			}
			if(buttonMatcher.find()) {
				if(name.isEmpty() == false) {
					name += " - ";	
				}
				name += buttonMatcher.group(1);
			}
			this.condition = cond;
		}
		this.hookId = DynamicComponents.BTHOME_DEVICE + parentId;
		this.input = new Input();
	}
	
	@Override
	public void associateWH(Webhooks webhooks) {
		List<Webhook> l = webhooks.getHooksList(this.hookId).stream().filter(wh -> Objects.equals(wh.getCondition(), condition)).toList();
		input.associateWH(l);
	}

	@Override
	public boolean isInputOn() {
		return false;
	}

	@Override
	public boolean enabled() { // input
		return true;
	}

	@Override
	public int getRegisteredEventsCount() {
		return input.getRegisteredEventsCount();
	}
	
	@Override
	public String getEvent(int i) {
		return input.getEvent(i);
	}
	
	@Override
	public boolean enabled(int i) { // webhook
		return input.enabled(i);
	}
	
	@Override
	public void execute(int i) throws IOException {
		input.execute(i);
	}

	@Override
	public String getLabel() {
		return name;
	}
	
	@Override
	public String toString() {
		return name + " - " + hookId;
	}
}