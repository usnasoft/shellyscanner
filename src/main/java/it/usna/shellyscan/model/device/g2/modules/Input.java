package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class Input implements InputInterface {
	private AbstractG2Device parent;
	private int id;
	private String name;
	private boolean enable;
//	private boolean reverse;
	private boolean inputIsOn;
	private Map<String, Webhook> webHooks;

	public Input(AbstractG2Device parent, int id) {
		this.parent = parent;
		this.id = id;
		webHooks = Collections.<String, Webhook>emptyMap();
	}
	
	public Input() {
	}
	
	public void fillSettings(JsonNode input) {
		name = input.get("name").asText("");
		enable = input.get("enable").booleanValue();
//		reverse = input.get("invert").asBoolean();
	}
	
	public void fillStatus(JsonNode input) {
		inputIsOn = input.get("state").asBoolean();
	}

	@Override
	public String getLabel() {
		return name;
	}
	
	public void setLabel(String name) {
		this.name = name;
	}

	@Override
	public boolean isInputOn() {
		return /*reverse ^*/ inputIsOn;
	}

	@Override
	public int getRegisteredEventsCount() {
		return webHooks.size();
	}

	@Override
	public Collection<String> getRegisteredEvents() {
		return webHooks.keySet();
	}

	@Override
	public boolean enabled(String type) {
		return webHooks.get(type).isEnabled();
	}
	
	@Override
	public boolean enabled() {
		return enable /*name.isEmpty() == false || webHooks.size() > 0*/;
	}
	
	public void setEnabled(boolean enable) {
		this.enable = enable;
	}

	@Override
	public void execute(String type) throws IOException {
		webHooks.get(type).execute();
	}
	
	public void trigger(String command) throws IOException {
		parent.getJSON("/rpc/Input.Trigger?id=" + id + "&event_type=" + command);
	}
	
	public void associateWH(Map<String, Webhook> wh) {
		this.webHooks = (wh == null) ? Collections.<String, Webhook>emptyMap() : wh;
	}
	
	public static String restore(AbstractG2Device parent, JsonNode config, int index) {
		return parent.postCommand("Input.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "input", index));
	}
	
	@Override
	public String toString() {
		return "In:" + name;
	}
}