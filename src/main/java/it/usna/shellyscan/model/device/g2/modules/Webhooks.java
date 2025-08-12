package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.util.AccumulatingMap;

public class Webhooks {
//	public static final String INPUT_ON = "input.toggle_on";
//	public static final String INPUT_OFF = "input.toggle_off";
//	public static final String INPUT_PUSH = "input.button_push";
//	public static final String INPUT_LONG_PUSH = "input.button_longpush";
//	public static final String INPUT_DOUBLE_PUSH = "input.button_doublepush";
//	public static final String INPUT_TRIPLE_PUSH = "input.button_triplepush";
//	private static final String SENSOR_EVENT_PREFIX = DynamicComponents.BTHOME_SENSOR + ".";
//	private static final String BTDEVICE_EVENT_PREFIX = DynamicComponents.BTHOME_DEVICE + ".";
	
	private final AbstractG2Device parent;
	private AccumulatingMap<String, Webhook> hooks = new AccumulatingMap<>(); // key: input3, bthomedevice200, ...

	public Webhooks(AbstractG2Device parent) {
		this.parent = parent;
	}

	public void fillSettings() throws IOException {
		hooks.clear();
		JsonNode whList = parent.getJSON("/rpc/Webhook.List").get("hooks");
		whList.forEach(hook -> {
			int cid = hook.get("cid").asInt();
			if(cid < DynamicComponents.MIN_ID) {
				String event = hook.get("event").asText();
				int dotpos = event.indexOf('.');
				final String eventOrigin = (dotpos > 0) ? event.substring(0, dotpos) : "";

				hooks.putVal(eventOrigin + cid, new Webhook(hook));
			}
		});
	}
	
	public void fillBTHomesensorSettings() throws IOException {
		hooks.clear();
		JsonNode whList = parent.getJSON("/rpc/Webhook.List").get("hooks");
		whList.forEach(hook -> {
			int cid = hook.get("cid").asInt();
			if(cid >= DynamicComponents.MIN_ID && cid <= DynamicComponents.MAX_ID) {
				String event = hook.get("event").asText();
				int dotpos = event.indexOf('.');
				final String eventOrigin = (dotpos > 0) ? event.substring(0, dotpos) : "";

				hooks.putVal(eventOrigin + cid, new Webhook(hook));
			}
		});
	}
	
	public List<Webhook> getHooksList(String id) {
		return hooks.get(id);
	}

	public static void delete(AbstractG2Device parent, String eventType, int cid, long delay) throws IOException {
		JsonNode whList = parent.getJSON("/rpc/Webhook.List").get("hooks");
		whList.forEach(hook -> {
			if(hook.get("cid").asInt() == cid && hook.get("event").textValue().startsWith(eventType + ".")) {
				try { TimeUnit.MILLISECONDS.sleep(delay); } catch (InterruptedException e) {}
				parent.postCommand("Webhook.Delete", "{\"id\":" + hook.get("id").asInt() + "}");
			}
		});
	}

	public static void restore(AbstractG2Device parent, JsonNode storedWH, long delay, ArrayList<String> errors) throws InterruptedException {
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
	public static void restore(AbstractG2Device parent, String storedKey, /*int newCid*/ String newKey, JsonNode storedWH, long delay, ArrayList<String> errors) throws InterruptedException {
		String typeIdxOld[] = storedKey.split(":");
		String typeIdxNew[] = newKey.split(":");
		restore(parent, typeIdxOld[0], Integer.parseInt(typeIdxOld[1]), Integer.parseInt(typeIdxNew[1]), storedWH, delay, errors);
	}
	
	public static void restore(AbstractG2Device parent, String eventType, int storedCid, int newCid, JsonNode storedWH, long delay, ArrayList<String> errors) throws InterruptedException {
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
}