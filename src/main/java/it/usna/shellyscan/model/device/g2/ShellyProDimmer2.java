package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.meters.MetersWVI;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Pro Dimmer 2PM model
 * @author usna
 */
public class ShellyProDimmer2 extends AbstractProDevice implements InternalTmpHolder, ModulesHolder {
	public final static String ID = "ProDimmerx";
	public final static String MODEL = "SPDM-002PE01EU";
	private float internalTmp;
	private float power0, power1;
	private float voltage0, voltage1;
	private float current0, current1;
	private Meters[] meters;
	private LightWhite light0 = new LightWhite(this, 0);
	private LightWhite light1 = new LightWhite(this, 1);
	private LightWhite[] lightArray = new LightWhite[] {light0, light1};

	public ShellyProDimmer2(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new MetersWVI[] {
				new MetersWVI() {
					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power0;
						} else if(t == Meters.Type.I) {
							return current0;
						} else {
							return voltage0;
						}
					}
				},
				new MetersWVI() {
					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power1;
						} else if(t == Meters.Type.I) {
							return current1;
						} else {
							return voltage1;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Pro Dimmer 2PM";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	public DeviceModule getModule(int index) {
		return index == 0 ? light0 : light1;
	}

	@Override
	public DeviceModule[] getModules() {
		return lightArray;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		light0.fillSettings(configuration.get("light:0"));
		light1.fillSettings(configuration.get("light:1"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode lightStatus0 = status.get("light:0");
		internalTmp = lightStatus0.get("temperature").get("tC").floatValue();

		power0 = lightStatus0.get("apower").floatValue();
		voltage0 = lightStatus0.get("voltage").floatValue();
		current0 = lightStatus0.get("current").floatValue();
		light0.fillStatus(lightStatus0, status.get("input:0"));
		
		JsonNode lightStatus1 = status.get("light:1");
		power1 = lightStatus1.get("apower").floatValue();
		voltage1 = lightStatus1.get("voltage").floatValue();
		current1 = lightStatus1.get("current").floatValue();
		light1.fillStatus(lightStatus1, status.get("input:2"));
	}
	
	@Override
	protected void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> resp) {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		if(MODEL.equals(devInfo.get("model").textValue()) == false) {
			resp.put(RestoreMsg.ERR_RESTORE_MODEL, null);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(light0.restore(configuration));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 2));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 3));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(light1.restore(configuration));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Lights: " + light0 + "-" + light1 ;
	}
}

/*
{
"name" : "Wohnzimmer Decke",
"id" : "shellyprodm2pm-xxxx",
"mac" : "xxxx",
"slot" : 1,
"key" : "00000",
"batch" : "2350-Broadwell",
"fw_sbits" : "04",
"model" : "SPDM-002PE01EU",
"gen" : 2,
"fw_id" : "20241011-114452/1.4.4-g6d2a586",
"ver" : "1.4.4",
"app" : "ProDimmerx",
"auth_en" : false,
"auth_domain" : null
}

--

{
"ble" : {
  "enable" : false,
  "rpc" : {
    "enable" : false
  },
  "observer" : {
    "enable" : false
  }
},
"bthome" : { },
"cloud" : {
  "enable" : true,
  "server" : "shelly-143-eu.shelly.cloud:6022/jrpc"
},
"eth" : {
  "enable" : false,
  "ipv4mode" : "dhcp",
  "ip" : null,
  "netmask" : null,
  "gw" : null,
  "nameserver" : null
},
"input:0" : {
  "id" : 0,
  "name" : "Links",
  "type" : "button",
  "enable" : true,
  "invert" : false
},
"input:1" : {
  "id" : 1,
  "name" : null,
  "type" : "button",
  "enable" : true,
  "invert" : false
},
"input:2" : {
  "id" : 2,
  "name" : "Rechts",
  "type" : "button",
  "enable" : true,
  "invert" : false
},
"input:3" : {
  "id" : 3,
  "name" : null,
  "type" : "button",
  "enable" : true,
  "invert" : false
},
"knx" : {
  "enable" : false,
  "ia" : "15.15.255",
  "routing" : {
    "addr" : "224.0.23.12:3671"
  }
},
"light:0" : {
  "id" : 0,
  "name" : "Rondot",
  "initial_state" : "restore_last",
  "auto_on" : false,
  "auto_on_delay" : 60.0,
  "auto_off" : false,
  "auto_off_delay" : 60.0,
  "transition_duration" : 1.0,
  "min_brightness_on_toggle" : 3,
  "night_mode" : {
    "enable" : false,
    "brightness" : 50,
    "active_between" : [ ]
  },
  "button_fade_rate" : 3,
  "button_presets" : {
    "button_doublepush" : {
      "brightness" : 100
    }
  },
  "in_mode" : "dim",
  "current_limit" : 1.22,
  "power_limit" : 230,
  "undervoltage_limit" : 200,
  "voltage_limit" : 280
},
"light:1" : {
  "id" : 1,
  "name" : "Tisch",
  "initial_state" : "restore_last",
  "auto_on" : false,
  "auto_on_delay" : 60.0,
  "auto_off" : false,
  "auto_off_delay" : 60.0,
  "transition_duration" : 1.0,
  "min_brightness_on_toggle" : 3,
  "night_mode" : {
    "enable" : false,
    "brightness" : 50,
    "active_between" : [ ]
  },
  "button_fade_rate" : 3,
  "button_presets" : {
    "button_doublepush" : {
      "brightness" : 100
    }
  },
  "in_mode" : "dim",
  "current_limit" : 1.22,
  "power_limit" : 230,
  "undervoltage_limit" : 200,
  "voltage_limit" : 280
},
"mqtt" : {
  "enable" : false,
  "server" : null,
  "client_id" : "shellyprodm2pm-000000",
  "user" : null,
  "ssl_ca" : null,
  "topic_prefix" : "shellyprodm2pm-000000",
  "rpc_ntf" : true,
  "status_ntf" : false,
  "use_client_cert" : false,
  "enable_rpc" : true,
  "enable_control" : true
},
"sys" : {
  "device" : {
    "name" : "Wohnzimmer Decke",
    "mac" : "xxxx",
    "fw_id" : "20241011-114452/1.4.4-g6d2a586",
    "discoverable" : true
  },
  "location" : {
    "tz" : "Europe/Vienna",
    "lat" : 0,
    "lon" : 0
  },
  "debug" : {
    "level" : 2,
    "file_level" : null,
    "mqtt" : {
      "enable" : false
    },
    "websocket" : {
      "enable" : false
    },
    "udp" : {
      "addr" : null
    }
  },
  "ui_data" : { },
  "rpc_udp" : {
    "dst_addr" : null,
    "listen_port" : null
  },
  "sntp" : {
    "server" : "ts1.univie.ac.at"
  },
  "cfg_rev" : 37
},
"wifi" : {
  "ap" : {
    "ssid" : "ShellyProDM2PM-xxxx",
    "is_open" : true,
    "enable" : false,
    "range_extender" : {
      "enable" : false
    }
  },
  "sta" : {
    "ssid" : „dummy",
    "is_open" : false,
    "enable" : true,
    "ipv4mode" : "dhcp",
    "ip" : null,
    "netmask" : null,
    "gw" : null,
    "nameserver" : null
  },
  "sta1" : {
    "ssid" : null,
    "is_open" : false,
    "enable" : false,
    "ipv4mode" : "dhcp",
    "ip" : null,
    "netmask" : null,
    "gw" : null,
    "nameserver" : null
  },
  "roam" : {
    "rssi_thr" : -80,
    "interval" : 60
  }
},
"ws" : {
  "enable" : false,
  "server" : null,
  "ssl_ca" : "ca.pem"
}
}


{
"ble" : { },
"bthome" : {
  "errors" : [ "bluetooth_disabled" ]
},
"cloud" : {
  "connected" : true
},
"eth" : {
  "ip" : null
},
"input:0" : {
  "id" : 0,
  "state" : null
},
"input:1" : {
  "id" : 1,
  "state" : null
},
"input:2" : {
  "id" : 2,
  "state" : null
},
"input:3" : {
  "id" : 3,
  "state" : null
},
"knx" : { },
"light:0" : {
  "id" : 0,
  "source" : "HTTP_in",
  "output" : false,
  "brightness" : 100,
  "temperature" : {
    "tC" : 29.8,
    "tF" : 85.6
  },
  "aenergy" : {
    "total" : 975.936,
    "by_minute" : [ 3.848, 0.0, 0.0 ],
    "minute_ts" : 1736501460
  },
  "apower" : 0.0,
  "current" : 0.0,
  "voltage" : 235.1
},
"light:1" : {
  "id" : 1,
  "source" : "dim",
  "output" : false,
  "brightness" : 100,
  "temperature" : {
    "tC" : 34.2,
    "tF" : 93.5
  },
  "aenergy" : {
    "total" : 410.856,
    "by_minute" : [ 0.0, 0.0, 0.0 ],
    "minute_ts" : 1736501460
  },
  "apower" : 0.0,
  "current" : 0.0,
  "voltage" : 235.1
},
"mqtt" : {
  "connected" : false
},
"sys" : {
  "mac" : "xxxx",
  "restart_required" : false,
  "time" : "10:31",
  "unixtime" : 1736501474,
  "uptime" : 7251,
  "ram_size" : 250752,
  "ram_free" : 102168,
  "fs_size" : 524288,
  "fs_free" : 176128,
  "cfg_rev" : 37,
  "kvs_rev" : 0,
  "schedule_rev" : 1,
  "webhook_rev" : 3,
  "available_updates" : {
    "beta" : {
      "version" : "1.5.0-beta1"
    }
  },
  "reset_reason" : 1
},
"wifi" : {
  "sta_ip" : "192.168.1.35",
  "status" : "got ip",
  "ssid" : „dummy",
  "rssi" : -37
},
"ws" : {
  "connected" : false
}
}
*/