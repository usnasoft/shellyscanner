package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class InputSensor extends Sensor implements InputInterface {
	public final static int OBJ_ID = 0x3A; // dec. 58
	private final Input input;

	InputSensor(int id, JsonNode sensorConf) {
		super(id, sensorConf);
		this.input = new Input();
		this.objID = OBJ_ID;
		this.mType = null;
	}
	
	public void associateWH(Map<String, Webhook> wh) {
		input.associateWH(wh);
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
	public Collection<String> getRegisteredEvents() {
		return input.getRegisteredEvents();
	}

	@Override
	public boolean enabled() { // input
		return true;
	}

	@Override
	public boolean enabled(String type) { // action type
		return input.enabled(type);
	}

	@Override
	public void execute(String type) throws IOException {
		input.execute(type);
	}
}
