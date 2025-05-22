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
//	private final static String SENSOR_EVENT_PREFIX = DynamicComponents.BTHOME_SENSOR + ".";
//	private final static String BTDEVICE_EVENT_PREFIX = DynamicComponents.BTHOME_DEVICE + ".";
	
	private final AbstractG2Device parent;
//	private Map<Integer, Map<String, Webhook>> hooks = new HashMap<>();
	private Map<String, Map<String, Webhook>> hooks = new HashMap<>();

	/*
	<eventOrigin><cid>: <event>:hookObj - e.g.

	 {
        "cid": 3,
        "condition": null,
        "enable": true,
        "event": "input.button_push",
        "id": 2,
        "name": "action input",
        "repeat_period": 0,
        "ssl_ca": "ca.pem",
        "urls": [ "http://xxx.x ]
    }
    
	input3 -> {button_push -> hookObj}
	
	input3 -> [hookObj]
	*/

	public Webhooks(AbstractG2Device parent) {
		this.parent = parent;
	}

	public void fillSettings(/*String eventOrigin*/) throws IOException {
		hooks.clear();
		JsonNode wh = parent.getJSON("/rpc/Webhook.List").get("hooks");
		wh.forEach(hook -> {
			int cid = hook.get("cid").asInt();
			if(cid < DynamicComponents.MIN_ID /*&& (event = hook.get("event").asText()).startsWith(eventOrigin)*/) {
				String event = hook.get("event").asText();
				int dotpos = event.indexOf('.');
				final String eventOrigin;
//				final String eventType;
				if(dotpos > 0) {
					eventOrigin = event.substring(0, dotpos);
//					eventType =event.substring(dotpos + 1);
				} else {
					eventOrigin = "";
//					eventType = event;
				}
				
				// old model
				Map<String, Webhook> cidMap = hooks.get(eventOrigin + cid);
				if(cidMap == null) {
					cidMap = new LinkedHashMap<>();
//					hooks.put(cid, cidMap);
					hooks.put(eventOrigin + cid, cidMap);
				}
				cidMap.put(event, new Webhook(hook));
				

			}
		});
	}
	
	public void fillBTHomesensorSettings() throws IOException {
		hooks.clear();
		JsonNode wh = parent.getJSON("/rpc/Webhook.List").get("hooks");
		wh.forEach(hook -> {
			int cid = hook.get("cid").asInt();
			if(cid >= DynamicComponents.MIN_ID && cid <= DynamicComponents.MAX_ID) {
				String event = hook.get("event").asText();
				int dotpos = event.indexOf('.');
				final String eventOrigin;
//				final String eventType;
				if(dotpos > 0) {
					eventOrigin = event.substring(0, dotpos);
//					eventType = event.substring(dotpos + 1);
				} else {
					eventOrigin = "";
//					eventType = event;
				}
//				if(event.startsWith(SENSOR_EVENT_PREFIX)) {
					Map<String, Webhook> cidMap = hooks.get(eventOrigin + cid);
					if(cidMap == null) {
						cidMap = new LinkedHashMap<>();
//						hooks.put(cid, cidMap);
						hooks.put(eventOrigin + cid, cidMap);
					}
					cidMap.put(event, new Webhook(hook));
//				}
			}
		});
	}
	
	public Map<String, Webhook> getHooks(/*int*/String id) {
		return hooks.get(id);
	}
	

	
//	public Map<String, Webhook> getHooks(String origin, int cid) {
//		return hooks.get(cid);
//	}

	public static void delete(AbstractG2Device parent, String eventType, int cid) throws IOException {
		JsonNode wh = parent.getJSON("/rpc/Webhook.List").get("hooks");
		wh.forEach(hook -> {
			if(hook.get("cid").asInt() == cid && hook.get("event").textValue().startsWith(eventType + ".")) {
				parent.postCommand("Webhook.Delete", "{\"id\":" + hook.get("id").asInt() + "}");
			}
		});
	}

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
		restore(parent, typeIdxOld[0], Integer.parseInt(typeIdxOld[1]), Integer.parseInt(typeIdxNew[1]), delay, storedWH, errors);
	}
	
	public static void restore(AbstractG2Device parent, String eventType, int storedCid, int newCid, long delay, JsonNode storedWH, ArrayList<String> errors) throws InterruptedException {
		for(JsonNode ac: storedWH.get("hooks")) {
			if(ac.get("cid").intValue() == storedCid && ac.get("event").textValue().startsWith(eventType + ".")) {
				ObjectNode thisAction = (ObjectNode)ac.deepCopy();
				thisAction.remove("id");
				thisAction.put("cid", newCid);
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
		private String event;
		private String name;
		private String condition;
		private List<String> urls = new ArrayList<>();

		private Webhook(JsonNode wh) {
//			id = wh.get("id").asInt();
			enable = wh.get("enable").asBoolean();
			event = wh.get("event").asText();
			name = wh.get("name").asText();
			condition = wh.path("condition").textValue();
			wh.get("urls").forEach(url -> urls.add(url.asText()));
		}
		
		public boolean isEnabled() {
			return enable;
		}
		
		public String getEvent() {
			return event;
		}
		
		public String getCondition() {
			return condition;
		}
		
		public void execute() throws IOException {
			for(String url: urls) {
				final URL command = new URL(url);
				command.openConnection().getContent();
			}
		}
		
		public String toString() {
			return name + " - " + condition + " - " + enable;
		}
	}
	
//	record EventHook(String event, Webhook hook) {}
}
// todo serve una classificazione elemento/cid per esempio "input0" invece di "0"; dove "input" e' prese da "event fino al '.'