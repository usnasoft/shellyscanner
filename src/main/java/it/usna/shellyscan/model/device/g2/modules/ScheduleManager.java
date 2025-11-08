package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class ScheduleManager {
	private final AbstractG2Device device;
	
	public ScheduleManager(AbstractG2Device device) {
		this.device = device;
	}
	
	public JsonNode getJobs() throws IOException {
		return device.getJSON("/rpc/Schedule.List").get("jobs");
	}
	
	public String enable(int id, boolean enable) {
		return device.postCommand("Schedule.Update", "{\"id\":" + id + ",\"enable\":" + String.valueOf(enable) + "}");
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
	
	public String autoFWUpdate() throws IOException {
		Iterator<JsonNode> jobsIt = getJobs().iterator();
		while(jobsIt.hasNext()) {
			JsonNode scheduleNode = jobsIt.next();
			if(scheduleNode.get("enable").asBoolean()) {
				JsonNode calls = scheduleNode.path("calls");
				Iterator<JsonNode> callsIt = calls.iterator();
				while(callsIt.hasNext()) {
					JsonNode call = callsIt.next();
					// if(call.hasNonNull("origin")) systemJob = true;
					if(call.path("method").asString("").equalsIgnoreCase("Shelly.Update")) {
						return call.path("params").get("stage").asString();
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Remove all existing jobs and add stored ones; do nothing if backupJsons does not contains "Schedule.List.json"
	 */
	public static void restore(AbstractG2Device parent, Map<String, JsonNode> backupJsons, final long delay, List<String> errors) throws InterruptedException {
		JsonNode schedule = backupJsons.get("Schedule.List.json");
		if(schedule != null) { // some devices do not have Schedule.List +H&T
			TimeUnit.MILLISECONDS.sleep(delay);
			errors.add(parent.postCommand("Schedule.DeleteAll", "{}"));
			for(JsonNode sc: schedule.get("jobs")) {
				ObjectNode thisSc = (ObjectNode)sc.deepCopy();
				thisSc.remove("id");
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(parent.postCommand("Schedule.Create", thisSc));
			}
		}
	}
}