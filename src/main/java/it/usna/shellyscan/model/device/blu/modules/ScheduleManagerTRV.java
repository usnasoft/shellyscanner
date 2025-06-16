package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.blu.BluTRV;

public class ScheduleManagerTRV {
	private final BluTRV device;
	
	public ScheduleManagerTRV(BluTRV device) {
		this.device = device;
	}
	
	public JsonNode getSchedules() throws IOException {
		return device.getJSON("/rpc/Trv.ListScheduleRules").get("jobs"); // ???
	}
	
	public String enable(int id, boolean enable) {
		return device.postCommand("Trv.UpdateScheduleRule", "{\"rule_id\":" + id + ",\"enable\":" + (enable ? "true" : "false") + "}"); // todo id
	}
	
	/**
	 * @param def
	 * @param enable
	 * @return the new id; < 0 in case of errpr
	 * @throws IOException 
	 */
	public int create(JsonNode def, boolean enable) throws IOException {
		//todo
//		((ObjectNode)def).put("enable", enable);
//		JsonNode res = device.getJSON("Trv.AddScheduleRule", def);
//		return res.path("id").asInt(-1);
		return -1;
	}
	
	public String update(int id, JsonNode def) {
		((ObjectNode)def).put("rule_id", id);
		 // todo id
		return device.postCommand("Trv.UpdateScheduleRule", def);
	}
	
	public String delete(int id) {
		return device.postCommand("Trv.RemoveScheduleRule", "{\"rule_id\":" + id + "}"); // todo id
	}
}