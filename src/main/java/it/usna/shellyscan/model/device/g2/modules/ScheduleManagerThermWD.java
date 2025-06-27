package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.WallDisplay;

/**
 * Thermostat ScheduleManager for Wall Display also managing profiles
 */
public class ScheduleManagerThermWD {
	private final static String THERM_ID = "0";
	private final WallDisplay wd;
	
	public ScheduleManagerThermWD(WallDisplay device) {
		this.wd = device;
	}

	// Profiles -->
	public List<ThermProfile> getProfiles() throws IOException {
		ArrayList<ThermProfile> ret = new ArrayList<>();
		wd.getJSON("/rpc/Thermostat.Schedule.ListProfiles?id=" + THERM_ID).path("profiles").forEach(node -> {
			ret.add(new ThermProfile(node.get("id").intValue(), node.get("name").textValue()));
		});
		return ret;
	}

	public String deleteProfiles(int id) {
		return wd.postCommand("Thermostat.Schedule.DeleteProfile", "{\"id\":" + THERM_ID + ",\"profile_id\":" + id + "}");
	}

	public String renameProfiles(int id, String newName) {
		return wd.postCommand("Thermostat.Schedule.RenameProfile", "{\"id\":" + THERM_ID + ",\"profile_id\":" + id + ",\"name\":\"" + newName + "\"}");
	}

	public int addProfiles(String name) throws IOException {
		JsonNode ret = wd.getJSON("Thermostat.Schedule.AddProfile", "{\"id\":" + THERM_ID + ",\"name\":\"" + name + "\"}");
		return ret.get("profile_id").intValue();
	}

	// Rules (Thermostat) -->
	
	// todo test
	public JsonNode getRules() throws IOException {
		return wd.getJSON("TThermostat.Schedule.ListRules").get("rules");
	}
	
	// todo test
	public String enable(int ruleId, boolean enable) {
		return wd.postCommand("Thermostat.Schedule.UpdateRule", "{\"id\":0,\"rule_id\":" + ruleId + ",\"rule\":{\"enable\":" + (enable ? "true" : "false") + "}}");
	}
	
	/**
	 * @param def
	 * @param enable
	 * @return the new id; < 0 in case of error
	 * @throws IOException 
	 */
	// todo test
	public int create(JsonNode def, boolean enable) throws IOException {
		((ObjectNode)def).put("enable", enable);
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.set("rule", def);
		JsonNode res = wd.getJSON("Thermostat.Schedule.CreateRule", out);
		return res.path("rule_id").asInt(-1);
	}
	
	// todo test
	public String update(int ruleId, JsonNode def) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("rule_id", ruleId);
		out.set("rule", def);
		return wd.postCommand("Thermostat.Schedule.UpdateRule", out);
	}
	
	// todo test
	public String delete(int ruleId) {
		return wd.postCommand("Thermostat.Schedule.DeleteRule", "{\"id\":0,\"rule_id\":" + ruleId + "}");
	}
	
	public record ThermProfile(int id, String name) {};
}