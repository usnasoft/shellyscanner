package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.CCTInterface;

/**
 * Used by wall dimmer, dimmer 0/1-10, RGBW, ...
 */
public class LightCCT implements CCTInterface {
	private final AbstractG2Device parent;
	private final int index;
	private String name;
	private boolean isOn;
	private int brightness;
	private int temperature;
	private String source;
	private boolean inputIsOn;
	private final int minBrightness;
	
	public LightCCT(AbstractG2Device parent, int index) {
		this.parent = parent;
		this.index = index;
		this.minBrightness = 0;
	}
	
	public LightCCT(AbstractG2Device parent, int minBrightness, int index) {
		this.parent = parent;
		this.index = index;
		this.minBrightness = minBrightness;
	}
	
	@Override
	public int getMinBrightness() {
		return minBrightness;
	}

	@Override
	public int getMaxBrightness() {
		return /*maxB*/100;
	}

	public void fillSettings(JsonNode settingsCt) {
		name = settingsCt.get("name").asText("");
	}
	
	public void fillStatus(JsonNode statusCt) {
		isOn = statusCt.get("output").asBoolean();
		brightness = statusCt.get("brightness").intValue();
		temperature = statusCt.get("ct").intValue();
		source = statusCt.get("source").asText("-");
	}
	
	public void fillStatus(JsonNode statusCt, JsonNode input) {
		isOn = statusCt.get("output").asBoolean();
		brightness = statusCt.get("brightness").intValue();
		temperature = statusCt.get("ct").intValue();
		source = statusCt.get("source").asText("-");
		inputIsOn = input.get("state").asBoolean();
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public boolean toggle() throws IOException {
		change(! isOn);
		return isOn;
	}
	
	@Override
	public void change(boolean on) throws IOException {
		parent.getJSON("/rpc/CCT.Set?id=" + index + "&on=" + on);
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
		parent.getJSON("/rpc/CCT.Set?id=" + index + "&brightness=" + b);
		brightness = b;
	}
	
	@Override
	public int getBrightness() {
		return brightness;
	}
	

	@Override
	public void setTemperature(int ct) throws IOException {
		parent.getJSON("/rpc/CCT.Set?id=" + index + "&ct=" + ct);
		brightness = ct;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	public String restore(JsonNode config) {
		return parent.postCommand("CCT.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "cct", index));
	}
	
	@Override
	public String getLabel() {
		return (name == null || name.isEmpty()) ? parent.getName() : name;
	}
	
	@Override
	public AbstractG2Device getParent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return getLabel() + ":" + brightness + (isOn ? "%-ON" : "%-OFF");
	}
}