package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class ScheduleManager {
	private final AbstractG2Device device;
	
	public ScheduleManager(AbstractG2Device device) {
		this.device = device;
	}
	
	public JsonNode getJobs() throws IOException {
		return device.getJSON("/rpc/Schedule.List").get("jobs");
	}
	
	public String enable(int id, boolean enable) {
		return device.postCommand("Schedule.Update", "{\"id\":" + id + ",\"enable\":" + (enable ? "true" : "false") + "}");
	}
	
	/**
	 * @param def
	 * @param enable
	 * @return the new id; < 0 in case of error
	 * @throws IOException 
	 */
	public int create(JsonNode def, boolean enable) throws IOException {
		((ObjectNode)def).put("enable", enable);
		JsonNode res = device.getJSON("Schedule.Create", def);
		return res.path("id").asInt(-1);
	}
	
	public String update(int id, JsonNode def) {
		((ObjectNode)def).put("id", id);
		return device.postCommand("Schedule.Update", def);
	}
	
	public String delete(int id) {
		return device.postCommand("Schedule.Delete", "{\"id\":" + id + "}");
	}
	
	// Remove all existing jobs and add stored ones
	public static void restore(AbstractG2Device parent, JsonNode schedule, final long delay, List<String> errors) throws InterruptedException {
		errors.add(parent.postCommand("Schedule.DeleteAll", "{}"));
		for(JsonNode sc: schedule.get("jobs")) {
			ObjectNode thisSc = sc.deepCopy();
			thisSc.remove("id");
			TimeUnit.MILLISECONDS.sleep(delay);
			errors.add(parent.postCommand("Schedule.Create", thisSc));
		}
	}
}