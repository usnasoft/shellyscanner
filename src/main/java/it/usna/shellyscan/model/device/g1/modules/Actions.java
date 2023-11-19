package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.net.URL;
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
					command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
				}
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(parent.sendCommand(command));
			}
		}
	}
	
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
		public Set<String> getSupportedEvents() {
			return act.keySet();
		}
		
		@Override
		public int getTypesCount() {
			return act.size();
		}
		
		@Override
		public String getLabel() {
			return name.length() > 0 ? name : parent.getName();
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