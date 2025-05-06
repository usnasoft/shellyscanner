package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class ScheduleManager {
	private final AbstractG2Device device;
	
	public ScheduleManager(AbstractG2Device device) {
		this.device = device;
	}
	
	public JsonNode getSchedules() throws IOException {
		return device.getJSON("/rpc/Schedule.List").get("jobs");
	}
	
	public String enable(int id, boolean enable) {
		return device.postCommand("Schedule.Update", "{\"id\":" + id + ",\"enable\":" + (enable ? "true" : "false") + "}");
	}
	
	/**
	 * @param def
	 * @param enable
	 * @return the new id
	 * @throws IOException 
	 */
	public int create(ObjectNode def, boolean enable) throws IOException {
//		todo test
		def.put("enable", enable);
		JsonNode res = device.getJSON("Schedule.Create", def);
		return res.get("id").asInt(-1);
	}
	
	public String update(int id, JsonNode def) {
//		todo
		return null;
	}
	
	public String delete(int id) {
//		todo test
		return device.postCommand("Schedule.Delete", "{\"id\":" + id + "}");
	}
}
