package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.InputActionInterface;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;

public class InputOnDevice implements InputActionInterface {
	private final Input input;
	private final int id;
	private final String hookId;
	private String name;

	public InputOnDevice(int id, String parentId) {
		this.id = id;
		this.hookId = DynamicComponents.BTHOME_DEVICE + parentId;
		this.name = id + "";
		this.input = new Input();
	}
	
	@Override
	public void associateWH(Webhooks webhooks) {
		input.associateWH(webhooks.getHooks(hookId));
	}

	@Override
	public void associateWH(Map<String, Webhook> wh) {
		input.associateWH(wh);
	}

	@Override
	public boolean isInputOn() {
		return false;
	}

	@Override
	public int getRegisteredEventsCount() {
		return input.getRegisteredEventsCount();
	}

	@Override
	public Collection<String> getRegisteredEvents() {
		return input.getRegisteredEvents();
	}

	@Override
	public boolean enabled() { // input
		return true;
	}

	@Override
	public boolean enabled(String type) { // action type
		return input.enabled(type);
	}

	@Override
	public void execute(String type) throws IOException {
		input.execute(type);
	}
	
	public int getId() {
		return id;
	}

	@Override
	public String getLabel() {
		return name;
	}
	
	@Override
	public String toString() {
		return name + " - " + id;
	}
	
//	@Override
//	public boolean equals(Object o) {
//		return id == ((InputOnDevice)o).id;
//	}
//	
//	@Override
//	public int hashCode() {
//		return id;
//	}
}
