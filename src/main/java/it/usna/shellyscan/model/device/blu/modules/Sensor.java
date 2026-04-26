package it.usna.shellyscan.model.device.blu.modules;

import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;

/**
 * Sensor factory / Generic BTHSensor / Measure BTHSensor
 * @see https://bthome.io/format/
 */
public class Sensor {
	protected final int id; // Id of the component instance
	private final int idx; // BTHome object index
	protected int objID;
	protected Meters.Type mType;
	protected String name;
	protected float value;

	public static Sensor create(int id, JsonNode sensorConf) {
		final int objId = sensorConf.path("obj_id").intValue(0);
		if(objId == InputSensor.OBJ_ID) {
			return new InputSensor(id, sensorConf);
		} else if(objId == MotionSensor.OBJ_ID) {
			return new MotionSensor(id, sensorConf);
		} else if(objId == DWSensor.OBJ_ID) {
			return new DWSensor(id, sensorConf);
		} else {
			return new Sensor(id, objId, sensorConf);
		}
	}

	protected Sensor(int id, JsonNode sensorConf) {
		this.id = id;
		this.idx = sensorConf.path("idx").intValue(0);
		this.mType = null;
	}
	
	private Sensor(int id, int objID, JsonNode sensorConf) {
		this.id = id;
		this.objID = objID;
		this.idx = sensorConf.path("idx").intValue(0);
		this.mType = switch(objID) {
		case 0x01 -> Meters.Type.BAT;
		case 0x05 -> Meters.Type.L; // lux
		case 0x1E -> Meters.Type.LIGHT; // 30 - 0 (False = No light), 1 (True = Light detected)
		case 0x2C -> Meters.Type.VIB; // 44 - vibration (0-1; on shelly is boolean)
		case 0x2E -> Meters.Type.H; // 46
		case 0x3F -> Meters.Type.ANG; // 63 - angle (accelerometer)
		case 0x40 -> Meters.Type.DMM; // 64 - distance mm
		case 0x45 -> Meters.Type.T; // 69
		case 0x5F -> Meters.Type.RAIN; // 95 - precipitation mm
		case 0x64 -> Meters.Type.LD; // 100 - light level: 0 (dark) - 1 (twilight) - 2 (bright)
		default -> null;
		};
		// 0x3C (60) dimmer (weel)
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
	
	public void fill(JsonNode comp) {
		// config
		name = comp.path("config").path("name").asString("");
		// status
		JsonNode valNode = comp.path("status").path("value");
		if(valNode.isBoolean()) {
			value = valNode.asBoolean() ? 1f : 0f;
		} else {
			value = valNode.floatValue(0); // can be temporarily null
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