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
		return 35;
	}
	
	@Override
	public float getMinTargetTemp() {
		return 5;
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
	
	/*
	 * return true if heating; false if cooling
	 */
	public boolean getType() {
		return heatingMode;
	}
	
	@Override
	public boolean isRunning() {
		return running;
	}
	
	public String restore(JsonNode data) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", 0);
		ObjectNode th = (ObjectNode)data.get("thermostat:" + 0).deepCopy();
		th.remove("id");
		th.remove("enable"); // do not restore "volatile" parameters
		th.remove("target_C"); // do not restore "volatile" parameters
		out.set("config", th);
		return parent.postCommand("Thermostat.SetConfig", out);
	}
	
	@Override
	public String toString() {
		return "Target temp:" + targetTemp;
	}
}

/*
"Thermostat.Create","Thermostat.Delete","Thermostat.GetConfig","Thermostat.GetStatus","Thermostat.Schedule.AddProfile","Thermostat.Schedule.AddRule","Thermostat.Schedule.ChangeRule","Thermostat.Schedule.DeleteProfile","Thermostat.Schedule.DeleteRule","Thermostat.Schedule.ListProfiles","Thermostat.Schedule.ListRules","Thermostat.Schedule.RenameProfile","Thermostat.Schedule.SetConfig","Thermostat.SetConfig"
*/