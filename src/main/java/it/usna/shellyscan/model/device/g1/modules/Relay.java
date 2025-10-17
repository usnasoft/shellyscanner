package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.RelayInterface;

/**
 * Used by 1, 1PM, EM, 2, 2.5, ...
 */
public class Relay implements RelayInterface {
	private final AbstractG1Device parent;
	private final int index;
	private String name;
	private boolean isOn;
	private String source;
	private boolean inputIsOn;
	
	public Relay(AbstractG1Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	public void fillSettings(JsonNode settingsRelay) {
		name = settingsRelay.get("name").asText("");
	}
	
	public void fillStatus(JsonNode relay) {
		isOn = relay.get("ison").booleanValue();
		source = relay.get("source").asText("-");
	}
	
	public void fillStatus(JsonNode relay, JsonNode inputs) {
		isOn = relay.get("ison").booleanValue();
		source = relay.path("source").asText("-"); //old fw miss "source"
		inputIsOn = inputs.path("input").booleanValue();
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean toggle() throws IOException {
		final JsonNode relay = parent.getJSON("/relay/" + index + "?turn=toggle");
		isOn = relay.get("ison").booleanValue();
		source = relay.get("source").asText("-");
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
		final JsonNode relay = parent.getJSON("/relay/" + index + "?turn=" + (on ? "on" : "off"));
		isOn = relay.get("ison").booleanValue();
		source = relay.get("source").asText("-");
	}
	
	@Override
	public boolean isOn() {
		return isOn;
	}
	
	@Override
	public boolean isInputOn() {
		return inputIsOn /*^ reverse*/;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode data) throws IOException {
		((ObjectNode)data).remove("ison");
		((ObjectNode)data).remove("has_timer");
		((ObjectNode)data).remove("overpower"); // 2.5
		return parent.sendCommand("/settings/relay/" + index + "?" + AbstractG1Device.jsonEntrySetToURLPar(data.properties()));
	}
	
	@Override
	public String getLabel() {
		return (name == null || name.isEmpty()) ? parent.getName() : name;
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + (isOn ? "ON" : "OFF");
	}
}