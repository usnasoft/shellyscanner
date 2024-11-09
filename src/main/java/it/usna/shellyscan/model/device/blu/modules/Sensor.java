package it.usna.shellyscan.model.device.blu.modules;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class Sensor {
	private final int id; // Id of the component instance
	private final int idx; // BTHome object index
	private final int objID;
	private final Meters.Type mType;
	private final boolean digitaiInput;
	private String name;
	private float value;

	Sensor(int id, JsonNode sensorConf) {
		this.id = id;
		this.idx = sensorConf.path("idx").intValue();
		this.objID = sensorConf.path("obj_id").intValue();
		if(objID == 0x3A) { // dec. 58
			this.digitaiInput = true;
			this.mType = null;
		} else {
			this.digitaiInput = false;
			this.mType = switch(objID) {
			case 0x01 -> Meters.Type.BAT;
			case 0x2E -> Meters.Type.H;
			case 0x45 -> Meters.Type.T;
			case 0x05 -> Meters.Type.L;
			default -> null;
			};
		}
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
	
	public void fillSConfig(JsonNode config) {
		name = config.path("name").asText("");
	}
	
	public void fillStatus(JsonNode status) {
		value = status.path("value").floatValue();
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
	
	public boolean isInput() {
		return digitaiInput;
	}
	
	@Override
	public String toString() {
		return name + " - " + objID;
	}
}
