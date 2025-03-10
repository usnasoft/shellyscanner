package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.RelayInterface;

/**
 * Used by +1, +1PM, +2PM
 */
public class Relay implements RelayInterface {
	private final AbstractG2Device parent;
	private final int index;
	private String name;
	private boolean isOn;
	private String source;
//	private boolean reverse;
	private boolean inputIsOn;
	
	public Relay(AbstractG2Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	public void fillSettings(JsonNode configuration) {
		name = configuration.get("name").asText("");
	}
	
	public void fillSettings(JsonNode configuration, JsonNode input) {
		name = configuration.get("name").textValue();
		if(name == null || name.isEmpty()) {
			name = input.get("name").asText("");
		}
//		reverse = inputs.get("invert").asBoolean();
	}
	
	public void fillStatus(JsonNode relay) { // Ralay
		isOn = relay.get("output").booleanValue();
		source = relay.get("source").asText("-");
	}
	
	public void fillStatus(JsonNode relay, JsonNode input) { // Ralay + Input
		isOn = relay.get("output").booleanValue();
		source = relay.get("source").asText("-");
		inputIsOn = input.get("state").booleanValue();
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean toggle() throws IOException {
//		final JsonNode relay = parent.getJSON("/relay/" + index + "?turn=toggle");
//		isOn = relay.get("ison").asBoolean();
//		source = relay.get("source").asText("-");
		final JsonNode relay = parent.getJSON("/rpc/Switch.Toggle?id=" + index);
		isOn = relay.get("was_on").asBoolean() == false;
		source = Devices.SCANNER_AGENT;
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
//		final JsonNode relay = parent.getJSON("/relay/" + index + "?turn=" + (on ? "on" : "off"));
//		isOn = relay.get("ison").asBoolean();
//		source = relay.get("source").asText("-");
		parent.getJSON("/rpc/Switch.Set?id=" + index + "&on=" + on);
		isOn = on;
		source = Devices.SCANNER_AGENT;
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
	
	@Override
	public String getLabel() {
		return (name == null || name.isEmpty()) ? parent.getName() : name;
	}
	
	public String restore(JsonNode config) {
		return parent.postCommand("Switch.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "switch", index));
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + (isOn ? "ON" : "OFF");
	}
}