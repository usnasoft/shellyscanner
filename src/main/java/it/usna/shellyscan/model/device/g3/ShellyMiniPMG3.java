package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

/**
 * Shelly Shelly Plus mini PM model
 * @author usna
 */
public class ShellyMiniPMG3 extends AbstractG3Device {
	public final static String ID = "MiniPMG3";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.V, Meters.Type.I};
	private float power;
	private float voltage;
	private float current;
	private Meters[] meters;

	public ShellyMiniPMG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters = new Meters[] {
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power;
						} else if(t == Meters.Type.I) {
							return current;
						} else {
							return voltage;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Mini PM G3";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

//	@Override
//	protected void fillSettings(JsonNode configuration) throws IOException {
//		super.fillSettings(configuration);
//	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode pm1 = status.get("pm1:0");
		power = pm1.get("apower").floatValue();
		voltage = pm1.get("voltage").floatValue();
		current = pm1.get("current").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
//		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
//		ObjectNode pm1 = (ObjectNode)config.path("pm1:0").deepCopy();
//		pm1.remove("id");
//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		errors.add(postCommand("PM1.SetConfig", pm1));
	}
}

/*
{
  "name" : null,
  "id" : "shellypmminig3-ZZZZZZZZZZZZ",
  "mac" : "ZZZZZZZZZZZZ",
  "slot" : 0,
  "model" : "S3PM-001PCEU16",
  "gen" : 3,
  "fw_id" : "20240223-141904/1.2.2-g7c39781",
  "ver" : "1.2.2",
  "app" : "MiniPMG3",
  "auth_en" : false,
  "auth_domain" : null
}

---------------

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
  "cloud" : {
    "enable" : true,
    "server" : "shelly-19-eu.shelly.cloud:6022/jrpc"
  },
  "mqtt" : {
    "enable" : false,
    "server" : null,
    "client_id" : "shellypmminig3-ZZZZZZZZZZZZ",
    "user" : null,
    "ssl_ca" : null,
    "topic_prefix" : "shellypmminig3-ZZZZZZZZZZZZ",
    "rpc_ntf" : true,
    "status_ntf" : false,
    "use_client_cert" : false,
    "enable_rpc" : true,
    "enable_control" : true
  },
  "pm1:0" : {
    "id" : 0,
    "name" : null
  },
  "sys" : {
    "device" : {
      "name" : null,
      "mac" : "ZZZZZZZZZZZZ",
      "fw_id" : "20240223-141904/1.2.2-g7c39781",
      "discoverable" : true,
      "eco_mode" : true
    },
    "location" : {
      "tz" : "Asia/Jerusalem",
      "lat" : 32.816,
      "lon" : 34.9821
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
      "server" : "time.google.com"
    },
    "cfg_rev" : 17
  },
  "wifi" : {
    "ap" : {
      "ssid" : "ShellyPMMiniG3-ZZZZZZZZZZZZ",
      "is_open" : true,
      "enable" : false,
      "range_extender" : {
        "enable" : false
      }
    },
    "sta" : {
      "ssid" : "NotYourSSID",
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

------------------

{
  "ble" : { },
  "cloud" : {
    "connected" : true
  },
  "mqtt" : {
    "connected" : false
  },
  "pm1:0" : {
    "id" : 0,
    "voltage" : 221.8,
    "current" : 0.0,
    "apower" : 0.0,
    "freq" : 50.1,
    "aenergy" : {
      "total" : 176.0,
      "by_minute" : [ 0.0, 0.0, 0.0 ],
      "minute_ts" : 1708946700
    },
    "ret_aenergy" : {
      "total" : 0.0,
      "by_minute" : [ 0.0, 0.0, 0.0 ],
      "minute_ts" : 1708946700
    }
  },
  "sys" : {
    "mac" : "ZZZZZZZZZZZZ",
    "restart_required" : false,
    "time" : "13:25",
    "unixtime" : 1708946733,
    "uptime" : 170274,
    "ram_size" : 260488,
    "ram_free" : 146400,
    "fs_size" : 1048576,
    "fs_free" : 720896,
    "cfg_rev" : 17,
    "kvs_rev" : 47,
    "schedule_rev" : 0,
    "webhook_rev" : 0,
    "available_updates" : { },
    "reset_reason" : 3
  },
  "wifi" : {
    "sta_ip" : "192.168.0.114",
    "status" : "got ip",
    "ssid" : "NotYourSSID",
    "rssi" : -73
  },
  "ws" : {
    "connected" : false
  }
}
*/