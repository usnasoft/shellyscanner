package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.shellyscan.model.device.g2.modules.ThermostatG2;
import it.usna.shellyscan.model.device.modules.DeviceModule;

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
			ArrayList<String> l = new ArrayList<>(Arrays.asList(
					"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
					"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
					"/rpc/Thermostat.Schedule.ListProfiles?id=0"));
			try {
				JsonNode profiles = getJSON("/rpc/Thermostat.Schedule.ListProfiles?id=0").get("profiles");
				for(JsonNode p: profiles) {
					l.add("(Thermostat.Schedule.ListRules [" + p.path("name").asText() + "])/rpc/Thermostat.Schedule.ListRules?id=0&profile_id=" + p.get("id").asText());
				}
			} catch (IOException e) {}
			return l.toArray(String[]::new);
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
	protected void backup(FileSystem out) throws IOException, InterruptedException {
		if(thermostat != null) {
			JsonNode profiles = sectionToStream("/rpc/Thermostat.Schedule.ListProfiles?id=0", "Thermostat.Schedule.ListProfiles.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			for(JsonNode p: profiles.get("profiles")) {
				final String id = p.get("id").asText();
				sectionToStream("/rpc/Thermostat.Schedule.ListRules?id=0&profile_id=" + id, "Thermostat.Schedule.ListRules_profile_id-" + id + ".json", out);
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
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException, IOException {
		JsonNode backupConfiguration = backupJsons.get("Shelly.GetConfig.json");
		boolean thermMode = backupConfiguration.get("thermostat:0") != null;
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		if(thermMode && thermostat != null) { // saved configuration was "thermostat" and the current too? 
			errors.add(thermostat.restore(backupConfiguration));
		} else if(thermMode == false && relay != null) {
			errors.add(relay.restore(backupConfiguration));
		} else {
			errors.add(RestoreMsg.ERR_RESTORE_MODE_THERM.name());
		}
		
		ObjectNode ui = (ObjectNode)backupConfiguration.get("ui").deepCopy();
		ObjectNode out = JsonNodeFactory.instance.objectNode().set("config", ui);
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Ui.SetConfig", out));
		
		// can't restore /sys/ext_sensor_id since external sensor must approve

		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Temperature.SetConfig", createIndexedRestoreNode(backupConfiguration, "temperature", 0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Humidity.SetConfig", createIndexedRestoreNode(backupConfiguration, "humidity", 0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Illuminance.SetConfig", createIndexedRestoreNode(backupConfiguration, "illuminance", 0)));
		
		ScheduleManagerThermWD scheduleManagerTherm = new ScheduleManagerThermWD(this);
		scheduleManagerTherm.restore(backupJsons, errors);
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

/*
http://deviceip/rpc/Shelly.ListMethods

{"methods":["BLE.GetConfig","BLE.GetStatus","BLE.SetConfig","Button.GetConfig","Button.GetStatus","Button.SetConfig","Button.Trigger","Cloud.GetConfig","Cloud.GetStatus","Cloud.SetConfig",
"DevicePower.GetStatus","Humidity.GetConfig","Humidity.GetStatus","Humidity.SetConfig","Illuminance.GetConfig","Illuminance.GetStatus","Illuminance.SetConfig","Input.GetConfig","Input.GetStatus",
"Input.SetConfig","Input.Trigger","Media.Delete","Media.GetConfig","Media.GetStatus","Media.List","Media.ListAudioAlbums","Media.ListAudioArtists","Media.MediaPlayer.Next","Media.MediaPlayer.Pause",
"Media.MediaPlayer.Play","Media.MediaPlayer.PlayAudioClip","Media.MediaPlayer.PlayOrPause","Media.MediaPlayer.Previous","Media.MediaPlayer.Stop","Media.PutMedia","Media.Radio.ListFavourites",
"Media.Radio.PlayFavourite","Media.Radio.PlayNextFavourite","Media.Radio.PlayPreviousFavourite","Media.Radio.Stop","Media.Reload","Media.SetVolume","Mqtt.GetConfig","Mqtt.GetStatus","Mqtt.SetConfig",
"Schedule.Create","Schedule.Delete","Schedule.DeleteAll","Schedule.GetConfig","Schedule.GetStatus","Schedule.List","Schedule.SetConfig","Schedule.Update","Script.GetConfig","Script.GetStatus",
"Script.List","Script.SetConfig","Shelly.CheckForUpdate","Shelly.DetectLocation","Shelly.FactoryReset","Shelly.GetComponents","Shelly.GetConfig","Shelly.GetDeviceInfo","Shelly.GetStatus",
"Shelly.ListMethods","Shelly.ListTimezones","Shelly.PutUserCA","Shelly.Reboot","Shelly.ResetAuthCode","Shelly.SetAuth","Shelly.SetConfig","Shelly.Update","Switch.GetConfig","Switch.GetStatus",
"Switch.Set","Switch.SetConfig","Switch.Toggle","Sys.GetConfig","Sys.GetInternalTemperatures","Sys.GetStatus","Sys.ListDebugComponents","Sys.RestartApplication","Sys.SetConfig","Sys.SetDebugConfig",
"Temperature.GetConfig","Temperature.GetStatus","Temperature.SetConfig","Thermostat.Create","Thermostat.Delete","Thermostat.GetConfig","Thermostat.GetStatus","Thermostat.Schedule.AddProfile",
"Thermostat.Schedule.AddRule","Thermostat.Schedule.ChangeRule","Thermostat.Schedule.CreateProfile","Thermostat.Schedule.CreateRule","Thermostat.Schedule.DeleteAllRules",
"Thermostat.Schedule.DeleteProfile","Thermostat.Schedule.DeleteRule","Thermostat.Schedule.ListProfiles","Thermostat.Schedule.ListRules","Thermostat.Schedule.RenameProfile",
"Thermostat.Schedule.SetConfig","Thermostat.Schedule.UpdateRule","Thermostat.SetConfig","Ui.GetConfig","Ui.GetStatus","Ui.ListAvailable","Ui.Screen.Set","Ui.SetConfig","Ui.Tap","Virtual.Add",
"Virtual.Delete","Virtual.List","Virtual.ListSupported","Webhook.Create","Webhook.Delete","Webhook.DeleteAll","Webhook.List","Webhook.ListSupported","Webhook.Update","WiFi.GetConfig",
"WiFi.GetStatus","WiFi.SavedNetworks.Delete","WiFi.SavedNetworks.List","WiFi.Scan","WiFi.SetConfig","WiFi.SpeedTest","Ws.GetConfig","Ws.GetStatus","Ws.SetConfig"]}

https://community.shelly.cloud/topic/1793-walldisplay-list-for-useful-rpc-commands/
*/