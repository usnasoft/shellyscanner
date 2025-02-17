package it.usna.shellyscan.model.device.blu.modules;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.modules.MotionInterface;

public class MotionSensor extends Sensor implements MotionInterface {
	private final static int OBJ_ID = 0x21; // dec. 33

	MotionSensor(int id, JsonNode sensorConf) {
		super(id, sensorConf);
		this.objID = OBJ_ID;
		this.mType = null;
	}

	@Override
	public boolean motion() {
		return value != 0f;
	}
}
