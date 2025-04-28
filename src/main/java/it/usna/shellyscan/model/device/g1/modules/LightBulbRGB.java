package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.CCTInterface;
import it.usna.shellyscan.model.device.modules.RGBInterface;

/**
 * Used by RGBW Bulbs
 */
public class LightBulbRGB implements CCTInterface, RGBInterface {
	private final AbstractG1Device parent;
	private final int index;
	private String name = "";
	private boolean isOn;
	private boolean modeColor;
	private int red; // 0..255
	private int green; // 0..255
	private int blue; // 0..255
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
			red = statuslight.get("red").intValue();
			green = statuslight.get("green").intValue();
			blue = statuslight.get("blue").intValue();
			gain = statuslight.get("gain").intValue();
//		} else {
			temp = statuslight.get("temp").intValue();
			brightness = statuslight.get("brightness").intValue();
//		}
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean toggle() throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?turn=toggle");
		fillStatus(status);
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
		final JsonNode status = parent.getJSON("/light/?turn=" + (on ? "on" : "off"));
		fillStatus(status);
	}
	
	@Override
	public void setColor(int r, int g, int b/*, int w*/) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?red=" + r + "&green=" + g + "&blue=" + b);
		fillStatus(status);
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
	// CCTInterface
	public int getMinBrightness() {
		return 0;
	}
	
	@Override
	// CCTInterface
	public int getMinTemperature() {
		return MIN_TEMP;
	}

	@Override
	public boolean isInputOn() {
		return false;
	}

	@Override
	public ShellyAbstractDevice getParent() {
		return parent;
	}
	
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

	@Override
	public void setBrightness(int b) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?brightness=" + b);
		fillStatus(status);
	}
	
	public void setGain(int b) throws IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?gain=" + b);
		fillStatus(status);
	}
	
	@Override
	public void setTemperature(int t) throws JsonProcessingException, IOException {
		final JsonNode status = parent.getJSON("/light/" + index + "?temp=" + t);
		fillStatus(status);
	}
	
	@Override
	public boolean isOn() {
		return isOn;
	}
	
	@Override
	public int getBrightness() {
		return brightness;
	}
	
	@Override
	public int getGain() {
		return gain;
	}
	
	@Override
	public int getTemperature() {
		return temp;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode data) throws IOException {
		((ObjectNode)data).remove("ison");
		((ObjectNode)data).remove("has_timer"); // maybe not present
		((ObjectNode)data).remove("mode"); // duplicated (settings)

		String command = null;
		for(Entry<String, JsonNode> par: data.properties()) {
			if(command == null) {
				command = "/settings/light/" + index + "?" + AbstractG1Device.jsonEntryToURLPar(par);
			} else {
				command += "&" + AbstractG1Device.jsonEntryToURLPar(par);
			}
		}
		return (command == null) ? null : parent.sendCommand(command);
		
//		Iterator<Entry<String, JsonNode>> pars = data.fields();
//		if(pars.hasNext()) {
//			String command = "/settings/light/" + index + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			while(pars.hasNext()) {
//				command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
//			}
//			return parent.sendCommand(command);
//		}
//		return null;
	}
	
	@Override
	public String getLabel() {
		return name.isEmpty() ? parent.getName() : name;
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + (modeColor ? gain : brightness) + (isOn ? "-ON" : "-OFF");
	}
}