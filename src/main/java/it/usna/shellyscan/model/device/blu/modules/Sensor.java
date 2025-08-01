package it.usna.shellyscan.model.device.blu.modules;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

/**
 * Sensor factory / Generic BTHSensor / Measure BTHSensor
 */
public class Sensor {
	protected final int id; // Id of the component instance
	private final int idx; // BTHome object index
	protected int objID;
	protected Meters.Type mType;
	protected String name;
	protected float value;

	public static Sensor create(int id, JsonNode sensorConf) {
		int objId = sensorConf.path("obj_id").intValue();
		if(objId == InputSensor.OBJ_ID) {
			return new InputSensor(id, sensorConf);
		} else if(objId == MotionSensor.OBJ_ID) {
			return new MotionSensor(id, sensorConf);
		} else {
			return new Sensor(id, objId, sensorConf);
		}
	}

	protected Sensor(int id, JsonNode sensorConf) {
		this.id = id;
		this.idx = sensorConf.path("idx").intValue();
		this.mType = null;
	}
	
	private Sensor(int id, int objID, JsonNode sensorConf) {
		this.id = id;
		this.objID = objID;
		this.idx = sensorConf.path("idx").intValue();
		this.mType = switch(objID) {
		case 0x01 -> Meters.Type.BAT;
		case 0x2E -> Meters.Type.H;
		case 0x45 -> Meters.Type.T;
		case 0x05 -> Meters.Type.L;
		case 0x2C -> Meters.Type.VIB; // dec 44 - vibration (0-1; on shelly is boolean)
		case 0x40 -> Meters.Type.DMM; // dec 64 - distance mm
		case 0x3F -> Meters.Type.ANG; // dec 63 - angle (accelerometer)
		default -> null;
		};
		
		// 0x3C (60) dimmer (weel)
		// 0x60 (96) channel
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
		JsonNode valNode = comp.path("status").path("value");
		if(valNode.isBoolean()) {
			value = valNode.asBoolean() ? 1f : 0f;
		} else {
			value = comp.path("status").path("value").floatValue();
		}
	}
	
	public String getLabel() {
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