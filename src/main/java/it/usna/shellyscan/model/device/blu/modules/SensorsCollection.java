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
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Collection of BTHomeDevice related sensors and "Meters" implementation
 */
public class SensorsCollection extends Meters {
	private final AbstractBluDevice blu;
	private Sensor[] sensorsArray;
//	private DeviceModule[] moduleSensors;
	private ArrayList<DeviceModule> modules;
	private Type[] mTypes;
	private EnumMap<Type, Sensor> measuresMap = new EnumMap<>(Type.class);
	
	public SensorsCollection(AbstractBluDevice blu) throws IOException {
		this.blu = blu;
		init();
	}
	
	private void init() throws IOException {
		JsonNode objects = blu.getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + blu.getIndex()).path("objects");

		ArrayList<Sensor> sensors = new ArrayList<>();
		/*ArrayList<Sensor>*/ modules = new ArrayList<>();
		Meters.Type lastT = null;
		Meters.Type lastRot = null;
		for(JsonNode sensorConf: objects) {
			String comp = sensorConf.path("component").asText();
			if(comp != null && comp.startsWith(AbstractBluDevice.SENSOR_KEY_PREFIX)) {
				final int id = Integer.parseInt(comp.substring(13));
				final Sensor sensor = Sensor.create(id, sensorConf); // create
				if(sensor instanceof DeviceModule dm) {
					modules.add(dm);
				} else {
					Type thisType = sensor.getMeterType();
					if(thisType != null) {
						if(thisType == Meters.Type.T) { // up to 5 temperature measures
							if(lastT == null) {
								lastT = Meters.Type.T;
							} else if(lastT == Meters.Type.T) {
								thisType = lastT = Meters.Type.T1;
							} else if(lastT == Meters.Type.T1) {
								thisType = lastT = Meters.Type.T2;
							} else if(lastT == Meters.Type.T2) {
								thisType = lastT = Meters.Type.T3;
							} else if(lastT == Meters.Type.T3) {
								thisType = lastT = Meters.Type.T4;
							}
						}
						if(thisType == Meters.Type.ANG) { // up to 3 angles
							if(lastRot == null) {
								lastRot = Meters.Type.ANG;
							} else if(lastRot == Meters.Type.ANG) {
								thisType = lastRot = Meters.Type.ANG1;
							} else if(lastRot == Meters.Type.ANG1) {
								thisType = lastRot = Meters.Type.ANG2;
							}
						}
						measuresMap.put(thisType, sensor);
					}
				}
				sensors.add(sensor);
			}
		}
		modules.sort((s1, s2) -> ((Sensor)s1).getIdx() - ((Sensor)s2).getIdx()); // order by idx
		sensorsArray = sensors.toArray(Sensor[]::new);
//		moduleSensors = modules.toArray(DeviceModule[]::new);
		mTypes = measuresMap.keySet().toArray(Type[]::new);
	}
	
	public Sensor[] getSensors() {
		return sensorsArray;
	}
	
	public ArrayList<DeviceModule> getModuleSensors() {
		return modules;
	}
	
	public Sensor getSensor(int id) { // HashMap<Integer, Sensor> ?
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
	
	public String getFullID() {
		return Stream.of(sensorsArray).map(s -> "s" + s.getObjId()).sorted().collect(Collectors.joining());
	}
	
// use it.usna.shellyscan.model.device.Meters.toString() instead
//	@Override
//	public String toString() {
//		return Stream.of(sensorsArray).map(s -> "s" + s.getObjId()).sorted().collect(Collectors.joining());
//	}
}

// https://bthome.io/format/