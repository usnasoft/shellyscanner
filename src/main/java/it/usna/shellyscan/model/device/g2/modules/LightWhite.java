package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.LightWhiteInterface;

/**
 * Used by wall dimmer
 */
public class LightWhite implements LightWhiteInterface {
	private final AbstractG2Device parent;
	private String name;
	private boolean isOn;
	private int brightness; // Brightness, 1..100 - seems wrong: 0 seems valid for RGBW2, not for dimmer
	private String source;
	private int index;
	private boolean inputIsOn;
//	public final static int MIN_BRIGHTNESS = 0;
	
	public LightWhite(AbstractG2Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}

	public void fillSettings(JsonNode settingsWhite) {
		name = settingsWhite.get("name").asText("");
	}
	
	public void fillStatus(JsonNode statusWhite) {
		isOn = statusWhite.get("output").asBoolean();
		brightness = statusWhite.get("brightness").asInt();
		source = statusWhite.get("source").asText("-");
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public boolean toggle() throws IOException {
//		final JsonNode w = parent.getJSON(prefix + "?turn=toggle");
//		fillStatus(w);
		parent.getJSON("/rpc/Light.Toggle?id=" + index);
		isOn = !isOn;
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {//Light.Set?id=0&on=true&brightness=50
//		final JsonNode color = parent.getJSON(prefix + "?turn=" + (on ? "on" : "off"));
//		fillStatus(color);
		parent.getJSON("/rpc/Light.Set?id=" + index + "&on=" + on);
		isOn = on;
	}
	
	@Override
	public boolean isOn() {
		return isOn;
	}
	
	public boolean isInputOn() {
		return inputIsOn;
	}

	@Override
	public void setBrightness(int b) throws IOException {
//		final JsonNode status = parent.getJSON(prefix + "?brightness=" + b);
//		fillStatus(status);
		parent.getJSON("/rpc/Light.Set?id=" + index + "&brightness=" + b);
		brightness = b;
	}
	
	@Override
	public int getBrightness() {
		return brightness;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
//	public String restore(JsonNode data) throws IOException { //lights
//		((ObjectNode)data).remove("ison");
////		((ObjectNode)data).remove("has_timer"); // not a parameter
////		((ObjectNode)data).remove("mode"); // duplicated (settings)
//
//		Iterator<Entry<String, JsonNode>> pars = data.fields();
//		if(pars.hasNext()) {
//			String command = "/settings" + prefix + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			while(pars.hasNext()) {
//				command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			}
////			System.out.println(command);
//			return parent.sendCommand(command);
//		}
//		return null;
//	}
	
	@Override
	public String getLabel() {
		return name.length() > 0 ? name : parent.getName();
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + brightness + (isOn ? "-ON" : "-OFF");
	}
}