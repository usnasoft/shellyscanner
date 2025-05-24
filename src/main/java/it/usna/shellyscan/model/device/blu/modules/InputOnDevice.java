package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.InputActionInterface;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;

public class InputOnDevice implements InputActionInterface {
	private final Input input;
	private final String condition;
	private final String hookId;
	private String name;

	public InputOnDevice(String cond, String parentId) {
		if(cond.isEmpty()) { // "condition" : "ev.idx == 0"
			this.name = null;
			this.condition = null;
		} else {
			this.name = cond.substring(10); // "condition" : "ev.idx == 0"
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