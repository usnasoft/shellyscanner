package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.WhiteInterface;

/**
 * Used by RGBW2 (white mode), Dimmer 1/2
 */
public class LightWhite implements WhiteInterface {
	private final AbstractG1Device parent;
	private String name;
	private boolean isOn;
	private int brightness;
	private String source;
	private String prefix; // "/white/<id>", "/light/<id>"
	private boolean inputIsOn;
	private final int minBrightness;
	
	public LightWhite(AbstractG1Device parent, final String command, int index) {
		this.parent = parent;
		this.prefix = command + index;
		this.minBrightness = 1;
	}
	
	public LightWhite(AbstractG1Device parent, final String command, int minBrightness, int index) {
		this.parent = parent;
		this.prefix = command + index;
		this.minBrightness = minBrightness;
	}
	
	@Override
	// On some devices 0 is not valid; e.g.: 0 is ok for RGBW2, not for dimmer (where min is 1)
	public int getMinBrightness() {
		return minBrightness;
	}

	public void fillSettings(JsonNode settingsWhite) {
		name = settingsWhite.get("name").asText("");
	}
	
	public void fillStatus(JsonNode statusWhite) {
		isOn = statusWhite.get("ison").asBoolean();
		brightness = statusWhite.get("brightness").asInt();
		source = statusWhite.get("source").asText("-");
	}
	
	public void fillStatus(JsonNode statusWhite, JsonNode statusInput) {
		isOn = statusWhite.get("ison").asBoolean();
		brightness = statusWhite.get("brightness").asInt();
		source = statusWhite.get("source").asText("-");
		inputIsOn = statusInput.get("input").asBoolean();
	}

	public String getName() {
		return name;
	}
	
	@Override
	public boolean toggle() throws IOException {
		final JsonNode w = parent.getJSON(prefix + "?turn=toggle");
		fillStatus(w);
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
		final JsonNode status = parent.getJSON(prefix + "?turn=" + (on ? "on" : "off"));
		fillStatus(status);
	}
	
	@Override
	public boolean isOn() {
		return isOn;
	}
	
	@Override
	public boolean isInputOn() {
		return inputIsOn;
	}

	@Override
	public void setBrightness(int b) throws IOException {
		final JsonNode status = parent.getJSON(prefix + "?brightness=" + b);
		fillStatus(status);
	}
	
	@Override
	public int getBrightness() {
		return brightness;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode data) throws IOException { //lights
		((ObjectNode)data).remove("ison");
//		((ObjectNode)data).remove("has_timer"); // not a parameter
//		((ObjectNode)data).remove("mode"); // duplicated (settings)
		
		String command = null;
		for(Entry<String, JsonNode> par: data.properties()) {
			if(command == null) {
				command = "/settings" + prefix + "?" + AbstractG1Device.jsonEntryToURLPar(par);
			} else {
				command += "&" + AbstractG1Device.jsonEntryToURLPar(par);
			}
		}
		return (command == null) ? null : parent.sendCommand(command);

//		Iterator<Entry<String, JsonNode>> pars = data.fields();
//		if(pars.hasNext()) {
//			String command = "/settings" + prefix + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			while(pars.hasNext()) {
//				command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			}
//			return parent.sendCommand(command);
//		}
//		return null;
	}
	
	@Override
	public String getLabel() {
		return (name == null || name.isEmpty()) ? parent.getName() : name;
	}
	
	@Override
	public AbstractG1Device getParent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + brightness + (isOn ? "-ON" : "-OFF");
	}
}