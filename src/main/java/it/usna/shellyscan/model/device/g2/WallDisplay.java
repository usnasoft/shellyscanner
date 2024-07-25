package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.ThermostatG2;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.ModulesHolder;

/**
 * Shelly Wall Display
 * @author usna
 */
public class WallDisplay extends AbstractG2Device implements ModulesHolder {
	public final static String ID = "WallDisplay";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.L};
	private float temp;
	private float humidity;
	private int lux;
	private Meters[] meters;
	private Relay relay = null;
	private Relay[] relays = null;
	private ThermostatG2 thermostat = null;
	private ThermostatG2[] thermostats = null;

	public WallDisplay(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.T) {
							return temp;
						} else if(t == Meters.Type.H) {
							return humidity;
						} else {
							return lux;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Wall Display";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public DeviceModule getModule(int index) {
		return relay != null ? relay : thermostat;
	}

	@Override
	public DeviceModule[] getModules() {
		return relay != null ? relays : thermostats;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		JsonNode thermostatConf = configuration.get("thermostat:0");
		if(thermostatConf != null) {
			if(thermostat == null) {
				thermostat = new ThermostatG2(this);
				thermostats = new ThermostatG2[] {thermostat};
				relay = null;
				relays = null;
			}
			thermostat.fillSettings(thermostatConf);
		} else {
			if(relay == null) {
				relay = new Relay(this, 0);
				relays = new Relay[] {relay};
				thermostat = null;
				thermostats = null;
			}
			relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		temp = status.path("temperature:0").path("tC").floatValue();
		humidity = status.path("humidity:0").path("rh").floatValue();
		lux = status.path("illuminance:0").path("lux").intValue();
		if(relay != null) {
			relay.fillStatus(status.get("switch:0"), status.get("input:0"));
		} else {
			thermostat.fillStatus(status.get("thermostat:0"));
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		if(relay != null) {
			return super.getInfoRequests();
		} else {
			return new String[] {
					"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
					"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
					"/rpc/Thermostat.Schedule.ListProfiles?id=0"};
		}
	}
	
	public float getTemp() {
		return temp;
	}
	
	public float getHumidity() {
		return humidity;
	}
	
	public float getIlluminance() {
		return lux;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void backup(ZipOutputStream out) throws IOException, InterruptedException {
		if(thermostat != null) {
			byte[] profiles = sectionToStream("/rpc/Thermostat.Schedule.ListProfiles?id=0", "Thermostat.Schedule.ListProfiles_id-0.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			
			JsonNode profilesJ = jsonMapper.readTree(profiles).get("profiles");
			for(JsonNode p: profilesJ) {
				final String id = p.get("id").asText();
				sectionToStream("/rpc/Thermostat.Schedule.ListRules?id=0&profile_id=" + id, "Thermostat.Schedule.ListRules_id-0_profile_id-" + id + ".json", out);
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
		}
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		JsonNode backupConfiguration = backupJsons.get("Shelly.GetConfig.json");
		boolean thermMode = backupConfiguration.get("thermostat:0") != null;
		if((thermMode && thermostat == null) || (thermMode == false && thermostat != null)) {
			res.put(RestoreMsg.ERR_RESTORE_MODE_THERM, null);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode backupConfiguration = backupJsons.get("Shelly.GetConfig.json");
		boolean thermMode = backupConfiguration.get("thermostat:0") != null;
		if(thermMode && thermostat != null) { // saved configuration was "thermostat" and the current too? 
			errors.add(thermostat.restore(backupConfiguration));
		} else if(thermMode == false && relay != null) {
			errors.add(relay.restore(backupConfiguration));
		} else {
			errors.add(RestoreMsg.ERR_RESTORE_MODE_THERM.name());
		}
		
		ObjectNode ui = (ObjectNode)backupConfiguration.get("ui").deepCopy();
		ObjectNode out = JsonNodeFactory.instance.objectNode().set("config", ui);
		errors.add(postCommand("Ui.SetConfig", out));
	}
	
	@Override
	public String toString() {
		if(relay != null) {
			return super.toString() + " Relay: " + relay;
		} else {
			return super.toString() + " Therm: " + thermostat;
		}
	}
}

// http://deviceip/
// http://192.168.1.28/rpc/Thermostat.Schedule.ListProfiles?id=0
// http://192.168.1.28/rpc/Thermostat.Schedule.ListRules?id=0&profile_id=0