package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Used by RGBW Bulbs
 */
public class LightBulbRGB implements DeviceModule {
	private final AbstractG1Device parent;
	private final int index;
	private String name;
	private boolean isOn;
	private boolean modeColor;
	private int red; // 0..255
	private int green; // 0..255
	private int blue; // 0..255
//	private int white; // White brightness, 0..255, applies in mode="color"
	private int temp = 3000; //Color temperature in K, 3000..6500, applies in mode="white"
	private int brightness; // Brightness, 0..100, applies in mode="white"
	private int gain; // Gain for all channels, 0..100, applies in mode="color"
	private String source;
	
	public final static int MIN_TEMP = 3000;
	public final static int MAX_TEMP = 6500;
	
	public LightBulbRGB(AbstractG1Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	public void fillSettings(JsonNode settingslight) {
		name = settingslight.path("name").asText("");
	}
	
	public void fillStatus(JsonNode statuslight) {
		isOn = statuslight.get("ison").asBoolean();
		modeColor = "color".equals(statuslight.get("mode").asText());
		source = statuslight.get("source").asText("-");
//		if(modeColor) {
			red = statuslight.get("red").asInt();
			green = statuslight.get("green").asInt();
			blue = statuslight.get("blue").asInt();
			gain = statuslight.get("gain").asInt();
//		} else {
//			white = statuslight.get("white").asInt();
			temp = statuslight.get("temp").asInt();
			brightness = statuslight.get("brightness").asInt();
//		}
	}

	public String getName() {
		return name;
	}

	public boolean toggle() throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?turn=toggle");
		fillStatus(status);
		return isOn;
	}
	
	public void setColor(int r, int g, int b/*, int w*/) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?red=" + r + "&green=" + g + "&blue=" + b);
		fillStatus(status);
	}
	
	public int getRed() {
		return red;
	}
	
	public int getGreen() {
		return green;
	}
	
	public int getBlue() {
		return blue;
	}
	
//	public int getWhite() {
//		return white;
//	}
	
	public boolean isColorMode() {
		return modeColor;
	}
	
	public void setColorMode(boolean color) throws IOException {
//		final JsonNode status = parent.getJSON("/light/" + index + "?mode=" + (color ? "color" : "white"));
//		refresh(status);
		final JsonNode setting = parent.getJSON("/settings?mode=" + (color ? "color" : "white"));
//		refresh(setting.get("lights").get(0)); // "mode" missing
		modeColor = "color".equals(setting.get("mode").asText());
	}

	public void setBrightness(int b) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?brightness=" + b);
		fillStatus(status);
	}
	
	public void setGain(int b) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?gain=" + b);
		fillStatus(status);
	}
	
	public void setTemp(int t) throws JsonProcessingException, IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?temp=" + t);
		fillStatus(status);
	}
	
	public boolean isOn() {
		return isOn;
	}
	
	public int getBrightness() {
		return brightness;
	}
	
	public int getGain() {
		return gain;
	}
	
	public int getTemp() {
		return temp;
	}
	
//	public void refresh() throws IOException {
//		fillStatus(parent.getJSON("/light/" + index));
//	}
	
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode data) throws IOException {
		((ObjectNode)data).remove("ison");
		((ObjectNode)data).remove("has_timer"); // maybe not present
		((ObjectNode)data).remove("mode"); // duplicated (settings)

		Iterator<Entry<String, JsonNode>> pars = data.fields();
		if(pars.hasNext()) {
			String command = "/settings/light/" + index + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
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
		return getLabel() + "-" + (modeColor ? gain : brightness) + (isOn ? "-ON" : "-OFF");
	}
}