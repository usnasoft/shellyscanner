package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.RollerInterface;

/**
 * Used by +2PM
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
	
	public String restore(JsonNode config) {
		JsonNodeFactory factory = new JsonNodeFactory(false);
		ObjectNode out = factory.objectNode();
		out.put("id", index);
		ObjectNode sw = (ObjectNode)config.get("cover:" + index).deepCopy();
		sw.remove("id");
		out.set("config", sw);
		return parent.postCommand("Cover.SetConfig", out);
	}
	
	@Override
	public String getLabel() {
		return name.length() > 0 ? name : parent.getName();
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