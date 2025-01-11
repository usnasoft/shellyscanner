package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;

public class ThermostatG2 implements ThermostatInterface {
	private final static String HEATING_TYPE = "heating";
	private final AbstractG2Device parent;
	private String name;
	private boolean enabled;
	private boolean heatingMode; // or cooling
	private boolean running; // or cooling
	private float targetTemp;
	// private int id = 0

	public ThermostatG2(AbstractG2Device parent) {
		this.parent = parent;
	}
	
	public void fillSettings(JsonNode thermostat) {
		name = thermostat.get("name").asText("");
		heatingMode = HEATING_TYPE.equals(thermostat.get("type").asText());
	}
	
	public void fillStatus(JsonNode thermostat) throws IOException {
		enabled = thermostat.get("enable").booleanValue();
		targetTemp = thermostat.get("target_C").floatValue();
		running = thermostat.get("output").booleanValue();
	}

	@Override
	public String getLabel() {
		return name;
	}

	@Override
	public float getMaxTargetTemp() {
		return 35f;
	}
	
	@Override
	public float getMinTargetTemp() {
		return 5f;
	}
	
	@Override
	public float getTargetTemp() {
		return targetTemp;
	}

	@Override
	public void setTargetTemp(float temp) throws IOException {
		// http://192.168.1.20/rpc/Thermostat.SetConfig?id=0&config={target_C=20}
		String res = parent.postCommand("Thermostat.SetConfig", "{\"id\":0,\"config\":{\"target_C\"=" + temp + "}}");
		if(res == null) {
			targetTemp = temp;
		} else {
			throw new IOException(res);
		}
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public void setEnabled(boolean enable) throws IOException {
		String res = parent.postCommand("Thermostat.SetConfig", "{\"id\":0,\"config\":{\"enable\"=" + enable + "}}");
		if(res == null) {
			enabled = enable;
		} else {
			throw new IOException(res);
		}
	}
	
	@Override
	public boolean isRunning() {
		return running;
	}

	/**
	 * return true if heating; false if cooling
	 */
	public boolean isHeating() {
		return heatingMode;
	}
	
	public String restore(JsonNode thermostatData) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", 0);
		ObjectNode th = (ObjectNode)thermostatData.get("thermostat:" + 0).deepCopy();
		th.remove("id");
		th.remove("enable"); // do not restore "volatile" parameters
		th.remove("target_C"); // do not restore "volatile" parameters
		out.set("config", th);
		return parent.postCommand("Thermostat.SetConfig", out);
	}
	
//	public void restoreProfiles(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException, UnsupportedEncodingException, IOException {
//		try {
//			JsonNode profiles = parent.getJSON("/rpc/Thermostat.Schedule.ListProfiles?id=0").get("profiles");
//			for(JsonNode prof: profiles) {
//				errors.add(parent.postCommand("Thermostat.Schedule.DeleteProfile", "{\"id\"=0,\"profile_id\"=" + prof.get("id").asText() + "}"));
//			}
//
//			JsonNode storedProfiles = backupJsons.get("Thermostat.Schedule.ListProfiles_id-0.json").get("profiles");
//			for(JsonNode prof: storedProfiles) {
//				int storedId = prof.get("id").intValue();
//				JsonNode p = parent.getJSON("/rpc/Thermostat.Schedule.CreateProfile?id=0&name=" + URLEncoder.encode(prof.get("name").asText(), StandardCharsets.UTF_8.name()));
//				int newId = p.get("profile_id").intValue();
//				
//				JsonNode rules = backupJsons.get("Thermostat.Schedule.ListRules_id-0_profile_id-" + storedId + ".json").get("rules");
//				for(JsonNode rule: rules) {
//					ObjectNode scheduleParams = JsonNodeFactory.instance.objectNode();
//					
//					((ObjectNode)rule).remove("rule_id");
////					((ObjectNode)rule).remove("profile_id");
//					((ObjectNode)rule).put("profile_id", newId);
//					((ObjectNode)rule).put("id", 0);
//					
//					scheduleParams.put("id", 0);
////					scheduleParams.put("profile_id", newId);
//					scheduleParams.set("rule", rule);
//					
//					ObjectNode outConfig = JsonNodeFactory.instance.objectNode();
//					outConfig.set("config", scheduleParams);
//					
//					errors.add(parent.postCommand("Thermostat.Schedule.AddRule", rule)); // CreateRule ?
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//				}
//			}
//		} catch (DeviceAPIException e) {
//			errors.add(e.getMessage());
//		}
//	}
	
	@Override
	public String toString() {
		return "Target temp: " + targetTemp + (running ? " (on)" : " (off)"); // profile
	}
}

/*
"Thermostat.Create","Thermostat.Delete","Thermostat.GetConfig","Thermostat.GetStatus","Thermostat.Schedule.AddProfile","Thermostat.Schedule.AddRule","Thermostat.Schedule.ChangeRule","Thermostat.Schedule.DeleteProfile","Thermostat.Schedule.DeleteRule","Thermostat.Schedule.ListProfiles","Thermostat.Schedule.ListRules","Thermostat.Schedule.RenameProfile","Thermostat.Schedule.SetConfig","Thermostat.SetConfig"
*/