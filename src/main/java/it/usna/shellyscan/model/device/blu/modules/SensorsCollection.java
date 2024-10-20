package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
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
		Meters.Type lastT = null;
		while (compIt.hasNext()) {
			JsonNode sensorConf = compIt.next();
			String comp = sensorConf.path("component").asText();
			if(comp != null && comp.startsWith("bthomesensor:")) {
				Sensor s = new Sensor(Integer.parseInt(comp.substring(13)), sensorConf);
				Type t = s.getMeterType();
				if(t != null) {
					if(t == Meters.Type.T) { // up to 5 temperature measures
						if(lastT == null) {
							lastT = Meters.Type.T;
						} else if(lastT == Meters.Type.T) {
							t = lastT = Meters.Type.T1;
						} else if(lastT == Meters.Type.T1) {
							t = lastT = Meters.Type.T2;
						} else if(lastT == Meters.Type.T2) {
							t = lastT = Meters.Type.T3;
						} else if(lastT == Meters.Type.T3) {
							t = lastT = Meters.Type.T4;
						}
					}
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
	
	public Sensor getSensor(int objID, int idx) {
		for(Sensor s: sensorsArray) {
			if(s.getObjId() == objID && s.getIdx() == idx) {
				return s;
			}
		}
		return null;
	}
	
	public String deleteAll() throws InterruptedException {
		String err;
		for(Sensor s: sensorsArray) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			err = blu.postCommand("BTHome.DeleteSensor", "{\"id\":" + s.getId() + "}");
			if(err != null) {
				return err;
			}
		}
		return null;
	}
}

// https://bthome.io/format/