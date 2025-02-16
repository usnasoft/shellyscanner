package it.usna.shellyscan.model.device.blu.modules;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

/**
 * Generic BTHSensor / Measure BTHSensor
 */
public class Sensor {
	private final int id; // Id of the component instance
	private final int idx; // BTHome object index
	protected int objID;
	protected Meters.Type mType;
	protected String name;
	private float value;
	public final static int INPUT_OID = 0x3A; // dec. 58
	
	static Sensor create(int id, JsonNode sensorConf) {
		int objId = sensorConf.path("obj_id").intValue();
		if(objId == INPUT_OID) {
			return new InputSensor(id, sensorConf);
		} else {
			return new Sensor(id, objId, sensorConf);
		}
	}

	Sensor(int id, JsonNode sensorConf) {
		this.id = id;
		this.idx = sensorConf.path("idx").intValue();
	}
	
	Sensor(int id, int objID, JsonNode sensorConf) {
		this.id = id;
		this.idx = sensorConf.path("idx").intValue();
		this.objID = objID;
		this.mType = switch(objID) {
		case 0x01 -> Meters.Type.BAT;
		case 0x2E -> Meters.Type.H;
		case 0x45 -> Meters.Type.T;
		case 0x05 -> Meters.Type.L;
		default -> null;
		};
	}
	
	public int getId() {
		return id;
	}
	
	public int getObjId() {
		return objID;
	}
	
	public int getIdx() {
		return idx;
	}
	
//	public void fillSConfig(JsonNode config) {
//		name = config.path("name").asText("");
//	}
//	
//	public void fillStatus(JsonNode status) {
//		value = status.path("value").floatValue();
//	}
	
	public void fill(JsonNode comp) {
		name = comp.path("config").path("name").asText("");
		value = comp.path("status").path("value").floatValue();
	}
	
	public String getName() {
		return name;
	}
	
	public float getValue() {
		return value;
	}
	
	public Meters.Type getMeterType() {
		return mType;
	}
	
	@Override
	public String toString() {
		return name + " - " + objID;
	}
}