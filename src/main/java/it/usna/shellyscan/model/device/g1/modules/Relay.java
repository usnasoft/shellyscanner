package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.RelayInterface;

/**
 * Used by 1, 1PM, EM, 2, 2.5
 */
public class Relay implements RelayInterface {
	private final AbstractG1Device parent;
	private final int index;
	private String name = "";
	private boolean isOn;
	private String source;
//	private final boolean associatedInput;
//	private boolean reverse;
	private boolean inputIsOn;
	
//	public Relay(AbstractG1Device parent, int index, boolean associatedInput) {
//		this.parent = parent;
//		this.index = index;
//		this.associatedInput = associatedInput;
//	}
	
	public Relay(AbstractG1Device parent, int index) {
		this.parent = parent;
		this.index = index;
//		this.associatedInput = false;
	}
	
	public void fillSettings(JsonNode settingsRelay) {
		name = settingsRelay.get("name").asText("");
	}
	
//	public void fillSettings(JsonNode settingsRelay, boolean input) {
//		name = settingsRelay.get("name").asText("");
//		reverse = input && settingsRelay.path("btn_reverse").asBoolean();
//	}
	
	public void fillStatus(JsonNode relay) throws IOException {
		isOn = relay.get("ison").asBoolean();
		source = relay.get("source").asText("-");
	}
	
	public void fillStatus(JsonNode relay, JsonNode inputs) throws IOException {
		isOn = relay.get("ison").asBoolean();
		source = relay.get("source").asText("-");
		inputIsOn = inputs.path("input").asBoolean();
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean toggle() throws IOException {
		final JsonNode relay = parent.getJSON("/relay/" + index + "?turn=toggle");
		isOn = relay.get("ison").asBoolean();
		source = relay.get("source").asText("-");
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
		final JsonNode relay = parent.getJSON("/relay/" + index + "?turn=" + (on ? "on" : "off"));
		isOn = relay.get("ison").asBoolean();
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

//		Iterator<Entry<String, JsonNode>> pars = data.fields();
//		if(pars.hasNext()) {
//			String command = "/settings/relay/" + index + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			while(pars.hasNext()) {
//				command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			}
//			return parent.sendCommand(command);
//		}
//		return null;
		return parent.sendCommand("/settings/relay/" + index + "?" + AbstractG1Device.jsonEntryIteratorToURLPar(data.fields()));
	}
	
	@Override
	public String getLabel() {
		return name.length() > 0 ? name : parent.getName();
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + (isOn ? "ON" : "OFF");
	}
}