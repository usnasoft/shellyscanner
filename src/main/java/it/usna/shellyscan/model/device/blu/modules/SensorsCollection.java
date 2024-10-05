package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;

// todo mamma type - sensor ?
// todo mamma ID - sensor ?
// todo measures order ? 
public class SensorsCollection extends Meters {
	private Sensor[] sens;
	private Type[] mTypes;
	
	public SensorsCollection(AbstractBluDevice blu) throws IOException {
		findSensorsID(blu);
	}
	
	private void findSensorsID(AbstractBluDevice blu) throws IOException {
		JsonNode objects = blu.getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + blu.getIndex()).path("objects");
		final Iterator<JsonNode> compIt = objects.iterator();
		
		ArrayList<Sensor> sensors = new ArrayList<>();
		ArrayList<Type> mt = new ArrayList<>();
		while (compIt.hasNext()) {
			JsonNode component = compIt.next();
			String comp = component.path("component").asText();
			if(comp != null && comp.startsWith("bthomesensor:")) {
				Sensor s = new Sensor(comp, component.path("obj_id").intValue());
				Type t = s.getMeterType();
				if(t != null) {
					mt.add(t);
				}
				sensors.add(s);
			}
		}
		sens = sensors.toArray(Sensor[]::new);
		mTypes = mt.toArray(Type[]::new);
	}
	
	public Sensor[] getSensors() {
		return sens;
	}
	
	public Sensor getSensor(int id) {
		for(Sensor s: sens) {
			if(s.getId() == id) {
				return s;
			}
		}
		return null;
	}

	@Override
	public Type[] getTypes() {
		return mTypes;
	}

	@Override
	public float getValue(Type t) {
		for(Sensor s: sens) {
			if(s.getMeterType() == t) {
				return s.getValue();
			}
		}
		return 0f;
	}
}

// https://bthome.io/format/