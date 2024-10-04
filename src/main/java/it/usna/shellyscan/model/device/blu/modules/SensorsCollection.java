package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;

public class SensorsCollection extends Meters {
	
	public SensorsCollection(AbstractBluDevice blu) {
		
	}
	
	private ArrayList<String> findSensorsID(AbstractBluDevice blu) throws IOException {
		JsonNode objects = blu.getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + blu.getIndex()).path("objects");
		final Iterator<JsonNode> compIt = objects.iterator();
		ArrayList<String> l = new ArrayList<>();
		while (compIt.hasNext()) {
			String comp = compIt.next().path("component").asText();
			if(comp != null && comp.startsWith("bthomesensor:")) {
				l.add(comp);
				new Sensor(); // todo
			}
		}
		return l;
	}

	@Override
	public Type[] getTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getValue(Type t) {
		// TODO Auto-generated method stub
		return 0;
	}

}
