package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g1.modules.Thermostat;

public class ShellyTRV extends AbstractG1Device {
	public final static String ID = "SHTRV-01";
//	private final static Logger LOG = LoggerFactory.getLogger(ShellyTRV.class);
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT, Meters.Type.T};
	private Thermostat thermostat = new Thermostat(this);
	private float measuredTemp;
	private Meters[] meters;
	protected int bat;

	public ShellyTRV(InetAddress address) {
		super(address);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.BAT) {
							return bat;
						} else {
							return measuredTemp;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "TRV";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		thermostat.fillSettings(settings.get("thermostats").get(0));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		bat = status.get("bat").get("value").asInt();
		JsonNode therm = status.get("thermostats").get(0);
		measuredTemp = (float)therm.get("tmp").get("value").doubleValue();
		thermostat.fillStatus(therm);
	}
	
	public float getMeasuredTemp() {
		return measuredTemp;
	}
	
//	public float getTargetTemp() {
//		return thermostat.getTargetTemp();
//	}
//	
//	public float getPosition() {
//		return thermostat.getPosition();
//	}
	
	public Thermostat getThermostat() {
		return thermostat;
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException, InterruptedException {
		JsonNode display = settings.path("display");
		errors.add(sendCommand("/settings?child_lock=" + settings.get("child_lock").asText() +
				"&display_brightness=" + display.get("brightness").asText() +
				"&display_flipped=" + display.get("flipped").asText()));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(thermostat.restore(settings.get("thermostats").get(0)));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Thermostat: " + thermostat;
	}
}

/*
SETTINGS
{
"device" : {
  "type" : "SHTRV-01",
  "mac" : "xxxxx",
  "hostname" : "shellytrv-xxxxx",
  "num_outputs" : 0
},
"wifi_ap" : {
  "enabled" : false,
  "ssid" : "shellytrv-xxxxxxxx"
},
"wifi_sta" : {
  "enabled" : true,
  "ssid" : "D-Link xxxxxxx",
  "ipv4_method" : "dhcp",
  "ip" : null,
  "gw" : null,
  "mask" : null,
  "dns" : null
},
"mqtt" : {
  "enable" : false,
  "server" : "192.168.33.2:1883",
  "user" : null,
  "id" : "shellytrv-xxxxxxxx",
  "clean_session" : true,
  "max_qos" : 0,
  "retain" : false,
  "update_period" : 60
},
"sntp" : {
  "server" : "time.google.com",
  "enabled" : true
},
"login" : {
  "enabled" : false,
  "unprotected" : false,
  "username" : "admin",
  "default_username" : "admin"
},
"pin_code" : "",
"name" : "Temperatura Studio",
"fw" : "20220811-152343/v2.1.8@5afc928c",
"discoverable" : false,
"build_info" : {
  "build_id" : "20220811-152343/v2.1.8@5afc928c",
  "build_timestamp" : "2022-08-11T15:23:43Z",
  "build_version" : "2022081115"
},
"cloud" : {
  "enabled" : true
},
"coiot" : {
  "enabled" : false,
  "update_period" : 3600,
  "peer" : ""
},
"timezone" : "Europe/Rome",
"lat" : 41.91114,
"lng" : 12.49649,
"tzautodetect" : true,
"tz_utc_offset" : 3600,
"tz_dst" : false,
"tz_dst_auto" : true,
"time" : "09:41",
"child_lock" : false,
"clog_prevention" : false,
"display" : {
  "brightness" : 4,
  "flipped" : false
},
"hwinfo" : {
  "hw_revision" : "xxxxx",
  "batch_id" : x
},
"sleep_mode" : {
  "period" : 60,
  "unit" : "m"
},
"thermostats" : [ {
  "target_t" : {
    "enabled" : true,
    "value" : 18.0,
    "value_op" : 8.0,
    "units" : "C",
    "accelerated_heating" : false
  },
  "schedule" : true,
  "schedule_profile" : 1,
  "schedule_profile_names" : [ "Livingroom", "Livingroom 1", "Bedroom", "Bedroom 1", "Holiday" ],
  "schedule_rules" : [ "0700-0123456-18.5", "2200-0123456-16" ],
  "temperature_offset" : -1.0,
  "ext_t" : {
    "enabled" : false,
    "floor_heating" : false
  },
  "t_auto" : {
    "enabled" : true
  },
  "boost_minutes" : 30,
  "valve_min_percent" : 0.0,
  "force_close" : false,
  "calibration_correction" : true,
  "extra_pressure" : false,
  "open_window_report" : false
} ]
}

STATUS
{
"wifi_sta" : {
  "connected" : true,
  "ssid" : "xxxxxxxxxxxxxx",
  "ip" : "192.168.3.40",
  "rssi" : -44
},
"cloud" : {
  "enabled" : true,
  "connected" : true
},
"mqtt" : {
  "connected" : false
},
"time" : "09:41",
"unixtime" : 1670229692,
"serial" : 416,
"has_update" : false,
"mac" : "xxxxxxx",
"cfg_changed_cnt" : 0,
"actions_stats" : {
  "skipped" : 0
},
"thermostats" : [ {
  "pos" : 56.6,
  "target_t" : {
    "enabled" : true,
    "value" : 18.0,
    "value_op" : 8.0,
    "units" : "C"
  },
  "tmp" : {
    "value" : 17.8,
    "units" : "C",
    "is_valid" : true
  },
  "schedule" : true,
  "schedule_profile" : 1,
  "boost_minutes" : 0,
  "window_open" : false
} ],
"calibrated" : true,
"bat" : {
  "value" : 100,
  "voltage" : 4.129
},
"charger" : false,
"update" : {
  "status" : "unknown",
  "has_update" : false,
  "new_version" : "20220811-152343/v2.1.8@5afc928c",
  "old_version" : "20220811-152343/v2.1.8@5afc928c",
  "beta_version" : null
},
"ram_total" : 97280,
"ram_free" : 22648,
"fs_size" : 65536,
"fs_free" : 59384,
"uptime" : 235824,
"fw_info" : {
  "device" : "shellytrv-xxxxxxxxxx",
  "fw" : "20220811-152343/v2.1.8@5afc928c"
},
"ps_mode" : 0,
"dbg_flags" : 0
}
*/