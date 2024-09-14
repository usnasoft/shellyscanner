package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.RGBWInterface;

/**
 * Used by RGBW2 (color mode)
 */
public class LightRGBW implements RGBWInterface {
	private final AbstractG1Device parent;
	private final int index;
	private boolean isOn;
	private int red; // 0..255
	private int green; // 0..255
	private int blue; // 0..255
	private int white; // 0..255
	private int gain; // Gain for all channels, 0..100
	private String source;
	
	public LightRGBW(AbstractG1Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	public void fillStatus(JsonNode statusColor) {
		refresh(statusColor);
	}
	
	private void refresh(JsonNode statusColor) {
		isOn = statusColor.get("ison").asBoolean();
		red = statusColor.get("red").asInt();
		green = statusColor.get("green").asInt();
		blue = statusColor.get("blue").asInt();
		white = statusColor.get("white").asInt();
		gain = statusColor.get("gain").asInt();
		source = statusColor.get("source").asText("-");
	}
	
	@Override
	public boolean toggle() throws IOException {
		final JsonNode color = parent.getJSON("/color/" + index + "?turn=toggle");
		refresh(color);
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
		final JsonNode color = parent.getJSON("/color/" + index + "?turn=" + (on ? "on" : "off"));
		refresh(color);
	}
	
	@Override
	public boolean isOn() {
		return isOn;
	}
	
	@Override
	public int getRed() {
		return red;
	}
	
	@Override
	public int getGreen() {
		return green;
	}
	
	@Override
	public int getBlue() {
		return blue;
	}
	
	@Override
	public void setWhite(int w) throws IOException {
		final JsonNode status = parent.getJSON("/color/" + index + "?white=" + w);
		refresh(status);
	}
	
	@Override
	public int getWhite() {
		return white;
	}
	
	@Override
	public void setColor(int r, int g, int b, int w) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?red=" + r + "&green=" + g + "&blue=" + b + "&white=" + w);
		refresh(status);
	}
	

	@Override
	public void setColor(int r, int g, int b) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?red=" + r + "&green=" + g + "&blue=" + b);
		refresh(status);
	}
	
	public void refresh() throws IOException {
		refresh(parent.getJSON("/color/" + index));
	}
	
	@Override
	public void setGain(int b) throws IOException {
		final JsonNode status = parent.getJSON("/color/" + index + "?gain=" + b);
		refresh(status);
	}
	
	@Override
	public int getGain() {
		return gain;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode data) throws IOException {
		((ObjectNode)data).remove("ison");
//		((ObjectNode)data).remove("has_timer"); // not a parameter
//		((ObjectNode)data).remove("mode"); // duplicated (settings)

		Iterator<Entry<String, JsonNode>> pars = data.fields();
		if(pars.hasNext()) {
			String command = "/settings/color/" + index + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
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
		return parent.getName();
	}
	
	@Override
	public String toString() {
		return getLabel() + /*"-" + gain +*/ (isOn ? "-ON" : "-OFF");
	}
}