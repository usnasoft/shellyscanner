package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;

public class Input implements InputActionInterface {
	private AbstractG2Device parent;
	private int id;
	private String name;
	private boolean enable;
//	private boolean reverse;
	private boolean inputIsOn;
	private List<Webhook> webHooks;

	public Input(AbstractG2Device parent, int id) {
		this.parent = parent;
		this.id = id;
		webHooks = Collections.<Webhook>emptyList();
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
		return webHooks == null ? 0 : webHooks.size();
	}
	
	@Override
	public String getEvent(int i) {
		return webHooks.get(i).getEvent();
	}
	
	@Override
	public boolean enabled(int i) {
		return webHooks.get(i).isEnabled();
	}
	
	@Override
	public boolean enabled() {
		return enable /*name.isEmpty() == false || webHooks.size() > 0*/;
	}
	
	public void setEnabled(boolean enable) {
		this.enable = enable;
	}
	
	@Override
	public void execute(int i) throws IOException {
		webHooks.get(i).execute();
	}
	
	public void trigger(String command) throws IOException {
		parent.getJSON("/rpc/Input.Trigger?id=" + id + "&event_type=" + command);
	}
	
	@Override
	public void associateWH(Webhooks webhooks) {
		associateWH(webhooks.getHooksList("input" + id));
	}

	public void associateWH(List<Webhook> wh) {
		this.webHooks = (wh == null) ? Collections.<Webhook>emptyList() : wh;
	}
	
	public static String restore(AbstractG2Device parent, JsonNode config, int index) {
		return parent.postCommand("Input.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "input", index));
	}
	
	@Override
	public String toString() {
		return "In:" + name;
	}
}