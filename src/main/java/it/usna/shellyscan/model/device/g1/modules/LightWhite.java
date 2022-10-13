package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Used by RGBW2 (white mode), Dimmer 1/2
 */
public class LightWhite implements DeviceModule {
	private final AbstractG1Device parent;
//	private final int index;
	private String name;
	private boolean isOn;
	private int brightness; // Brightness, 1..100
	private String source;
	private String prefix; // "/white/", "/light/"
	private boolean inputIsOn;
	
	public LightWhite(AbstractG1Device parent, final String command, int index) {
		this.parent = parent;
//		this.index = index;
		this.prefix = command + index;
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
	
	public boolean toggle() throws IOException {
		final JsonNode w = parent.getJSON(prefix + "?turn=toggle");
		fillStatus(w);
		return isOn;
	}
	
	public void change(boolean on) throws IOException {
		final JsonNode color = parent.getJSON(prefix + "?turn=" + (on ? "on" : "off"));
		fillStatus(color);
	}
	
	public boolean isOn() {
		return isOn;
	}
	
	public boolean isInputOn() {
		return inputIsOn;
	}
	
//	public void refresh() throws IOException {
//		fillStatus(parent.getJSON(prefix));
//	}

	public void setBrightness(int b) throws IOException {
		final JsonNode status = parent.getJSON(prefix + "?brightness=" + b);
		fillStatus(status);
	}
	
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

		Iterator<Entry<String, JsonNode>> pars = data.fields();
		if(pars.hasNext()) {
			String command = "/settings" + prefix + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
			while(pars.hasNext()) {
				command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
			}
//			System.out.println(command);
			return parent.sendCommand(command);
		}
		return null;
	}
	
	@Override
	public String getLabel() {
		return name.length() > 0 ? name : parent.getName();
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + brightness + (isOn ? "-ON" : "-OFF");
	}
}