package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
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

	public String enableProfiles(boolean enable) {
		return wd.postCommand("Thermostat.Schedule.SetConfig", "{\"id\":" + THERM_ID + ",\"config\":{\"enable\":" + String.valueOf(enable) + "}}" );
	}
	
	public List<ThermProfile> getProfiles() throws IOException {
		ArrayList<ThermProfile> ret = new ArrayList<>();
		wd.getJSON("/rpc/Thermostat.Schedule.ListProfiles?id=" + THERM_ID).path("profiles").forEach(node ->
			ret.add(new ThermProfile(node.get("id").intValue(), node.path("name").asText("")))
		);
		return ret;
	}

	public ThermProfile getCurrentProfile() throws IOException {
		JsonNode status = wd.getJSON("/rpc/Shelly.GetStatus").get("thermostat:0").get("schedules");
		return (status != null && status.get("enable").booleanValue()) ? new ThermProfile(status.get("profile_id").intValue(), status.path("profile_name").asText("")) : null;
	}

	//todo verifica
	public String setCurrentProfile(int profileId) throws IOException {
		return wd.postCommand("Thermostat.Schedule.SetConfig", "{\"id\":" + THERM_ID + ",\"config\":{\"profile_id\":" + profileId + "}}" );
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
			rules.add(new Rule(rule.get("rule_id").textValue(), rule.get("target_C").floatValue(), rule.get("timespec").textValue(), rule.path("enable").booleanValue()));
		}
		return rules;
	}

	public String enable(String ruleId, int profileId, boolean enable) {
		 return wd.postCommand("Thermostat.Schedule.UpdateRule", "{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + ",\"config\":{\"rule_id\":\"" + ruleId + "\",\"enable\":" +  String.valueOf(enable) + "}}");
	}
	
	/**
	 * Rule creation
	 * @param r
	 * @param profileId
	 * @return the new id; null in case of error
	 * @throws IOException
	 */
	public String create(Rule r, int profileId) throws IOException {
		JsonNode res = wd.getJSON("Thermostat.Schedule.CreateRule",
				"{\"id\":" + THERM_ID + ",\"config\":{\"profile_id\":" + profileId + ",\"target_C\":" + r.target + ",\"timespec\":\"" + r.timespec + "\",\"enable\":" + String.valueOf(r.enabled) + "}}");
		return res.get("new_rule").get("rule_id").asText();
	}
	
	public String create(String timespec, float target, boolean enabled, int profileId) throws IOException {
		JsonNode res = wd.getJSON("Thermostat.Schedule.CreateRule",
				"{\"id\":" + THERM_ID + ",\"config\":{\"profile_id\":" + profileId + ",\"target_C\":" + target + ",\"timespec\":\"" + timespec + "\",\"enable\":" + String.valueOf(enabled) + "}}");
		return res.get("new_rule").get("rule_id").asText();
	}

	public String update(Rule r, int profileId) {
		return wd.postCommand("Thermostat.Schedule.UpdateRule",
				"{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + ",\"config\":{\"rule_id\":\"" + r.ruleId + "\",\"target_C\":" + r.target + ",\"timespec\":\"" + r.timespec  + "\",\"enable\":" + String.valueOf(r.enabled) + "}}");
	}
	
	public String delete(String ruleId, int profileId) {
		return wd.postCommand("Thermostat.Schedule.DeleteRule", "{\"id\":" + THERM_ID + ",\"profile_id\":" + profileId + ",\"rule_id\":\"" + ruleId + "\"}");
	}
	
	public void restore(Map<String, JsonNode> backup, List<String> errors) throws InterruptedException {
		try {
			// delete existing prifiles
			for(ThermProfile p: getProfiles()) {
				errors.add(deleteProfiles(p.id));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}

			// restore profiles
			JsonNode profilesNode = backup.get("Thermostat.Schedule.ListProfiles.json").path("profiles");
			for(JsonNode storedProfile: profilesNode) {
				int newProfileId = addProfiles(storedProfile.path("name").asText(""));
				int oldProfileId = storedProfile.get("id").intValue();
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				
				// create rules for each profile
				JsonNode rules = backup.get("Thermostat.Schedule.ListRules_profile_id-" + oldProfileId + ".json").get("rules");
				for(JsonNode rule: rules) {
					create(rule.get("timespec").asText(), rule.get("target_C").floatValue(), rule.path("enable").booleanValue(), newProfileId);
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			}
		} catch (IOException e) {
			errors.add(e.getMessage());
		}
	}
	
	public record ThermProfile(int id, String name) {
		@Override
		public String toString() {
			return name;
		}
	}
	
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
		
		public String getId() {
			return ruleId;
		}
		
		public void setId(String ruleId) {
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