package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.blu.BluTRV;

public class ScheduleManagerTRV {
	private final BluTRV trv;
	
	public ScheduleManagerTRV(BluTRV trv) {
		this.trv = trv;
	}
	
	public JsonNode getSchedules() throws IOException {
		return trv.getTRVJSON("TRV.ListScheduleRules", "{\"id\":0}").get("rules");
	}

	public String enable(int ruleId, boolean enable) {
		return trv.postTRVCommand("Trv.UpdateScheduleRule", "{\"id\":0,\"rule_id\":" + ruleId + ",\"rule\":{\"enable\":" + (enable ? "true" : "false") + "}}");
	}
	
	/**
	 * @param def
	 * @param enable
	 * @return the new id; < 0 in case of errpr
	 * @throws IOException 
	 */
	public int create(JsonNode def, boolean enable) throws IOException {
		((ObjectNode)def).put("enable", enable);
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", 0);
		out.set("rule", def);
		JsonNode res = trv.getTRVJSON("Trv.AddScheduleRule", out);
		return res.path("rule_id").asInt(-1);
	}
	
	public String update(int ruleId, JsonNode def) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", 0);
		out.put("rule_id", ruleId);
		out.set("rule", def);
		return trv.postTRVCommand("Trv.UpdateScheduleRule", out);
	}
	
	public String delete(int ruleId) {
		return trv.postTRVCommand("Trv.RemoveScheduleRule", "{\"id\":0,\"rule_id\":" + ruleId + "}");
	}
	
	public static void restore(BluTRV trv, JsonNode storedSchedule, List<String> errors) throws InterruptedException, IOException {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", Integer.parseInt(trv.getIndex()));
		// BluTrv.Call - TRV.RemoveScheduleRule
		JsonNode existingRules = trv.getJSON("/rpc/BluTrv.Call?id=" + trv.getIndex() + "&method=%22TRV.ListScheduleRules%22&params=%7B%22id%22:0%7D").get("rules");
		ObjectNode scheduleParams = JsonNodeFactory.instance.objectNode();
		scheduleParams.put("id", 0);
		out.put("method", "TRV.RemoveScheduleRule");
		for(JsonNode rule: existingRules) {
			scheduleParams.set("rule_id", rule.get("rule_id"));
			out.set("params", scheduleParams);
			errors.add(trv.postCommand("BluTrv.Call", out));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		}

		// BluTrv.Call - TRV.AddScheduleRule
		/*ObjectNode*/ scheduleParams = JsonNodeFactory.instance.objectNode();
		scheduleParams.put("id", 0);
		out.put("method", "TRV.AddScheduleRule");
		for(JsonNode rule: storedSchedule.get("rules")) {
			((ObjectNode)rule).remove("rule_id");
			scheduleParams.set("rule", rule);
			out.set("params", scheduleParams);
			errors.add( trv.postCommand("BluTrv.Call", out));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		}
	}
}