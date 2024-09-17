package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.RGBWInterface;

public class LightRGBW implements RGBWInterface {
	private final AbstractG2Device parent;
	private final int index;
	private String name;
	private boolean isOn;
	private int red; // 0..255
	private int green; // 0..255
	private int blue; // 0..255
	private int white; // 0..255
	private int gain; // Gain for all channels, 0..100
	private String source;
	private boolean inputIsOn;
	
	public LightRGBW(AbstractG2Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	public void fillConfig(JsonNode config) {
		name = config.get("name").asText("");
	}
	
	public void fillStatus(JsonNode statusColor) {
		isOn = statusColor.get("output").asBoolean();
		final JsonNode rgbNode = statusColor.get("rgb");
		red = rgbNode.get(0).asInt();
		green = rgbNode.get(1).asInt();
		blue = rgbNode.get(2).asInt();
		white = statusColor.get("white").asInt();
		gain = statusColor.get("brightness").asInt();
		source = statusColor.get("source").asText("-");
	}
	
	public void fillStatus(JsonNode statusColor, JsonNode input) {
		fillStatus(statusColor);
		inputIsOn = input.get("state").asBoolean();
	}

	@Override
	public String getLabel() {
		return (name != null && name.length() > 0) ? name : parent.getName();
	}

	@Override
	public boolean toggle() throws IOException {
		/*final JsonNode resp =*/ parent.getJSON("/rpc/RGBW.Toggle?id=" + index); // no return value
//		isOn = resp.get("was_on").asBoolean() == false;
		change(! isOn);
		return isOn;
	}

	@Override
	public void change(boolean on) throws IOException {
		parent.getJSON("/rpc/RGBW.Set?id=" + index + "&on=" + on);
		isOn = on;
		source = Devices.SCANNER_AGENT;
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
	public int getWhite() {
		return white;
	}

	@Override
	public void setWhite(int w) throws IOException {
		parent.getJSON("/rpc/RGBW.Set?id=" + index + "&white=" + w);
		white = w;
	}

	@Override
	public void setColor(int r, int g, int b, int w) throws IOException {
		parent.getJSON("/rpc/RGBW.Set?id=" + index + "&white=" + w + "&rgb=[" + r + "," + g + "," + b + "]");
		red = r;
		green = g;
		blue = b;
		white = w;
	}
	
	@Override
	public void setColor(int r, int g, int b) throws IOException {
		parent.getJSON("/rpc/RGBW.Set?id=" + index + "&rgb=[" + r + "," + g + "," + b + "]");
		red = r;
		green = g;
		blue = b;
	}

	@Override
	public void setGain(int b) throws IOException {
		parent.getJSON("/rpc/RGBW.Set?id=" + index + "&brightness=" + b);
		gain = b;
	}

	@Override
	public int getGain() {
		return gain;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode config) {
		return parent.postCommand("RGBW.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "rgbw", index));
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + gain + (isOn ? "-ON" : "-OFF");
	}
}
