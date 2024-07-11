package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class Input implements InputInterface {
//	private final AbstractG2Device parent;
	private String name;
	private boolean enable;
//	private boolean reverse;
	private boolean inputIsOn;
	private  Map<String, Webhook> webHooks;

//	public Input(AbstractG2Device parent) {
////		this.parent = parent;
//	}
	
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

	@Override
	public boolean isInputOn() {
		return /*reverse ^*/ inputIsOn;
	}

	@Override
	public int getTypesCount() {
		return webHooks.size();
	}

	@Override
	public Collection<String> getSupportedEvents() {
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

	@Override
	public void execute(String type) throws IOException {
		webHooks.get(type).execute();
	}
	
//	public void trigger(String command) throws IOException {
//		parent.getJSON("/rpc/Input.Trigger?id=" + idx + "&event_type=" + command);
//	}
	
	public void associateWH(Map<String, Webhook> wh) {
		this.webHooks = (wh == null) ? Collections.<String, Webhook>emptyMap() : wh;
	}
	
	public static String restore(AbstractG2Device parent, JsonNode config, int index) {
		return restore(parent, config, index + "");
	}
	
	public static String restore(AbstractG2Device parent, JsonNode config, String index) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", index);

		ObjectNode input = (ObjectNode)config.get("input:" + index).deepCopy();
		input.remove("id");
		out.set("config", input);
		return parent.postCommand("Input.SetConfig", out);
	}
	
	@Override
	public String toString() {
		return name;
	}
}