package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;

import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.InputActionInterface;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import tools.jackson.databind.JsonNode;

public class InputSensor extends Sensor implements InputActionInterface {
	public static final int OBJ_ID = 0x3A; // dec. 58
	private final Input input;

	InputSensor(int id, JsonNode sensorConf) {
		super(id, sensorConf);
		this.input = new Input();
		this.objID = OBJ_ID;
		this.mType = null;
	}
	
	@Override
	public void fill(JsonNode comp) {
		name = comp.path("config").path("name").asString("");
//		value = comp.path("status").path("value")...Value();
	}
	
	@Override
	public void associateWH(Webhooks webhooks) {
		input.associateWH(webhooks.getHooksList(DynamicComponents.BTHOME_SENSOR + this.id));
	}

	@Override
	public boolean isInputOn() {
		return false;
	}

	@Override
	public int getRegisteredEventsCount() {
		return input.getRegisteredEventsCount();
	}
	
	@Override
	public String getEvent(int i) {
		return input.getEvent(i);
	}

	@Override
	public boolean enabled() { // input
		return true;
	}

	@Override
	public boolean enabled(int i) { // action type
		return input.enabled(i);
	}
	
	@Override
	public void execute(int i) throws IOException {
		input.execute(i);
	}
}