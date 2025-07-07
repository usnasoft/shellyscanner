package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

	public String deleteProfiles(int profileId) {
		return wd.postCommand("Thermostat.Schedule.DeleteProfile", "{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + "}");
	}

	public String renameProfiles(int profileId, String newName) {
		return wd.postCommand("Thermostat.Schedule.RenameProfile", "{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + ",\"name\":\"" + newName + "\"}");
	}

	public int addProfiles(String name) throws IOException {
		JsonNode ret = wd.getJSON("Thermostat.Schedule.AddProfile", "{\"id\":" + THERM_ID + ",\"name\":\"" + name + "\"}");
		return ret.get("profile_id").intValue();
	}

	// Rules (Thermostat) -->
	
	public List<Rule>getRules(int profileId) throws IOException {
		ArrayList<Rule> rules = new ArrayList<>();
		JsonNode r = wd.getJSON("Thermostat.Schedule.ListRules", "{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + "}").get("rules");
		for(JsonNode rule: r) {
			rules.add(new Rule(rule.get("rule_id").textValue(), rule.get("target_C").floatValue(), /*CronUtils.fragStrToNum(*/rule.get("timespec").textValue(), rule.path("enable").booleanValue()));
		}
		return rules;
	}
	
	public String enable(String ruleId, int profileId, boolean enable) {
		 return wd.postCommand("Thermostat.Schedule.UpdateRule", "{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + ",\"config\":{\"rule_id\":\"" + ruleId + "\",\"enable\":" + (enable ? "true" : "false") + "}}");
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
	public String update(String ruleId, JsonNode def) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("rule_id", ruleId);
		out.set("rule", def);
		return wd.postCommand("Thermostat.Schedule.UpdateRule", out);
	}
	
	// todo test
	public String delete(String ruleId, int profileId) {
		return wd.postCommand("Thermostat.Schedule.DeleteRule", "{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + ",\"rule_id\":\"" + ruleId + "\"}");
	}
	
	public static void restore(WallDisplay parent, Map<String, JsonNode> backup, final long delay, List<String> errors) throws InterruptedException {
		// todo delete existing
		JsonNode profilesNode = backup.get("Thermostat.Schedule.ListProfiles.json").path("profiles");
		for(JsonNode profile: profilesNode) {
			//todo
		}
	}
	
	public record ThermProfile(int id, String name) {}
	
	public static class Rule {
		private String ruleId;
		private Float target;
		private String timespec;
		private boolean enabled;
		
		public Rule(String ruleId, Float target, String timespec, boolean enabled) {
			this.ruleId = ruleId;
			this.target = target;
			this.timespec = timespec;
			this.enabled = enabled;
		}
		public String getRuleId() {
			return ruleId;
		}
		public void setRuleId(String ruleId) {
			this.ruleId = ruleId;
		}
		public Float getTarget() {
			return target;
		}
		public void setTarget(Float target) {
			this.target = target;
		}
		public String getTimespec() {
			return timespec;
		}
		public void setTimespec(String timespec) {
			this.timespec = timespec;
		}
		public boolean isEnabled() {
			return enabled;
		}
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}
}