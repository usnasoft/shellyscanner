package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.modules.InputInterface;

/**
 * Collection of BTHomeDevice related sensors and "Meters" implementation
 */
public class SensorsCollection extends Meters {
	private final AbstractBluDevice blu;
	private Sensor[] sensorsArray;
	private InputSensor[] inputSensors;
	private Type[] mTypes;
	private EnumMap<Type, Sensor> measuresMap = new EnumMap<>(Type.class);
	
	public SensorsCollection(AbstractBluDevice blu) throws IOException {
		this.blu = blu;
		init();
	}
	
	private void init() throws IOException {
		JsonNode objects = blu.getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + blu.getIndex()).path("objects");

		ArrayList<Sensor> sensors = new ArrayList<>();
		ArrayList<Sensor> inputs = new ArrayList<>();
		Meters.Type lastT = null;
		for(JsonNode sensorConf: objects) {
			String comp = sensorConf.path("component").asText();
			if(comp != null && comp.startsWith(AbstractBluDevice.SENSOR_KEY_PREFIX)) {
				final int id = Integer.parseInt(comp.substring(13));
				final Sensor sensor = Sensor.create(id, sensorConf); // create
				if(sensor instanceof InputInterface) {
					inputs.add(sensor);
				} else {
					Type t = sensor.getMeterType();
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
						measuresMap.put(t, sensor);
					}
				}
				sensors.add(sensor);
			}
		}
		inputs.sort((s1, s2) -> s1.getIdx() - s2.getIdx()); // order by idx
		sensorsArray = sensors.toArray(Sensor[]::new);
		inputSensors = inputs.toArray(InputSensor[]::new);
		mTypes = measuresMap.keySet().toArray(Type[]::new);
	}
	
	public Sensor[] getSensors() {
		return sensorsArray;
	}
	
	public InputSensor[] getInputSensors() {
		return inputSensors;
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
		return measuresMap.get(t).getLabel();
	}
	
//	public Sensor getSensor(int objID, int idx) {
//		for(Sensor s: sensorsArray) {
//			if(s.getObjId() == objID && s.getIdx() == idx) {
//				return s;
//			}
//		}
//		return null;
//	}
	
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
	
	@Override
	public String toString() {
		return Stream.of(sensorsArray).map(s -> "s" + s.getObjId()).sorted().collect(Collectors.joining());
	}
}

// https://bthome.io/format/