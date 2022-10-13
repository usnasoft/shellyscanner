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

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;

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
	private Map<Integer, Input> ac;
	
	public Actions(AbstractG1Device parent) {
		this.parent = parent;
	}
	
	public void fillSettings(JsonNode inputs) throws IOException {
		ac = new HashMap<>();
		Iterator<Entry<String, JsonNode>> events = parent.getJSON("/settings/actions").get("actions").fields();
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
				Input input = ac.get(index);
				if(input == null) {
					input = new Input(inputs.get(index).get("name").asText(""));
					ac.put(index, input);
				}
				input.addAction(ev.getKey(), new Action(enabled, urls));
			}
		}
	}
	
	public Input getInput(int index) {
		return ac.get(index);
	}
	
	public static void restore(AbstractG1Device parent, JsonNode actions, ArrayList<String> errors) throws IOException {
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
				errors.add(parent.sendCommand(command));
			}
		}
	}
	
	public class Input implements DeviceModule {
		private final String name;
		private final Map<String, Action> act = new LinkedHashMap<>();
		
		private Input(String name) {
			this.name = name;
		}
		
		private void addAction(String type, Action a) {
			act.put(type, a);
		}
		
		public boolean enabled(String type) {
			Action action;
			return act != null && (action = act.get(type)) != null && action.isActive();
		}
		
		public void execute(String type) throws IOException {
			List<String> urls = act.get(type).getUrls();
			for(String url: urls) {
				final URL command = new URL(url);
				command.openConnection().getContent();
			}
		}
		
		public Set<String> getSupportedTypes() {
			return act.keySet();
		}
		
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
		
		public List<String> getUrls() {
			return url;
		}
	}
}