package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class Webhooks {
//	public final static String INPUT_ON = "input.toggle_on";
//	public final static String INPUT_OFF = "input.toggle_off";
//	public final static String INPUT_PUSH = "input.button_push";
//	public final static String INPUT_LONG_PUSH = "input.button_longpush";
//	public final static String INPUT_DOUBLE_PUSH = "input.button_doublepush";
//	public final static String INPUT_TRIPLE_PUSH = "input.button_triplepush";
	private final static String SENSOR_EVENT_PREFIX = DynamicComponents.BTHOME_SENSOR + ".";
	
	private final AbstractG2Device parent;
	private Map<Integer, Map<String, Webhook>> hooks = new HashMap<>();

	public Webhooks(AbstractG2Device parent) {
		this.parent = parent;
	}

	public void fillSettings() throws IOException {
		hooks.clear();
		JsonNode wh = parent.getJSON("/rpc/Webhook.List").get("hooks");
		wh.forEach(hook -> {
			int cid = hook.get("cid").asInt();
			if(cid < DynamicComponents.MIN_ID) {
				Map<String, Webhook> cidMap = hooks.get(cid);
				if(cidMap == null) {
					cidMap = new LinkedHashMap<>();
					hooks.put(cid, cidMap);
				}
				String event = hook.get("event").asText();
				cidMap.put(event, new Webhook(hook));
			}
		});
	}
	
	public void fillBTHomesensorSettings() throws IOException {
		hooks.clear();
		JsonNode wh = parent.getJSON("/rpc/Webhook.List").get("hooks");
		wh.forEach(hook -> {
			int cid = hook.get("cid").asInt();
			String event;
			if(cid >= DynamicComponents.MIN_ID && cid <= DynamicComponents.MAX_ID && (event = hook.get("event").textValue()).startsWith(SENSOR_EVENT_PREFIX)) {
				Map<String, Webhook> cidMap = hooks.get(cid);
				if(cidMap == null) {
					cidMap = new LinkedHashMap<>();
					hooks.put(cid, cidMap);
				}
				cidMap.put(event, new Webhook(hook));
			}
		});
	}
	
	public Map<String, Webhook> getHooks(int cid) {
		return hooks.get(cid);
	}

//	public static void deleteByCid(AbstractG2Device parent, String type, int cid) throws IOException {
//		JsonNode wh = parent.getJSON("/rpc/Webhook.List").get("hooks");
//		wh.forEach(hook -> {
//			if(hook.get("cid").asInt() == cid && hook.get("event").textValue().startsWith(type + ".")) {
//				parent.postCommand("Webhook.Delete", "{\"id\":" + hook.get("id").asInt() + "}");
//			}
//		});
//	}

	public static void restore(AbstractG2Device parent, long delay, JsonNode storedWH, ArrayList<String> errors) throws InterruptedException {
		TimeUnit.MILLISECONDS.sleep(delay);
		errors.add(parent.postCommand("Webhook.DeleteAll", "{}"));
		for(JsonNode ac: storedWH.get("hooks")) {
			ObjectNode thisAction = (ObjectNode)ac.deepCopy();
			thisAction.remove("id");
			TimeUnit.MILLISECONDS.sleep(delay);
			String ret = parent.postCommand("Webhook.Create", thisAction);
			if(ret != null) {
				ret = "Action \"" + ac.path("name").asText("") + "\" - error: " + ret;
			}
			errors.add(ret);
		}
	}
	
	// restore all webhooks with a specific "cid" on a new "cid"
	public static void restore(AbstractG2Device parent, String storedKey, /*int newCid*/ String newKey, long delay, JsonNode storedWH, ArrayList<String> errors) throws InterruptedException {
		String typeIdxOld[] = storedKey.split(":");
		String typeIdxNew[] = newKey.split(":");
		for(JsonNode ac: storedWH.get("hooks")) {
			if(ac.get("cid").intValue() == Integer.parseInt(typeIdxOld[1]) && ac.get("event").textValue().startsWith(typeIdxOld[0] + ".")) {
				ObjectNode thisAction = (ObjectNode)ac.deepCopy();
				thisAction.remove("id");
				thisAction.put("cid", Integer.parseInt(typeIdxNew[1]));
				TimeUnit.MILLISECONDS.sleep(delay);
				String ret =  parent.postCommand("Webhook.Create", thisAction);
				if(ret != null) {
					ret = "Action \"" + ac.path("name").asText("") + "\" - error: " + ret;
				}
				errors.add(ret);
			}
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
	
	record EventHook(String event, Webhook hook) {}
}