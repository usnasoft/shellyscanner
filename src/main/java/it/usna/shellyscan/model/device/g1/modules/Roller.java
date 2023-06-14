package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.RollerInterface;

/**
 * Used by 2; 2.5
 */
public class Roller implements RollerInterface {
	private final AbstractG1Device parent;
	private final int index;
	private boolean calibrated;
	private int position;
	private String source;
	
	public Roller(AbstractG1Device parent, int index) {
		this.parent = parent;
		this.index = index;
	}
	
	public void fillStatus(JsonNode rollerStatus) {
		calibrated = rollerStatus.get("positioning").asBoolean();
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
	
	public String restore(JsonNode data) throws IOException {
		((ObjectNode)data).remove("state");
		((ObjectNode)data).remove("power");
		((ObjectNode)data).remove("is_valid");
		((ObjectNode)data).remove("safety_switch");
		((ObjectNode)data).remove("is_valid");
//		((ObjectNode)data).remove("positioning");
		Iterator<Entry<String, JsonNode>> pars = data.fields();
		if(pars.hasNext()) {
			String command = "/settings/roller/" + index + "?" + AbstractG1Device.jsonEntryToURLPar(pars.next());
			while(pars.hasNext()) {
				command += "&" + AbstractG1Device.jsonEntryToURLPar(pars.next());
			}
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
		if(calibrated) {
			return getLabel() + "-" + position;
		} else {
			return getLabel() + "-n.c.";
		}
	}
}