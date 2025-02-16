package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class InputSensor extends Sensor implements InputInterface {
	private final Input input;

	InputSensor(int id, JsonNode sensorConf) {
		super(id, sensorConf);
		this.input = new Input();
		this.objID = Sensor.INPUT_OID;
		this.mType = null;
	}
	
	public void associateWH(Map<String, Webhook> wh) {
		input.associateWH(wh);
	}

	@Override
	public String getLabel() {
		return name;
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
	public boolean enabled() {
		return true;
	}

	@Override
	public boolean enabled(String type) {
		return input.enabled(type);
	}

	@Override
	public void execute(String type) throws IOException {
		input.execute(type);
	}
}
