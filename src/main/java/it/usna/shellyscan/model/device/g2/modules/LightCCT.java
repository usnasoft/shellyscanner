package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.CCTInterface;
import tools.jackson.databind.JsonNode;

/**
 * CCTInterface implementation for gen 2+ devices.
 * Used by Pro RGBWW
 * @author usna
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
	private int minTemperature = 2700;
	private int maxTemperature = 6500;
	private final boolean fixRange;
	
	public LightCCT(AbstractG2Device parent, int index) {
		this.parent = parent;
		this.index = index;
		this.minBrightness = 0;
		this.fixRange = false;
	}
	
	public LightCCT(AbstractG2Device parent, int minTemperature, int maxTemperature, int index) {
		this.parent = parent;
		this.index = index;
		this.minBrightness = 0;
		this.minTemperature = minTemperature;
		this.maxTemperature = maxTemperature;
		this.fixRange = true;
	}

	@Override
	public int getMinBrightness() {
		return minBrightness;
	}

	public void fillSettings(JsonNode configCCT) {
		name = configCCT.get("name").asString("");
		if(fixRange == false) {
			final JsonNode ctRange = configCCT.get("ct_range");
			minTemperature = ctRange.get(0).asInt();
			maxTemperature = ctRange.get(1).asInt();
		}
	}
	
	public void fillStatus(JsonNode statusCCT) {
		isOn = statusCCT.get("output").asBoolean();
		brightness = statusCCT.get("brightness").intValue(0);
		temperature = statusCCT.get("ct").intValue(0);
		source = statusCCT.get("source").asString("-");
	}
	
	public void fillStatus(JsonNode statusCt, JsonNode input) {
		isOn = statusCt.get("output").asBoolean();
		brightness = statusCt.get("brightness").intValue(0);
		temperature = statusCt.get("ct").intValue(0);
		source = statusCt.get("source").asString("-");
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
		temperature = ct;
	}

	@Override
	public int getTemperature() {
		return temperature;
	}
	
	@Override
	public int getMinTemperature() {
		return minTemperature;
	}
	
	@Override
	public int getMaxTemperature() {
		return maxTemperature;
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
	
//	@Override
//	public AbstractG2Device getParent() {
//		return parent;
//	}
	
	@Override
	public String toString() {
		return getLabel() + ":" + brightness + (isOn ? "-ON" : "-OFF");
	}
}