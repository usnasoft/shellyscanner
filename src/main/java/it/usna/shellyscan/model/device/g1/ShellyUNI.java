package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g1.modules.Relay;

public class ShellyUNI extends AbstractG1Device implements ModulesHolder {
	public final static String ID = "SHUNI-1";
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private float voltage;
	private float extT0, extT1, extT2;
	private int humidity;
	private Meters[] meters;

	public ShellyUNI(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	protected void init() throws IOException {
		final JsonNode settings = getJSON("/settings");
		this.hostname = settings.get("device").get("hostname").asText("");
		fillSettings(settings);
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		fillStatus(getJSON("/status"));
		
		meters = new Meters[] {
				new Meters() {
					final Meters.Type[] mTypes;
					{
						final ArrayList<Meters.Type> tt = new ArrayList<>();
						final JsonNode extT = settings.path("ext_temperature");
						if(extT.has("0"))  {
							tt.add(Meters.Type.T);
						}
						if(extT.has("1"))  {
							tt.add(Meters.Type.T1);
						}
						if(extT.has("2"))  {
							tt.add(Meters.Type.T2);
						}
						if(settings.path("ext_humidity").has("0"))  {
							tt.add(Meters.Type.H);
						}
						tt.add(Meters.Type.V);
						mTypes = tt.toArray(Meters.Type[]::new);
					}
					
					@Override
					public Type[] getTypes() {
						return mTypes;
					}

					@Override
					public float getValue(Type t) {
						if(t == Type.V) {
							return voltage;
						} if(t == Type.H) {
							return humidity;
						} else if (t == Type.T) {
							return extT0;
						} else if (t == Type.T1) {
							return extT1;
						} else {
							return extT2;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly UNI";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getModulesCount() {
		return 2;
	}
	
	@Override
	public Relay getModule(int index) {
		return index == 0 ? relay0 : relay1;
	}

	@Override
	public Relay[] getModules() {
		return new Relay[] {relay0, relay1};
	}
	
//	public float getVoltage() {
//		return voltage;
//	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		JsonNode ralaysSetting = settings.get("relays");
		relay0.fillSettings(ralaysSetting.get(0));
		relay1.fillSettings(ralaysSetting.get(1));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode ralaysStatus = status.get("relays");
		relay0.fillStatus(ralaysStatus.get(0), status.get("inputs").get(0));
		relay1.fillStatus(ralaysStatus.get(1), status.get("inputs").get(1));
		voltage = (float)status.get("adcs").get(0).get("voltage").asDouble();
		
		JsonNode extTNode = status.path("ext_temperature");
		JsonNode extTNode0 = extTNode.get("0");
		if(extTNode0 != null) {
			extT0 = (float) extTNode0.path("tC").asDouble();
		}
		JsonNode extTNode1 = extTNode.get("1");
		if(extTNode1 != null) {
			extT1 = (float) extTNode1.path("tC").asDouble();
		}
		JsonNode extTNode2 = extTNode.get("2");
		if(extTNode2 != null) {
			extT2 = (float) extTNode2.path("tC").asDouble();
		}
		JsonNode extHNode = status.path("ext_humidity").get("0");
		if (extHNode != null) {
			humidity = extHNode.path("hum").asInt();
		}
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "longpush_time", "factory_reset_from_switch") +
				"&ext_sensors_temperature_unit=" + settings.path("ext_sensors").path("temperature_unit").asText()));

		// ret.startsWith("[") ... don't ask ... it's an array and return an array
		for (int i = 0; i < 3; i++) {
			JsonNode extT = settings.path("ext_temperature").path(i + "");
			if(extT.isNull() == false && extT.get(0) != null) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				String ret = sendCommand("/settings/ext_temperature/" + i + "?" + jsonEntryIteratorToURLPar(extT.get(0).fields()));
				errors.add((ret == null || ret.startsWith("[")) ? null : ret);
			}
		}
		JsonNode hum0 = settings.path("ext_humidity").path("0");
		if(hum0.isNull() == false && hum0.get(0) != null) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			String ret = sendCommand("/settings/ext_humidity/0?" + jsonEntryIteratorToURLPar(hum0.get(0).fields()));
			errors.add((ret == null || ret.startsWith("[")) ? null : ret);
		}
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay0.restore(settings.get("relays").get(0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay1.restore(settings.get("relays").get(1)));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		JsonNode adc0 = settings.get("adcs").get(0);
		errors.add(sendCommand("/settings/adc/0?range=" + adc0.get("range").asText() + "&offset=" + adc0.path("offset").asText()));
		JsonNode relAct = adc0.get("relay_actions");

		for(int index = 0; index < relAct.size(); index++) {
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(sendCommand("/settings/adc/0/relay_actions." + index + "?" + AbstractG1Device.jsonEntryIteratorToURLPar(relAct.get(index).fields())));
		}
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
	}
}

/*
--- settings.json
{
	"device": {
		"type": "SHUNI-1",
		"mac": "xxx",
		"hostname": "shellyuni-xxx",
		"num_outputs": 2
	},
	"wifi_ap": {
		"enabled": false,
		"ssid": "shellyuni-xxx",
		"key": ""
	},
	"wifi_sta": {
		"enabled": true,
		"ssid": "xxx",
		"ipv4_method": "static",
		"ip": "192.168.1.8",
		"gw": "192.168.1.1",
		"mask": "255.255.255.0",
		"dns": null
	},
	"wifi_sta1": {
		"enabled": true,
		"ssid": "ShellyPlus1-xxx",
		"ipv4_method": "static",
		"ip": "192.168.33.8",
		"gw": "192.168.33.1",
		"mask": "255.255.255.0",
		"dns": null
	},
	"ap_roaming": {
		"enabled": true,
		"threshold": -70
	},
	"mqtt": {
		"enable": false,
		"server": "192.168.33.3:1883",
		"user": "",
		"id": "shellyuni-xxx",
		"reconnect_timeout_max": 60.0,
		"reconnect_timeout_min": 2.0,
		"clean_session": true,
		"keep_alive": 60,
		"max_qos": 0,
		"retain": false,
		"update_period": 30
	},
	"coiot": {
		"enabled": true,
		"update_period": 15,
		"peer": ""
	},
	"sntp": {
		"server": "time.google.com",
		"enabled": true
	},
	"login": {
		"enabled": false,
		"unprotected": false,
		"username": "admin"
	},
	"pin_code": "",
	"name": "Briefkastensäule",
	"fw": "20230913-114521/v1.14.0-gcb84623",
	"factory_reset_from_switch": true,
	"pon_wifi_reset": false,
	"discoverable": false,
	"build_info": {
		"build_id": "20230913-114521/v1.14.0-gcb84623",
		"build_timestamp": "2023-09-13T11:45:21Z",
		"build_version": "1.0"
	},
	"cloud": {
		"enabled": true,
		"connected": true
	},
	"timezone": "Europe/Vienna",
	"lat": 48.184502,
	"lng": 16.330151,
	"tzautodetect": true,
	"tz_utc_offset": 7200,
	"tz_dst": false,
	"tz_dst_auto": true,
	"time": "13:22",
	"unixtime": 1696332128,
	"debug_enable": false,
	"allow_cross_origin": false,
	"actions": {
		"active": true,
		"names": [
			"out_on_url",
			"out_off_url",
			"btn_on_url",
			"btn_off_url",
			"longpush_url",
			"shortpush_url",
			"out_on_url",
			"out_off_url",
			"btn_on_url",
			"btn_off_url",
			"longpush_url",
			"shortpush_url",
			"adc_over_url",
			"adc_under_url",
			"report_url",
			"report_url",
			"report_url",
			"ext_temp_over_url",
			"ext_temp_under_url",
			"ext_temp_over_url",
			"ext_temp_under_url",
			"ext_temp_over_url",
			"ext_temp_under_url",
			"ext_temp_over_url",
			"ext_temp_under_url",
			"ext_temp_over_url",
			"ext_temp_under_url",
			"ext_hum_over_url",
			"ext_hum_under_url"
		]
	},
	"hwinfo": {
		"hw_revision": "prod-202101",
		"batch_id": 0
	},
	"mode": "relay",
	"longpush_time": 5000,
	"relays": [
		{
			"name": "Türöffner [Klingeltaster]",
			"appliance_type": "General",
			"ison": false,
			"has_timer": false,
			"default_state": "off",
			"btn_type": "detached",
			"btn_reverse": 0,
			"auto_on": 0.0,
			"auto_off": 1.0,
			"schedule": false,
			"schedule_rules": []
		},
		{
			"name": "Beleuchtung [Post]",
			"appliance_type": "General",
			"ison": false,
			"has_timer": false,
			"default_state": "last",
			"btn_type": "detached",
			"btn_reverse": 1,
			"auto_on": 0.0,
			"auto_off": 0.0,
			"schedule": true,
			"schedule_rules": [
				"2200-0123-off",
				"0000ass-0123456-on",
				"0000asr-0123456-off",
				"0800-0123456-off",
				"0000-0123456-off",
				"0500-02-on",
				"0600-134-on"
			]
		}
	],
	"adcs": [
		{
			"range": 12,
			"offset": 0.7,
			"relay_actions": [
				{
					"over_threshold": 0,
					"over_act": "disabled",
					"under_threshold": 0,
					"under_act": "disabled"
				},
				{
					"over_threshold": 0,
					"over_act": "disabled",
					"under_threshold": 0,
					"under_act": "disabled"
				}
			]
		}
	],
	"ext_sensors": {
		"temperature_unit": "C"
	},
	"ext_temperature": {
		"0": [
			{
				"overtemp_threshold_tC": 0.0,
				"overtemp_threshold_tF": 32.0,
				"undertemp_threshold_tC": 0.0,
				"undertemp_threshold_tF": 32.0,
				"overtemp_act": "disabled",
				"undertemp_act": "disabled",
				"offset_tC": 0.6,
				"offset_tF": 1.1
			},
			{
				"overtemp_threshold_tC": 0.0,
				"overtemp_threshold_tF": 32.0,
				"undertemp_threshold_tC": 0.0,
				"undertemp_threshold_tF": 32.0,
				"overtemp_act": "disabled",
				"undertemp_act": "disabled",
				"offset_tC": 0.6,
				"offset_tF": 1.1
			}
		]
	},
	"ext_humidity": {
		"0": [
			{
				"overhum_threshold": 0.0,
				"underhum_threshold": 0.0,
				"overhum_act": "disabled",
				"underhum_act": "disabled",
				"offset": -3.0
			},
			{
				"overhum_threshold": 0.0,
				"underhum_threshold": 0.0,
				"overhum_act": "disabled",
				"underhum_act": "disabled",
				"offset": -3.0
			}
		]
	},
	"eco_mode_enabled": false
}


--- status
{
    "actions_stats": {
        "skipped": 0
    },
    "adcs": [
        {
            "voltage": 12.05
        }
    ],
    "cfg_changed_cnt": 2,
    "cloud": {
        "connected": true,
        "enabled": true
    },
    "ext_humidity": {
        "0": {
            "hum": 58.7,
            "hwID": "0505"
        }
    },
    "ext_sensors": {
        "temperature_unit": "C"
    },
    "ext_temperature": {
        "0": {
            "hwID": "0505",
            "tC": 12.8,
            "tF": 55.04
        }
    },
    "fs_free": 131022,
    "fs_size": 233681,
    "has_update": false,
    "inputs": [
        {
            "event": "",
            "event_cnt": 0,
            "input": 0
        },
        {
            "event": "",
            "event_cnt": 1,
            "input": 0
        }
    ],
    "mac": "xxxxx",
    "mqtt": {
        "connected": false
    },
    "ram_free": 35244,
    "ram_total": 50776,
    "relays": [
        {
            "has_timer": false,
            "ison": false,
            "source": "input",
            "timer_duration": 0,
            "timer_remaining": 0,
            "timer_started": 0
        },
        {
            "has_timer": false,
            "ison": true,
            "source": "schedule",
            "timer_duration": 0,
            "timer_remaining": 0,
            "timer_started": 0
        }
    ],
    "serial": 2311,
    "time": "20:12",
    "unixtime": 1696443134,
    "update": {
        "has_update": false,
        "new_version": "20230913-114521/v1.14.0-gcb84623",
        "old_version": "20230913-114521/v1.14.0-gcb84623",
        "status": "idle"
    },
    "uptime": 110054,
    "wifi_sta": {
        "connected": true,
        "ip": "192.168.33.8",
        "rssi": -76,
        "ssid": "ShellyPlus1-xxxx"
    }
}

setting - no sensor
...
	"ext_sensors": {},
	"ext_temperature": {},
	"ext_humidity": {},
...

status - no sensor
...
	"ext_sensors": {},
	"ext_temperature": {},
	"ext_humidity": {},
...
*/