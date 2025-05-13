package it.usna.shellyscan.view.scheduler;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class MethodHints {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHints.class);
	private final AbstractG2Device device;
	private ArrayList<Method> methodsList;
	
	public MethodHints(AbstractG2Device device) {
		this.device = device;
	}
	
	public Object[] get(JTextField method, JTextField parameters) {
		if(methodsList == null) {
			generate();
		}

		Object[] actions = new Object[methodsList.size()];
		for(int i = 0; i < actions.length; i++) {
			Method m = methodsList.get(i);
			actions[i] = new AbstractAction(m.name) {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					method.setText(m.method);
					parameters.setText(m.pars);
				}
			};
		}
		return actions;
	}
	
	private void generate() {
		methodsList = new ArrayList<Method>();
		try {
			final JsonNode components = device.getJSON("/rpc/Shelly.GetComponents?include=[%22config%22]").path("components"); // todo: manage pagination
			final Iterator<JsonNode> compIt = components.iterator();
			while (compIt.hasNext()) {
				JsonNode comp = compIt.next();
				String key = comp.get("key").asText();
				if(key.startsWith("switch:")) {
					int id = comp.get("config").path("id").intValue();
					String nameBase = actionBaseName(comp);
					methodsList.add(new Method(nameBase + "On", "switch.set", "\"id\":" + id + ",\"on\":true"));
					methodsList.add(new Method(nameBase + "Off", "switch.set", "\"id\":" + id + ",\"on\":false"));
					methodsList.add(new Method(nameBase + "Toggle", "switch.toggle", "\"id\":" + id));
				} else if(key.startsWith("cover:")) {
					int id = comp.get("config").path("id").intValue();
					String nameBase = actionBaseName(comp);
					methodsList.add(new Method(nameBase + "Open", "Cover.Open", "\"id\":" + id));
					methodsList.add(new Method(nameBase + "Close", "Cover.Close", "\"id\":" + id));
					methodsList.add(new Method(nameBase + "Go 50%", "Cover.GoToPosition", "\"id\":" + id + ",\"pos\":50"));
					// config/slat/slat == true ...
				} else if(key.startsWith("light:")) {
					int id = comp.get("config").path("id").intValue();
					String nameBase = actionBaseName(comp);
					methodsList.add(new Method(nameBase + "On", "Light.Set", "\"id\":" + id + ",\"on\":true"));
					methodsList.add(new Method(nameBase + "Off", "Light.Set", "\"id\":" + id + ",\"on\":false"));
					methodsList.add(new Method(nameBase + "Toggle", "Light.Toggle", "\"id\":" + id));
					methodsList.add(new Method(nameBase + "On 50%", "Light.Set", "\"id\":" + id + ",\"on\":true,\"brightness\":50"));
				} else if(key.startsWith("rgb:")) {
					int id = comp.get("config").path("id").intValue();
					String nameBase = actionBaseName(comp);
					methodsList.add(new Method(nameBase + "On", "RGB.Set", "\"id\":" + id + ",\"on\":true"));
					methodsList.add(new Method(nameBase + "Off", "RGB.Set", "\"id\":" + id + ",\"on\":false"));
					methodsList.add(new Method(nameBase + "Toggle", "RGB.Toggle", "\"id\":" + id));
					methodsList.add(new Method(nameBase + "On 50%", "RGB.Set", "\"id\":" + id + ",\"on\":true,\"brightness\":50"));
					methodsList.add(new Method(nameBase + "On, red", "RGB.Set", "\"id\":" + id + ",\"on\":true,\"rgb\":[255,0,0]"));
					methodsList.add(new Method(nameBase + "On, green", "RGB.Set", "\"id\":" + id + ",\"on\":true,\"rgb\":[0,255,0]"));
					methodsList.add(new Method(nameBase + "On, blu", "RGB.Set", "\"id\":" + id + ",\"on\":true,\"rgb\":[0,0,255]"));
				} else if(key.startsWith("rgbw:")) {
					int id = comp.get("config").path("id").intValue();
					String nameBase = actionBaseName(comp);
					methodsList.add(new Method(nameBase + "On", "RGBW.Set", "\"id\":" + id + ",\"on\":true"));
					methodsList.add(new Method(nameBase + "Off", "RGBW.Set", "\"id\":" + id + ",\"on\":false"));
					methodsList.add(new Method(nameBase + "Toggle", "RGBW.Toggle", "\"id\":" + id));
					methodsList.add(new Method(nameBase + "On 50%", "RGBW", "\"id\":" + id + ",\"on\":true,\"brightness\":50"));
					methodsList.add(new Method(nameBase + "On, red", "RGBW.Set", "\"id\":" + id + ",\"on\":true,\"white\":0,\"rgb\":[255,0,0]"));
					methodsList.add(new Method(nameBase + "On, green", "RGBW.Set", "\"id\":" + id + ",\"on\":true,\"white\":0,\"rgb\":[0,255,0]"));
					methodsList.add(new Method(nameBase + "On, blu", "RGBW.Set", "\"id\":" + id + ",\"on\":true,\"white\":0,\"rgb\":[0,0,255]"));
					methodsList.add(new Method(nameBase + "On, white", "RGBW.Set", "\"id\":" + id + ",\"on\":true,\"rgb\":[0,0,0],\"white\":255"));
				} else if(key.startsWith("cct")) {
					int id = comp.get("config").path("id").intValue();
					String nameBase = actionBaseName(comp);
					methodsList.add(new Method(nameBase + "On", "CCT.Set", "\"id\":" + id + ",\"on\":true"));
					methodsList.add(new Method(nameBase + "Off", "CCT.Set", "\"id\":" + id + ",\"on\":false"));
					methodsList.add(new Method(nameBase + "Toggle", "CCT.Toggle", "\"id\":" + id));
					methodsList.add(new Method(nameBase + "On 50%", "CCT.Set", "\"id\":" + id + ",\"on\":true,\"brightness\":50"));
					methodsList.add(new Method(nameBase + "On 4000K", "CCT.Set", "\"id\":" + id + ",\"on\":true,\"ct\":4000"));
				} // else if(...
			}
		} catch (IOException e) {
			LOG.error("create hint", e);
		}
	}
	
	private static String actionBaseName(JsonNode comp) {
		String compName = comp.get("config").path("name").textValue();
		return ((compName == null || compName.length() == 0) ? comp.get("key").asText() : compName) + " - ";
	}
	
	record Method(String name, String method, String pars) {}
}
