package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Roller;

/**
 * Dual Cover model
 * @author usna
 */
public class ShellyProDualCover extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "Pro4PM";
	public static final String MODEL = "SPSH-002PE16EU";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Roller roller0 = new Roller(this, 0);
	private Roller roller1 = new Roller(this, 1);
	private float internalTmp;
	private float power0, power1;
	private float voltage0, voltage1;
	private float current0, current1;
	private float pf0, pf1;
	private Meters[] meters;
	private Roller[] rollers = new Roller[] {roller0, roller1};

	public ShellyProDualCover(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters = new Meters[] {
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power0;
						} else if(t == Meters.Type.I) {
							return current0;
						} else if(t == Meters.Type.PF) {
							return pf0;
						} else {
							return voltage0;
						}
					}
				},
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power1;
						} else if(t == Meters.Type.I) {
							return current1;
						} else if(t == Meters.Type.PF) {
							return pf1;
						} else {
							return voltage1;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro Dual Cover";
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
	public Roller[] getModules() {
		return rollers;
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
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		roller0.fillSettings(configuration.get("cover:0"));
		roller1.fillSettings(configuration.get("cover:1"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode coverStatus0 = status.get("cover:0");
		roller0.fillStatus(coverStatus0);
		power0 = coverStatus0.get("apower").floatValue();
		voltage0 = coverStatus0.get("voltage").floatValue();
		current0 = coverStatus0.get("current").floatValue();
		pf0 = coverStatus0.get("pf").floatValue();

		JsonNode coverStatus1 = status.get("cover:1");
		roller1.fillStatus(coverStatus1);
		power1 = coverStatus1.get("apower").floatValue();
		voltage1 = coverStatus1.get("voltage").floatValue();
		current1 = coverStatus1.get("current").floatValue();
		pf1 = coverStatus1.get("pf").floatValue();

		internalTmp = coverStatus0.path("temperature").path("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws JsonProcessingException, InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 2));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 3));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

		errors.add(roller0.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(roller1.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Ui.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("ui")) + "}"));
	}

	@Override
	public String toString() {
		return super.toString() + " Roller0: " + roller0 + "; Roller1: " + roller1;
	}
}

/*
 * INFO
{
  "name" : "---",
  "id" : "shellypro2cover-xxxx",
  "mac" : "xxxx",
  "slot" : 0,
  "key" : "xxx",
  "batch" : "2316-Broadwell",
  "fw_sbits" : "04",
  "model" : "SPSH-002PE16EU",
  "gen" : 2,
  "fw_id" : "20241011-114451/1.4.4-g6d2a586",
  "ver" : "1.4.4",
  "app" : "Pro4PM",
  "auth_en" : false,
  "auth_domain" : null
}

* CONFIG
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
  "bthome" : { },
  "cloud" : {
    "enable" : true,
    "server" : "shelly-3-eu.shelly.cloud:6022/jrpc"
  },
  "cover:0" : {
    "id" : 0,
    "name" : "n0",
    "motor" : {
      "idle_power_thr" : 2.0,
      "idle_confirm_period" : 0.25
    },
    "maxtime_open" : 60.0,
    "maxtime_close" : 60.0,
    "initial_state" : "stopped",
    "invert_directions" : false,
    "in_mode" : "single",
    "swap_inputs" : false,
    "safety_switch" : {
      "enable" : false,
      "direction" : "both",
      "action" : "stop",
      "allowed_move" : null
    },
    "power_limit" : 4480,
    "voltage_limit" : 280,
    "undervoltage_limit" : 0,
    "current_limit" : 16.0,
    "obstruction_detection" : {
      "enable" : false,
      "direction" : "both",
      "action" : "stop",
      "power_thr" : 159,
      "holdoff" : 1.0
    }
  },
  "cover:1" : {
    "id" : 1,
    "name" : "n1",
    "motor" : {
      "idle_power_thr" : 2.0,
      "idle_confirm_period" : 0.25
    },
    "maxtime_open" : 60.0,
    "maxtime_close" : 60.0,
    "initial_state" : "stopped",
    "invert_directions" : false,
    "in_mode" : "single",
    "swap_inputs" : false,
    "safety_switch" : {
      "enable" : false,
      "direction" : "both",
      "action" : "stop",
      "allowed_move" : null
    },
    "power_limit" : 4480,
    "voltage_limit" : 280,
    "undervoltage_limit" : 0,
    "current_limit" : 16.0,
    "obstruction_detection" : {
      "enable" : false,
      "direction" : "both",
      "action" : "stop",
      "power_thr" : 159,
      "holdoff" : 1.0
    }
  },
  "eth" : {
    "enable" : true,
    "ipv4mode" : "dhcp",
    "ip" : null,
    "netmask" : null,
    "gw" : null,
    "nameserver" : null
  },
  "input:0" : {
    "id" : 0,
    "name" : null,
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
    "name" : null,
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
  "mqtt" : {
    "enable" : true,
    "server" : "192.168.10.2:1882",
    "client_id" : "shellypro2cover-xxxx",
    "user" : "device",
    "ssl_ca" : null,
    "topic_prefix" : "shellypro2cover-xxxx",
    "rpc_ntf" : true,
    "status_ntf" : true,
    "use_client_cert" : false,
    "enable_rpc" : true,
    "enable_control" : true
  },
  "sys" : {
    "device" : {
      "name" : "name",
      "mac" : "xxxx",
      "fw_id" : "20241011-114451/1.4.4-g6d2a586",
      "discoverable" : true,
      "eco_mode" : false
    },
    "location" : {
      "tz" : "Europe/Berlin",
      "lat" : 52.5343,
      "lon" : 10.1381
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
      "server" : "time.google.com"
    },
    "cfg_rev" : 39
  },
  "ui" : {
    "idle_brightness" : 30
  },
  "wifi" : {
    "ap" : {
      "ssid" : "ShellyPro2Cover-xxxx",
      "is_open" : true,
      "enable" : false,
      "range_extender" : {
        "enable" : false
      }
    },
    "sta" : {
      "ssid" : "shelly2",
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
      "is_open" : true,
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

* STATUS
    "errors" : [ "bluetooth_disabled" ]
  },
  "cloud" : {
    "connected" : true
  },
  "cover:0" : {
    "id" : 0,
    "source" : "limit_switch",
    "state" : "open",
    "apower" : 0.0,
    "voltage" : 240.8,
    "current" : 0.0,
    "pf" : 0.0,
    "freq" : 50.0,
    "aenergy" : {
      "total" : 573.284,
      "by_minute" : [ 0.0, 0.0, 0.0 ],
      "minute_ts" : 1741596360
    },
    "temperature" : {
      "tC" : 24.9,
      "tF" : 76.9
    },
    "pos_control" : true,
    "last_direction" : "open",
    "current_pos" : 100
  },
  "cover:1" : {
    "id" : 1,
    "source" : "limit_switch",
    "state" : "open",
    "apower" : 0.0,
    "voltage" : 241.0,
    "current" : 0.0,
    "pf" : 0.0,
    "freq" : 50.0,
    "aenergy" : {
      "total" : 571.034,
      "by_minute" : [ 0.0, 0.0, 0.0 ],
      "minute_ts" : 1741596360
    },
    "temperature" : {
      "tC" : 24.9,
      "tF" : 76.9
    },
    "pos_control" : true,
    "last_direction" : "open",
    "current_pos" : 100
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
  "mqtt" : {
    "connected" : true
  },
  "sys" : {
    "mac" : "xxxx",
    "restart_required" : false,
    "time" : "09:46",
    "unixtime" : 1741596406,
    "uptime" : 269406,
    "ram_size" : 247668,
    "ram_free" : 85112,
    "fs_size" : 524288,
    "fs_free" : 188416,
    "cfg_rev" : 39,
    "kvs_rev" : 0,
    "schedule_rev" : 0,
    "webhook_rev" : 0,
    "available_updates" : {
      "beta" : {
        "version" : "1.5.1-beta1"
      }
    },
    "reset_reason" : 3
  },
  "ui" : { },
  "wifi" : {
    "sta_ip" : "192.168.20.72",
    "status" : "got ip",
    "ssid" : "shelly2",
    "rssi" : -65
  },
  "ws" : {
    "connected" : false
  }
}
*/