package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.WhiteInterface;

/**
 * Used by wall dimmer, dimmer 0/1-10
 */
public class LightWhite implements WhiteInterface {
	private final AbstractG2Device parent;
	private final int index;
	private String name;
	private boolean isOn;
	private int brightness;
//	private int minB = 0;
//	private int maxB = 1000;
	private String source;
	private boolean inputIsOn;
	
	public LightWhite(AbstractG2Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	@Override
	public int getMinBrightness() {
		return /*minB*/0;
	}

	@Override
	public int getMaxBrightness() {
		return /*maxB*/100;
	}

	public void fillSettings(JsonNode settingsWhite) {
		name = settingsWhite.get("name").asText("");
//		JsonNode range = settingsWhite.get("range_map");
//		if(range != null) {
//			minB = range.get(0).intValue();
//			maxB = range.get(1).intValue();
//		}
	}
	
	public void fillStatus(JsonNode statusWhite) {
		isOn = statusWhite.get("output").asBoolean();
		brightness = statusWhite.get("brightness").intValue();
		source = statusWhite.get("source").asText("-");
	}
	
	public void fillStatus(JsonNode statusWhite, JsonNode input) {
		isOn = statusWhite.get("output").asBoolean();
		brightness = statusWhite.get("brightness").intValue();
		source = statusWhite.get("source").asText("-");
		inputIsOn = input.get("state").asBoolean();
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public boolean toggle() throws IOException {
		parent.getJSON("/rpc/Light.Toggle?id=" + index);
		return (isOn = ! isOn);
	}
	
	@Override
	public void change(boolean on) throws IOException {
		parent.getJSON("/rpc/Light.Set?id=" + index + "&on=" + on);
		isOn = on;
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
	
	public String restore(JsonNode config) {
		return parent.postCommand("Light.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "light", index));
	}
	
	@Override
	public String getLabel() {
		return (name != null && name.length() > 0) ? name : parent.getName();
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + brightness + (isOn ? "-ON" : "-OFF");
	}
}