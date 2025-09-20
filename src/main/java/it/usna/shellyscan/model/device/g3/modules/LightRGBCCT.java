package it.usna.shellyscan.model.device.g3.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.DeviceAPIException;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.RGBCCTInterface;

public class LightRGBCCT implements RGBCCTInterface {
	private static final int INDEX = 0;
	private final AbstractG2Device parent;
	private boolean colorMode;
	private String name;
	private boolean isOn;
	private String source;
	// rgb
	private int red; // 0..255
	private int green; // 0..255
	private int blue; // 0..255
	private int brightness; // Gain - 0..100
	//cct
	private int temperature;
	private static final int MIN_TEMP = 2700;
	private static final int MAX_TEMP = 6500;
	private static final int MIN_BRIGHTNESS = 0;
	
	public LightRGBCCT(AbstractG2Device parent /*, int index*/) {
		this.parent = parent;
	}
	
	@Override
	public boolean isColorMode() {
		return colorMode;
	}

	@Override
	public void setColorMode(boolean color) throws IOException {
		String ret = parent.postCommand("RGBCCT.SetConfig", "{\"id\":" + INDEX + ",\"config\":{\"mode\":\"" + (color ? "rgb" : "cct") + "\"}}");
		if(ret == null) {
			colorMode = color;
		} else {
			throw new DeviceAPIException(DeviceAPIException.UNAVAILABLE, ret);
		}
//		parent.getJSON("/rpc/RGBCCT.SetConfig?id=" + INDEX + "&config={\"mode\":\"" + (color ? "rgb" : "cct") + "\"}");
		colorMode = color;
	}
	
	public void fillSettings(JsonNode config) {
		name = config.get("name").asText("");
		colorMode = "rgb".equals(config.get("mode").textValue()); // Range of values: rgb, cct
	}
	
	public void fillStatus(JsonNode statusRGBCCT) {
		isOn = statusRGBCCT.get("output").asBoolean();
		final JsonNode rgbNode = statusRGBCCT.get("rgb");
		red = rgbNode.get(0).asInt();
		green = rgbNode.get(1).asInt();
		blue = rgbNode.get(2).asInt();
		brightness = statusRGBCCT.get("brightness").asInt();
		
		temperature = statusRGBCCT.get("ct").intValue();
		
		source = statusRGBCCT.get("source").asText("-");
	}

	@Override
	public String getLabel() {
		return (name == null || name.isEmpty()) ? parent.getName() : name;
	}

	@Override
	public boolean toggle() throws IOException {
		change(! isOn);
		return isOn;
	}

	@Override
	public void change(boolean on) throws IOException {
		parent.getJSON("/rpc/RGBCCT.Set?id=" + INDEX + "&on=" + on);
		isOn = on;
		source = Devices.SCANNER_AGENT;
	}

	@Override
	public boolean isOn() {
		return isOn;
	}
	
	@Override
	public boolean isInputOn() {
		return false;
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
	public void setColor(int r, int g, int b) throws IOException {
		parent.getJSON("/rpc/RGBCCT.Set?id=" + INDEX + "&rgb=[" + r + "," + g + "," + b + "]");
		red = r;
		green = g;
		blue = b;
	}

	@Override
	public void setGain(int b) throws IOException {
		parent.getJSON("/rpc/RGBCCT.Set?id=" + INDEX + "&brightness=" + b);
		brightness = b;
	}

	@Override
	public int getGain() {
		return brightness;
	}
	
	@Override
	// Brightness and Gain are separate entities on bulbs gen 1
	public void setBrightness(int b) throws IOException {
		setGain(b);
	}
	
	@Override
	// Brightness and Gain are separate entities on bulbs gen 1
	public int getBrightness() {
		return getGain();
	}
	
	@Override
	public void setTemperature(int ct) throws IOException {
		parent.getJSON("/rpc/RGBCCT.Set?id=" + INDEX + "&ct=" + ct);
		temperature = ct;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}
	
	@Override
	public int getMinTemperature() {
		return MIN_TEMP;
	}
	
	@Override
	public int getMaxTemperature() {
		return MAX_TEMP;
	}
	
	@Override
	public int getMinBrightness() {
		return MIN_BRIGHTNESS;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode config) {
		return parent.postCommand("RGBCCT.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "rgbcct", INDEX));
	}
	
//	@Override
//	public AbstractG2Device getParent() {
//		return parent;
//	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + brightness + (isOn ? "-ON" : "-OFF");
	}
}