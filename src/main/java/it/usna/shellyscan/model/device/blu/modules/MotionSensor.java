package it.usna.shellyscan.model.device.blu.modules;

import it.usna.shellyscan.model.device.modules.MotionInterface;
import tools.jackson.databind.JsonNode;

public class MotionSensor extends Sensor implements MotionInterface {
	public static final int OBJ_ID = 0x21; // dec. 33
	private boolean motion;

	MotionSensor(int id, JsonNode sensorConf) {
		super(id, sensorConf);
		this.objID = OBJ_ID;
		this.mType = null;
	}
	
	@Override
	public void fill(JsonNode comp) {
		name = comp.path("config").path("name").asString("");
		motion = comp.path("status").path("value").asBoolean(); // description: 0 (False = Clear) - 1 (True = Detected) but real boolean found
	}
	
//	@Override
//	public String getLabel() {
//		return name.isEmpty() ?  "Motion: " + motion : name + ": " + motion;
//	}

	@Override
	public boolean motion() {
		return motion;
	}
}
