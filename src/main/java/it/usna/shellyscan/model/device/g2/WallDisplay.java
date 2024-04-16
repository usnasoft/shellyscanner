package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

/**
 * Shelly Shelly Plus mini 1 model
 * @author usna
 */
public class WallDisplay extends AbstractG2Device /*implements RelayCommander, InternalTmpHolder*/ {
	public final static String ID = "WallDisplay";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.L};
//	private Relay relay = new Relay(this, 0);
//	private Relay[] ralayes = new Relay[] {relay};
//	private float internalTmp;
	private float temp;
	private float humidity;
	private int lux;
	private Meters[] meters;

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
						if(t == Meters.Type.H) {
							return lux;
						} else if(t == Meters.Type.H) {
							return humidity;
						} else {
							return temp;
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
	
//	@Override
//	public Relay getRelay(int index) {
//		return relay;
//	}
//	
//	@Override
//	public Relay[] getRelays() {
//		return ralayes;
//	}
//	
//	@Override
//	public float getInternalTmp() {
//		return internalTmp;
//	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
//		relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		temp = status.path("temperature:0").path("tC").floatValue();
		humidity = status.path("humidity:0").path("rh").floatValue();
		lux = status.path("illuminance:0").path("lux").intValue();
		
//		JsonNode switchStatus = status.get("switch:0");
//		relay.fillStatus(switchStatus, status.get("input:0"));
//
//		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
	}
	
	public float getTemp() {
		return temp;
	}
	
	public float getHumidity() {
		return humidity;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
//		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
//		errors.add(Input.restore(this, configuration, "0"));
//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		errors.add(relay.restore(configuration));
	}
	
//	@Override
//	public String toString() {
//		return super.toString() + " Relay: " + relay;
//	}
}

/*
GetDeviceInfo

{
  "id" : "ShellyWallDisplay-000822D5DAD8",
  "mac" : "000822D5DAD8",
  "model" : "SAWD-0A1XX10EU1",
  "gen" : 2,
  "fw_id" : "20240312-155021/1.2.9-30788ca8",
  "ver" : "1.2.9",
  "app" : "WallDisplay",
  "auth_en" : false,
  "uptime" : 2771767,
  "app_uptime" : 2771660,
  "discoverable" : false,
  "cfg_rev" : 8,
  "schedule_rev" : 0,
  "webhook_rev" : 0,
  "platform" : "vXD10000K",
  "available_updates" : { },
  "restart_required" : false,
  "unixtime" : 1713115413,
  "relay_in_thermostat" : false,
  "sensor_in_thermostat" : true
}

GetConfig

{
  "ble" : {
    "enable" : false,
    "rpc" : {
      "enable" : true
    },
    "observer" : {
      "enable" : false
    }
  },
  "wifi" : {
    "sta" : {
      "enable" : true,
      "ssid" : "20S",
      "roam_interval" : 900,
      "is_open" : false,
      "ipv4mode" : "dhcp",
      "ip" : "192.168.20.74",
      "netmask" : "255.255.255.0",
      "gw" : "192.168.20.1",
      "nameserver" : "8.8.8.8"
    },
    "sta1" : {
      "ssid" : "Su KITCHEN",
      "enable" : true,
      "is_open" : false,
      "ipv4mode" : "dhcp"
    }
  },
  "switch:0" : {
    "id" : 0,
    "auto_off" : false,
    "auto_on_delay" : 0,
    "initial_state" : "off",
    "in_mode" : "follow",
    "name" : null
  },
  "input:0" : {
    "id" : 0,
    "type" : "switch",
    "invert" : false,
    "factory_reset" : true,
    "name" : null
  },
  "temperature:0" : {
    "id" : 0,
    "report_thr_C" : 1,
    "offset_C" : 0,
    "name" : null
  },
  "humidity:0" : {
    "id" : 0,
    "report_thr" : 1,
    "offset" : 0,
    "name" : null
  },
  "illuminance:0" : {
    "id" : 0,
    "bright_thr" : 200,
    "dark_thr" : 30,
    "name" : null
  },
  "ui" : {
    "lock_type" : "none",
    "disable_gestures_when_locked" : false,
    "show_favourites" : true,
    "show_main_sensor_graph" : true,
    "use_F" : false,
    "screen_saver" : {
      "enable" : false,
      "timeout" : 10,
      "show_clock" : true,
      "show_humidity" : true,
      "show_temperature" : true
    },
    "screen_off_when_idle" : true,
    "brightness" : {
      "auto" : true,
      "level" : 0,
      "auto_off" : {
        "enable" : true,
        "by_lux" : false
      }
    },
    "relay_state_overlay" : {
      "enable" : true,
      "always_visible" : false
    }
  },
  "sys" : {
    "device" : {
      "name" : "Cey's Thermostat",
      "fw_id" : "20240312-155021/1.2.9-30788ca8",
      "mac" : "000822D5DAD8",
      "discoverable" : false
    },
    "cfg_rev" : 8,
    "location" : {
      "tz" : "Europe/Sofia",
      "lat" : 53.5542,
      "lon" : -6.7878
    },
    "sntp" : {
      "server" : "time.google.com"
    },
    "debug" : {
      "websocket" : {
        "enable" : false
      },
      "mqtt" : {
        "enable" : false
      },
      "logs" : {
        "Generic" : true,
        "Bluetooth" : true,
        "Cloud" : true,
        "Interface" : true,
        "Network" : true,
        "RPC" : true,
        "Thermostat" : true,
        "Screen" : true,
        "UART" : true,
        "Webhooks" : true,
        "WebSocket" : true
      }
    }
  },
  "cloud" : {
    "enable" : true,
    "server" : "shelly-36-eu.shelly.cloud:6022/jrpc"
  },
  "mqtt" : {
    "enable" : false,
    "client_id" : "ShellyWallDisplay-000822D5DAD8",
    "topic_prefix" : "ShellyWallDisplay-000822D5DAD8"
  },
  "ws" : {
    "enable" : false,
    "ssl_ca" : "ca.pem"
  },
  "thermostat:0" : {
    "id" : 0,
    "enable" : false,
    "sensor" : "shelly://shellywalldisplay-000822d5dad8/c/temperature:0",
    "type" : "heating",
    "actuator" : "shelly://shellyplus2pm-c049ef8e473c/c/switch:1:1",
    "hysteresis" : 0,
    "invert_output" : false,
    "display_unit" : "C",
    "target_C" : 11.5,
    "name" : null
  },
  "awaiting_auth_code" : false
}

GetStatus

{
  "ble" : { },
  "cloud" : {
    "connected" : true
  },
  "mqtt" : {
    "connected" : false
  },
  "temperature:0" : {
    "id" : 0,
    "tC" : 21.2,
    "tF" : 70.2
  },
  "humidity:0" : {
    "id" : 0,
    "rh" : 52.9
  },
  "illuminance:0" : {
    "id" : 0,
    "lux" : 30,
    "illumination" : "twilight"
  },
  "switch:0" : {
    "id" : 0,
    "output" : false,
    "source" : "Ui"
  },
  "input:0" : {
    "id" : 0,
    "state" : false
  },
  "sys" : {
    "id" : "ShellyWallDisplay-000822D5DAD8",
    "mac" : "000822D5DAD8",
    "model" : "SAWD-0A1XX10EU1",
    "gen" : 2,
    "fw_id" : "20240312-155021/1.2.9-30788ca8",
    "ver" : "1.2.9",
    "app" : "WallDisplay",
    "auth_en" : false,
    "uptime" : 2771769,
    "app_uptime" : 2771661,
    "discoverable" : false,
    "cfg_rev" : 8,
    "schedule_rev" : 0,
    "webhook_rev" : 0,
    "platform" : "vXD10000K",
    "available_updates" : { },
    "restart_required" : false,
    "unixtime" : 1713115414,
    "relay_in_thermostat" : false,
    "sensor_in_thermostat" : true
  },
  "wifi" : {
    "sta_ip" : "192.168.20.74",
    "status" : "got ip",
    "mac" : "00:08:22:D5:DA:D8",
    "ssid" : "Su Master",
    "rssi" : -75,
    "netmask" : "255.255.0.0",
    "gw" : "192.168.1.1",
    "nameserver" : "8.8.8.8"
  },
  "devicepower:0" : {
    "id" : 0,
    "battery" : null,
    "external" : {
      "present" : true
    }
  },
  "awaiting_auth_code" : false,
  "thermostat:0" : {
    "id" : 0,
    "enable" : false,
    "target_C" : 11.5,
    "current_C" : 21.2,
    "output" : false,
    "schedules" : {
      "enable" : false
    }
  }
}
*/