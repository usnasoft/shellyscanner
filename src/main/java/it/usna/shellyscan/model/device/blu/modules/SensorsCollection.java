package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;

// todo mamma ID - sensor ?
// todo measures order ? 
public class SensorsCollection extends Meters {
	private final AbstractBluDevice blu;
	private Sensor[] sensorsArray;
	private Type[] mTypes;
	private EnumMap<Type, Sensor> measuresMap = new EnumMap<>(Type.class);
	
	public SensorsCollection(AbstractBluDevice blu) throws IOException {
		this.blu = blu;
		init();
	}
	
	private void init() throws IOException {
		JsonNode objects = blu.getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + blu.getIndex()).path("objects");
		final Iterator<JsonNode> compIt = objects.iterator();
		
		ArrayList<Sensor> sensors = new ArrayList<>();
		while (compIt.hasNext()) {
			JsonNode component = compIt.next();
			String comp = component.path("component").asText();
			if(comp != null && comp.startsWith("bthomesensor:")) {
				Sensor s = new Sensor(comp, component.path("obj_id").intValue());
				Type t = s.getMeterType();
				if(t != null) {
					measuresMap.put(t, s);
				}
				sensors.add(s);
			}
		}
		sensorsArray = sensors.toArray(Sensor[]::new);
		mTypes = measuresMap.keySet().toArray(Type[]::new);
	}
	
	public Sensor[] getSensors() {
		return sensorsArray;
	}
	
	public Sensor getSensor(int id) {
		for(Sensor s: sensorsArray) {
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
		return measuresMap.get(t).getValue();
	}
	
	@Override
	public String getName(Type t) {
		return measuresMap.get(t).getName();
	}
	
	public String deleteAll() {
//		JsonNode objects = blue.getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + blue.getIndex()).path("objects");
//		final Iterator<JsonNode> compIt = objects.iterator();
//		while (compIt.hasNext()) {
//			JsonNode component = compIt.next();
//			String comp = component.path("component").asText();
//			if(comp != null && comp.startsWith("bthomesensor:")) {
//				String idx = comp.substring(13);
//				blue.postCommand("BTHome.DeleteSensor", "{\"id\":" + idx + "}");
//			}
//		}
		String err;
		for(Sensor s: sensorsArray) {
			err = blu.postCommand("BTHome.DeleteSensor", "{\"id\":" + s.getId() + "}");
			if(err != null) {
				return err;
			}
		}
		return null;
	}
}

// https://bthome.io/format/