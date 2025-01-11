package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class Actions {
	public final static String PUSH = "shortpush_url";
	public final static String DOUBLE_PUSH = "double_shortpush_url";
	public final static String TRIPLE_PUSH = "triple_shortpush_url";
	public final static String LONG_PUSH = "longpush_url";
	public final static String BUTTON_ON = "btn_on_url";
	public final static String BUTTON_OFF = "btn_off_url";
	public final static String OUT_ON = "out_on_url";
	public final static String OUT_OFF = "out_off_url";
	public final static String SHORT_LONG_PUSH = "shortpush_longpush_url";
	public final static String LONG_SHORT_PUSH = "longpush_shortpush_url";
	public final static String ROLLER_OPEN = "roller_open_url";
	public final static String ROLLER_CLOSE = "roller_close_url";
	public final static String ROLLER_STOP = "roller_stop_url";
	
	private final AbstractG1Device parent;
	private Map<Integer, Input> inputMap = new HashMap<>();
	
	public Actions(AbstractG1Device parent) {
		this.parent = parent;
	}
	
	public JsonNode fillSettings(JsonNode inputs) throws IOException {
		inputMap.clear();
		JsonNode actions = parent.getJSON("/settings/actions");
		Iterator<Entry<String, JsonNode>> events = actions.get("actions").fields();
		while(events.hasNext()) {
			Entry<String, JsonNode> ev = events.next();
			Iterator<JsonNode> itIdx = ev.getValue().iterator();
			while(itIdx.hasNext()) {
				Iterator<Entry<String, JsonNode>> pars = itIdx.next().fields();
				List<String> urls = new ArrayList<>();
				boolean enabled = false;
				int index = 0;
				while(pars.hasNext()) {
					Entry<String, JsonNode> entry = pars.next();
					final String key = entry.getKey();
					final JsonNode val = entry.getValue();
					if(key.equals("urls")) {
						for(JsonNode url: val) {
							urls.add(url.asText());
						}
					} else if(key.equals("enabled")) {
						enabled = val.asBoolean();
					} else if(key.equals("index")) {
						index = val.asInt();
					}
				}
				Input input = inputMap.get(index);
				if(input == null) {
//					JsonNode in = inputs.get(index);
					input = new Input(inputs.get(index).get("name").asText(""));
//					input.setReverse(in.path("btn_reverse").asBoolean(false));
					
					inputMap.put(index, input);
				}
				// if(enabled || urls.size() > 0)
				input.addAction(ev.getKey(), new Action(enabled, urls));
			}
		}
		return actions;
	}
	
	public void fillStatus(JsonNode inputs) throws IOException {
		for(Map.Entry<Integer, Input> inEntry: inputMap.entrySet()) {
			JsonNode in = inputs.get(inEntry.getKey());
			inEntry.getValue().setInputIsOn(in.path("input").asBoolean());
		}
	}
	
	public Input getInput(int index) {
		return inputMap.get(index);
	}
	
	public static void restore(AbstractG1Device parent, JsonNode actions, long delay, ArrayList<String> errors) throws IOException, InterruptedException {
		Iterator<Entry<String, JsonNode>> events = actions.get("actions").fields();
		while(events.hasNext()) {
			Entry<String, JsonNode> ev = events.next();
			Iterator<JsonNode> itIdx = ev.getValue().iterator();
			while(itIdx.hasNext()) {
				String command = "/settings/actions?name=" + ev.getKey();
				Iterator<Entry<String, JsonNode>> pars = itIdx.next().fields();
				while(pars.hasNext()) {
					command += /*"&" +*/ actionJsonEntryToURLPar(pars.next());
				}
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(parent.sendCommand(command));
			}
		}
	}
	
	private static String actionJsonEntryToURLPar(Entry<String, JsonNode> jsonEntry) throws UnsupportedEncodingException {
		final String name = jsonEntry.getKey();
		final JsonNode val = jsonEntry.getValue();
		if(val.isArray()) {
			String res = "";
			if(val.size() > 0) {
				for(int i=0; i < val.size(); i++) {
					if(val.get(i).isContainerNode()) {
						Iterator<Entry<String, JsonNode>> fields = ((ContainerNode<?>)val.get(i)).fields();
						while(fields.hasNext()) {
							Entry<String, JsonNode> field = fields.next();
							res += "&" + name + "[" + i + "][" + field.getKey() + "]=" + URLEncoder.encode(field.getValue().asText(""), StandardCharsets.UTF_8.name());
						}
					} else {
						res += "&" + name + "[]=" + URLEncoder.encode(val.get(i).asText(""), StandardCharsets.UTF_8.name());
					}
				}
			} else {
				res = "&" + name + "[]=";
			}
			return res;
		} else {
			return "&" + name + "=" + URLEncoder.encode(val.asText(), StandardCharsets.UTF_8.name());
		}
	}

/*
 * Shelly 2.5
    "btn_on_url" : [ {
      "index" : 0,
      "urls" : [ ],
      "enabled" : false
    }, {
      "index" : 1,
      "urls" : [ "http://192.168.1.207/light/0?turn=toggle", "http://192.168.1.208/light/0?turn=toggle" ],
      "enabled" : false
    } ],

 * Shelly motion
    "motion_off" : [ {
        "index" : 0,
        "enabled" : true,
        "urls" : [ {
          "url" : "HTTP://xx.it",
          "int" : "0159-2301"
        } ]
      } ],
	
	http://192.168.1.225/settings/actions?index=0&enabled=true&name=motion_on&urls[0][url]=http%3A%2F%2Fxx.ii&urls[0][int]=0000-0000&urls[1][url]=http%3A%2F%2Fxx2.ii&urls[1][int]=0000-0010
*/
	
	private class Input implements InputInterface {
		private final String name;
//		private boolean reverse;
		private boolean inputIsOn;
		private final Map<String, Action> act = new LinkedHashMap<>();
		
		private Input(String name) {
			this.name = name;
		}
		
		@Override
		public boolean isInputOn() {
			return /*reverse ^*/ inputIsOn;
		}
		
		private void addAction(String type, Action a) {
			act.put(type, a);
		}
		
		@Override
		public boolean enabled(String type) {
			Action action;
			return act != null && (action = act.get(type)) != null && action.isActive();
		}
		
		@Override
		public boolean enabled() {
//			return name.isEmpty() == false || (act != null && act.size() > 0);
			return true;
		}
		
		@Override
		public void execute(String type) throws IOException {
			act.get(type).execute();
		}
		
//		public Action getAction(String type) {
//			return act.get(type);
//		}
		
		@Override
		public Set<String> getRegisteredEvents() {
			return act.keySet();
		}
		
		@Override
		public int getRegisteredEventsCount() {
			return act.size();
		}
		
		@Override
		public String getLabel() {
			return (name == null || name.isEmpty()) ? parent.getName() : name;
		}
		
		@Override
		public String toString() {
			return getLabel();
		}
		
//		private void setReverse(boolean reverse) {
//			this.reverse = reverse;
//		}
		
		private void setInputIsOn(boolean on) {
			this.inputIsOn = on;
		}
	}
	
	private static class Action {
		private boolean enabled;
		private List<String> url;
		
		private Action(boolean enabled, List<String> urls) {
			this.enabled = enabled;
			this.url = urls;
		}
		
		public boolean isActive() {
			return enabled && url != null && url.size() > 0;
		}
		
//		public List<String> getUrls() {
//			return url;
//		}
		
		public void execute() throws IOException {
			for(String url: url) {
				final URL command = new URL(url);
				command.openConnection().getContent();
			}
		}
	}
}