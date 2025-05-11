package it.usna.shellyscan.view.scheduler;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JTextField;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class MethodHints {
	private final AbstractG2Device device;
	private ArrayList<Method> methodsList;
	
	public MethodHints(AbstractG2Device device) {
		this.device = device;
	}
	
	public Object[] get(JTextField method, JTextField parameters) {
		if(methodsList == null) {
			create();
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
	
	private void create() {
		methodsList = new ArrayList<Method>();
		try {
			final JsonNode components = device.getJSON("/rpc/Shelly.GetComponents?include=[%22config%22]").path("components"); // manage pagination
			final Iterator<JsonNode> compIt = components.iterator();
			while (compIt.hasNext()) {
				JsonNode comp = compIt.next();
				String key = comp.get("key").asText();
				if(key.startsWith("switch:")) {
					String id = comp.get("config").path("id").asText();
					String compName = comp.get("config").path("name").textValue();
					String nameBase = ((compName == null || compName.length() == 0) ? key : compName) + " - ";
					methodsList.add(new Method(nameBase + "On", "switch.set", "\"id\":" + id + ",\"on\":true"));
					methodsList.add(new Method(nameBase + "Off", "switch.set", "\"id\":" + id + ",\"on\":false"));
					methodsList.add(new Method(nameBase + "Toggle", "switch.toggle", "\"id\":" + id));
				}
			}
			
//			methodsList.add(new Method("input 0", "input", "\"id\"=0"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	record Method(String name, String method, String pars) {}
}
