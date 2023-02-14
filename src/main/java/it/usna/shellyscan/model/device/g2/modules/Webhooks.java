package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class Webhooks {
	public final static String INPUT_ON = "input.toggle_on";
	public final static String INPUT_OFF = "input.toggle_off";
	public final static String INPUT_PUSH = "input.button_push";
	public final static String INPUT_LONG_PUSH = "input.button_longpush";
	public final static String INPUT_DOUBLE_PUSH = "input.button_doublepush";
	
	private final AbstractG2Device parent;
	private Map<Integer, Map<String, Webhook>> hooks = new HashMap<>();

	public Webhooks(AbstractG2Device parent) {
		this.parent = parent;
	}

	public /*JsonNode*/void fillSettings() throws IOException {
		hooks.clear();
		JsonNode wh = parent.getJSON("/rpc/Webhook.List").get("hooks");
		wh.forEach(node -> {
			int cid = node.get("cid").asInt();
			Map<String, Webhook> cidMap = hooks.get(cid);
			if(cidMap == null) {
				cidMap = new HashMap<>();
				hooks.put(cid, cidMap);
			}
			String event = node.get("event").asText();
			cidMap.put(event, new Webhook(node));
		});
		//		System.out.println("x");
	}
	
	public Map<String, Webhook> getHooks(int index) {
		return hooks.get(index);
	}

	public static void restore(AbstractG2Device parent, JsonNode webhooks, ArrayList<String> errors) throws InterruptedException {
		errors.add(parent.postCommand("Webhook.DeleteAll", "{}"));
		for(JsonNode ac: webhooks.get("hooks")) {
			ObjectNode thisAction = (ObjectNode)ac.deepCopy();
			thisAction.remove("id");
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(parent.postCommand("Webhook.Create", thisAction));
		}
	}

	public static class Webhook {
//		private int id;
		private boolean enable;
//		private String event;
		private String name;
		private List<String> urls = new ArrayList<>();

		private Webhook(JsonNode wh) {
//			id = wh.get("id").asInt();
			enable = wh.get("enable").asBoolean();
//			event = wh.get("event").asText();
			name = wh.get("name").asText();
			wh.get("urls").forEach(url -> urls.add(url.asText()));
		}
		
		public boolean isEnabled() {
			return enable;
		}
		
//		public String getEvent() {
//			return event;
//		}
		
		public void execute() throws IOException {
			for(String url: urls) {
				final URL command = new URL(url);
				command.openConnection().getContent();
			}
		}
		
		public String toString() {
			return name + " - " + enable;
		}
	}
}
