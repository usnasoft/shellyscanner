package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.RollerInterface;

/**
 * Cover - used by +2PM, pro 2PM
 */
public class Roller implements RollerInterface {
	private final AbstractG2Device parent;
	private String name;
	private final int index;
	private boolean calibrated;
	private int position;
	private String source;
	
	public Roller(AbstractG2Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	public void fillSettings(JsonNode configuration) {
		name = configuration.get("name").asText("");
	}
	
	public void fillStatus(JsonNode rollerStatus) {
		calibrated = rollerStatus.get("pos_control").asBoolean();
		if(calibrated) {
			position = rollerStatus.get("current_pos").asInt();
		}
		source = rollerStatus.get("source").asText("-");
	}
	
	@Override
	public boolean isCalibrated() {
		return calibrated;
	}
	
	@Override
	public int getPosition() {
		return position;
	}
	
	@Override
	public void setPosition(int pos) throws IOException {
		final JsonNode roller = parent.getJSON("/roller/" + index + "?go=to_pos&roller_pos=" + pos);
		position = roller.get("current_pos").asInt();
		source = roller.get("source").asText("-");
	}
	
	@Override
	public void open() throws IOException {
		final JsonNode roller = parent.getJSON("/roller/" + index + "?go=open");
		position = 100;
		source = roller.get("source").asText("-");
	}
	
	@Override
	public void close() throws IOException {
		final JsonNode roller = parent.getJSON("/roller/" + index + "?go=close");
		position = 0;
		source = roller.get("source").asText("-");
	}
	
	@Override
	public void stop() throws IOException {
		final JsonNode roller = parent.getJSON("/roller/" + index + "?go=stop");
		if(calibrated) {
			position = roller.get("current_pos").asInt();
		}
		source = roller.get("source").asText("-");
	}
	
	@Override
	public String getLastSource() {
		return source;
	}
	
	@Override
	public String getLabel() {
		return (name == null || name.isEmpty()) ? parent.getName() : name;
	}
	
	public String restore(JsonNode config) {
		return parent.postCommand("Cover.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "cover", index));
	}
	
	@Override
	public String toString() {
		if(calibrated) {
			return getLabel() + "-" + position;
		} else {
			return getLabel() + "-n.c.";
		}
	}
}