package it.usna.shellyscan.model.device.g3;

import java.net.InetAddress;

/**
 * Outdoor PlugS Gen3 model
 * (= PlugS Gen3 model)
 */
public class ShellyPlugSOutdoorG3 extends ShellyPlugSG3 {
	public static final String ID = "OutdoorPlugSG3";

	public ShellyPlugSOutdoorG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Outdoor Plug S G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	// matter ?
}

/*
GetDeviceInfo
*************

{
  "name" : "Outdoor PlugS Gen3",
  "id" : "shellyoutdoorsg3-123456789012",
  "mac" : "123456789012",
  "slot" : 1,
  "key" : "xxxx",
  "batch" : "2448-Broadwell",
  "fw_sbits" : "04",
  "model" : "S3PL-20112EU",
  "gen" : 3,
  "fw_id" : "20250924-062737/1.7.1-gd336f31",
  "ver" : "1.7.1",
  "app" : "OutdoorPlugSG3",
  "auth_en" : false,
  "auth_domain" : null,
  "matter" : true
}



GetConfig
*********

{
  "ble" : {
    "enable" : true,
    "rpc" : {
      "enable" : true
    }
  },
  "bthome" : { },
  "cloud" : {
    "enable" : true,
    "server" : "shelly-86-eu.shelly.cloud:6022/jrpc"
  },
  "knx" : {
    "enable" : false,
    "ia" : "15.15.255",
    "routing" : {
      "addr" : "224.0.23.12:3671"
    }
  },
  "matter" : {
    "enable" : true
  },
  "mqtt" : {
    "enable" : false,
    "server" : null,
    "client_id" : "shellyoutdoorsg3-123456789012",
    "user" : null,
    "ssl_ca" : null,
    "topic_prefix" : "shellyoutdoorsg3-123456789012",
    "rpc_ntf" : true,
    "status_ntf" : false,
    "use_client_cert" : false,
    "enable_rpc" : true,
    "enable_control" : true
  },
  "plugs_ui" : {
    "leds" : {
      "mode" : "switch",
      "colors" : {
        "switch:0" : {
          "on" : {
            "rgb" : [ 2.0, 100.0, 0.0 ],
            "brightness" : 35.0
          },
          "off" : {
            "rgb" : [ 100.0, 0.0, 0.0 ],
            "brightness" : 35.0
          }
        },
        "power" : {
          "brightness" : 100.0
        }
      },
      "night_mode" : {
        "enable" : false,
        "brightness" : 100.0,
        "active_between" : [ ]
      }
    },
    "controls" : {
      "switch:0" : {
        "in_mode" : "momentary"
      }
    }
  },
  "switch:0" : {
    "id" : 0,
    "name" : "Stecker 7 (Outdoor)",
    "initial_state" : "off",
    "auto_on" : false,
    "auto_on_delay" : 60.0,
    "auto_off" : false,
    "auto_off_delay" : 60.0,
    "power_limit" : 2000,
    "voltage_limit" : 250,
    "autorecover_voltage_errors" : false,
    "current_limit" : 10.0,
    "reverse" : false
  },
  "sys" : {
    "device" : {
      "name" : "Outdoor PlugS Gen3",
      "mac" : "123456789012",
      "fw_id" : "20250924-062737/1.7.1-gd336f31",
      "discoverable" : true,
      "eco_mode" : true
    },
    "location" : {
      "tz" : "Europe/Vienna",
      "lat" : 47.4658,
      "lon" : 9.7558
    },
    "debug" : {
      "level" : 2,
      "file_level" : null,
      "mqtt" : {
        "enable" : false
      },
      "websocket" : {
        "enable" : true
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
      "server" : "time.cloudflare.com"
    },
    "cfg_rev" : 29
  },
  "wifi" : {
    "ap" : {
      "ssid" : "ShellyOutdoorSG3-123456789012",
      "is_open" : true,
      "enable" : true,
      "range_extender" : {
        "enable" : false
      }
    },
    "sta" : {
      "ssid" : "xxx.Net1",
      "is_open" : false,
      "enable" : true,
      "ipv4mode" : "static",
      "ip" : "192.168.x.x",
      "netmask" : "255.255.255.0",
      "gw" : "192.168.x.1",
      "nameserver" : "192.168.x.1"
    },
    "sta1" : {
      "ssid" : "xxx.Net1_2G_EXT",
      "is_open" : false,
      "enable" : true,
      "ipv4mode" : "static",
      "ip" : "192.168.x.x",
      "netmask" : "255.255.255.0",
      "gw" : "192.168.x.1",
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




GetStatus
*********

{
  "ble" : { },
  "bthome" : { },
  "cloud" : {
    "connected" : true
  },
  "knx" : { },
  "matter" : {
    "num_fabrics" : 0,
    "commissionable" : false
  },
  "mqtt" : {
    "connected" : false
  },
  "plugs_ui" : { },
  "switch:0" : {
    "id" : 0,
    "source" : "SHC",
    "output" : true,
    "apower" : 0.0,
    "voltage" : 237.6,
    "freq" : 50.0,
    "current" : 0.0,
    "aenergy" : {
      "total" : 0.0,
      "by_minute" : [ 0.0, 0.0, 0.0 ],
      "minute_ts" : 1759669980
    },
    "ret_aenergy" : {
      "total" : 0.0,
      "by_minute" : [ 0.0, 0.0, 0.0 ],
      "minute_ts" : 1759669980
    },
    "temperature" : {
      "tC" : 45.9,
      "tF" : 114.6
    }
  },
  "sys" : {
    "mac" : "123456789012",
    "restart_required" : false,
    "time" : "15:13",
    "unixtime" : 1759670022,
    "last_sync_ts" : 1759668608,
    "uptime" : 68085,
    "ram_size" : 258820,
    "ram_free" : 64436,
    "ram_min_free" : 50580,
    "fs_size" : 917504,
    "fs_free" : 471040,
    "cfg_rev" : 29,
    "kvs_rev" : 0,
    "schedule_rev" : 0,
    "webhook_rev" : 0,
    "btrelay_rev" : 2,
    "available_updates" : { },
    "reset_reason" : 1,
    "utc_offset" : 7200
  },
  "wifi" : {
    "sta_ip" : "192.168.1.86",
    "status" : "got ip",
    "ssid" : "xxx.Net1",
    "bssid" : "08:b6:57:10:04:06",
    "rssi" : -66,
    "sta_ip6" : [ "fe80::e6b0:63ff:fedc:a2c4" ]
  },
  "ws" : {
    "connected" : false
  }
}
*/