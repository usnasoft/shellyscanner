package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.ScheduleManagerThermWD;
import it.usna.shellyscan.model.device.g2.modules.ThermostatG2;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.DisplayInterface;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Shelly Wall Display X2i
 * Supported bases:
 * 2-output power base
 * 2-output power base
 * @author usna
 */
public class WallDisplayX2i extends AbstractG2Device implements DisplayInterface, ModulesHolder {
	public static final String ID = "WallDisplayV2";
	public static final String MODEL = "SAWD-5A1XX10EU0";
	private static final Meters.Type[] SUPPORTED_MEASURES_T = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.L};
	private static final Meters.Type[] SUPPORTED_MEASURES_NO_T = new Meters.Type[] {Meters.Type.L};
	private float temp;
	private float humidity;
	private int lux;
	private Meters[] meters;
	private Relay relay0 = null;
	private Relay relay1 = null;
	private Relay[] relays = null;
	private ThermostatG2 thermostat = null;
	private ThermostatG2[] thermostats = null;

	public WallDisplayX2i(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		super.init(devInfo);
//		if(devInfo.get("ch").asString().equals("switch:0")) {
//			// single switch power base
//		} else ...
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						// (humidity < 0 || temp < -200) : no external h&t sensor connected
						return (humidity < 0 || temp < -200) ? SUPPORTED_MEASURES_NO_T : SUPPORTED_MEASURES_T;
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
		return "Wall Display X2i";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public DeviceModule[] getModules() {
		return relays != null ? relays : thermostats;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		JsonNode thermostatConf = configuration.get("thermostat:0");
		if(thermostatConf != null) {
			if(thermostat == null) {
				thermostat = new ThermostatG2(this);
				thermostats = new ThermostatG2[] {thermostat};
				relay0 = relay1 = null;
				relays = null;
			}
			thermostat.fillSettings(thermostatConf);
		} else {
			JsonNode switch0Conf = configuration.get("switch:0");
			JsonNode switch1Conf = configuration.get("switch:1");
			if(switch0Conf != null && switch1Conf != null) {
				if(relay0 == null /*|| relay1 == null*/) {
					relay0 = new Relay(this, 0);
					relay1 = new Relay(this, 1);
					relays = new Relay[] {relay0, relay1};
					thermostat = null;
					thermostats = null;
				}
				relay0.fillSettings(switch0Conf);
				relay1.fillSettings(switch1Conf);
			} else if(switch0Conf != null) {
				if(relay0 == null || relay1 != null) {
					relay0 = new Relay(this, 0);
					relay1 = null;
					relays = new Relay[] {relay0};
					thermostat = null;
					thermostats = null;
				}
				JsonNode input0Conf = configuration.get("input:0");
				if(input0Conf != null) {
					relay0.fillSettings(switch0Conf, input0Conf);
				} else {
					relay0.fillSettings(switch0Conf);
				}
			}
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		temp = status.path("temperature:0").path("tC").floatValue(-270f);
		humidity = status.path("humidity:0").path("rh").floatValue(-2f);
		lux = status.path("illuminance:0").path("lux").intValue();
		
		if(thermostat != null) {
			thermostat.fillStatus(status.get("thermostat:0"));
		} else {
			if(relay0 != null) {
				JsonNode input0Status = status.get("input:0");
				if(input0Status != null) {
					relay0.fillStatus(status.get("switch:0"), input0Status);
				} else {
					relay0.fillStatus(status.get("switch:0"));
				}
			}
			if(relay1 != null) {
				relay1.fillStatus(status.get("switch:1"));
			}
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		if(thermostat != null) {
			ArrayList<String> l = new ArrayList<>(Arrays.asList(super.getInfoRequests()));
			l.add("/rpc/Thermostat.Schedule.ListProfiles?id=0");
			try {
				JsonNode profiles = getJSON("/rpc/Thermostat.Schedule.ListProfiles?id=0").get("profiles");
				for(JsonNode p: profiles) {
					l.add("(Thermostat.Schedule.ListRules [" + p.path("id").asString(null) + "])/rpc/Thermostat.Schedule.ListRules?id=0&profile_id=" + p.get("id").asString(null));
				}
			} catch (IOException e) {}
			return l.toArray(String[]::new);
		} else {
			return super.getInfoRequests();
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
	public boolean hasThermostat() {
		return thermostat != null;
	}
	
	@Override
	protected void backup(ZipOutputStream out) throws IOException, InterruptedException {
		if(thermostat != null) {
			JsonNode profiles = sectionToStream("/rpc/Thermostat.Schedule.ListProfiles?id=0", "Thermostat.Schedule.ListProfiles.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			for(JsonNode p: profiles.get("profiles")) {
				final String id = p.get("id").asString("");
				sectionToStream("/rpc/Thermostat.Schedule.ListRules?id=0&profile_id=" + id, "Thermostat.Schedule.ListRules_profile_id-" + id + ".json", out);
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
		}
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		JsonNode backupDeviceInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		JsonNode deviceInfo = getJSON("/rpc/Shelly.GetDeviceInfo");
		if(backupDeviceInfo.get("ch").equals(deviceInfo.get("ch")) == false) {
			res.put(RestoreMsg.ERR_RESTORE_POWER_BASE, null);
			return;
		}
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
//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		if(thermMode && thermostat != null) { // saved configuration was "thermostat" and the current is too? 
			errors.add(thermostat.restore(backupConfiguration));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			new ScheduleManagerThermWD(this).restore(backupJsons, errors);
		}
		
		if(relay0 != null && backupConfiguration.hasNonNull("switch:0")) {
			errors.add(relay0.restore(backupConfiguration));
		}
		
		if(relay1 != null && backupConfiguration.hasNonNull("switch:1")) {
			errors.add(relay1.restore(backupConfiguration));
		}

		if(backupConfiguration.hasNonNull("input:0")) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(Input.restore(this, backupConfiguration, 0));
		}
		
		JsonNode ui = backupConfiguration.get("ui")/*.deepCopy()*/;
		ObjectNode out = JsonNodeFactory.instance.objectNode().set("config", ui);
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Ui.SetConfig", out));
		
		// can't restore /sys/ext_sensor_id since external sensors must be approved by the user

		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Temperature.SetConfig", createIndexedRestoreNode(backupConfiguration, "temperature", 0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Humidity.SetConfig", createIndexedRestoreNode(backupConfiguration, "humidity", 0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Illuminance.SetConfig", createIndexedRestoreNode(backupConfiguration, "illuminance", 0)));
	}
	
//	@Override
//	public String toString() {
//		if(relay0 != null) {
//			return super.toString() + " Relay: " + relay0;
//		} else {
//			return super.toString() + " Therm: " + thermostat;
//		}
//	}
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