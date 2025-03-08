package it.usna.shellyscan.model.device.g3;

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
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.meters.MetersWVI;

/**
 * Shelly mini 1PM G3 model
 * @author usna
 */
public class ShellyMini1PMG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	public final static String ID = "Mini1PMG3";
	private float internalTmp;
	private float power;
	private float voltage;
	private float current;
	private Meters[] meters;
	private Relay relay = new Relay(this, 0);
	private Relay[] relays = new Relay[] {relay};

	public ShellyMini1PMG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters = new MetersWVI[] {
				new MetersWVI() {
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
		return "Shelly Mini 1PM G3";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public Relay[] getModules() {
		return relays;
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
		relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get("input:0"));
		power = switchStatus.get("apower").floatValue();
		voltage = switchStatus.get("voltage").floatValue();
		current = switchStatus.get("current").floatValue();

		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}

/*
{
"ble": { },
"cloud": {
  "connected": true
},
"input:0": {
  "id": 0,
  "state": false
},
"mqtt": {
  "connected": false
},
"switch:0": {
  "id": 0,
  "source": "init",
  "output": false,
  "apower": 0.0,
  "voltage": 234.6,
  "freq": 50.1,
  "current": 0.000,
  "aenergy": {
    "total": 0.000,
    "by_minute": [
      0.000,
      0.000,
      0.000
    ],
    "minute_ts": 1708706580
  },
  "ret_aenergy": {
    "total": 0.000,
    "by_minute": [
      0.000,
      0.000,
      0.000
    ],
    "minute_ts": 1708706580
  },
  "temperature": {
    "tC": 43.2,
    "tF": 109.8
  }
},
"sys": {
  "mac": "xxxxxxx",
  "restart_required": false,
  "time": "16:43",
  "unixtime": 1708706590,
  "uptime": 218,
  "ram_size": 259236,
  "ram_free": 142112,
  "fs_size": 1048576,
  "fs_free": 716800,
  "cfg_rev": 13,
  "kvs_rev": 6,
  "schedule_rev": 0,
  "webhook_rev": 0,
  "available_updates": {
    "stable": {
      "version": "1.2.2"
    }
  },
  "reset_reason": 1
},
"wifi": {
  "sta_ip": "192.168.97.68",
  "status": "got ip",
  "ssid": "xxx - IoT",
  "rssi": -84
},
"ws": {
  "connected": false
}
}


http://192.168.97.68/rpc/Shelly.GetConfig
{
"ble": {
  "enable": false,
  "rpc": {
    "enable": true
  },
  "observer": {
    "enable": false
  }
},
"cloud": {
  "enable": true,
  "server": "shelly-7-eu.shelly.cloud:6022/jrpc"
},
"input:0": {
  "id": 0,
  "name": null,
  "type": "switch",
  "enable": true,
  "invert": false,
  "factory_reset": true
},
"mqtt": {
  "enable": false,
  "server": null,
  "client_id": "shelly1pmminig3-xxxxx",
  "user": null,
  "ssl_ca": null,
  "topic_prefix": "shelly1pmminig3-xxxxx",
  "rpc_ntf": true,
  "status_ntf": false,
  "use_client_cert": false,
  "enable_rpc": true,
  "enable_control": true
},
"switch:0": {
  "id": 0,
  "name": null,
  "in_mode": "follow",
  "initial_state": "match_input",
  "auto_on": false,
  "auto_on_delay": 60.00,
  "auto_off": false,
  "auto_off_delay": 60.00,
  "power_limit": 2240,
  "voltage_limit": 280,
  "autorecover_voltage_errors": false,
  "current_limit": 8.000
},
"sys": {
  "device": {
    "name": null,
    "mac": "5432044F8230",
    "fw_id": "20240130-115216/1.2.0-beta1-gd959786",
    "discoverable": true,
    "eco_mode": false
  },
  "location": {
    "tz": null,
    "lat": null,
    "lon": null
  },
  "debug": {
    "level": 2,
    "file_level": null,
    "mqtt": {
      "enable": false
    },
    "websocket": {
      "enable": false
    },
    "udp": {
      "addr": null
    }
  },
  "ui_data": { },
  "rpc_udp": {
    "dst_addr": null,
    "listen_port": null
  },
  "sntp": {
    "server": "time.google.com"
  },
  "cfg_rev": 13
},
"wifi": {
  "ap": {
    "ssid": "Shelly1PMMiniG3-xxxxx",
    "is_open": true,
    "enable": false,
    "range_extender": {
      "enable": false
    }
  },
  "sta": {
    "ssid": "xx - IoT",
    "is_open": false,
    "enable": true,
    "ipv4mode": "static",
    "ip": "192.168.97.68",
    "netmask": "255.255.255.0",
    "gw": "192.168.97.1",
    "nameserver": "1.1.1.1"
  },
  "sta1": {
    "ssid": null,
    "is_open": true,
    "enable": false,
    "ipv4mode": "dhcp",
    "ip": null,
    "netmask": null,
    "gw": null,
    "nameserver": null
  },
  "roam": {
    "rssi_thr": -80,
    "interval": 60
  }
},
"ws": {
  "enable": false,
  "server": null,
  "ssl_ca": "ca.pem"
}
}


http://192.168.97.68/rpc/Shelly.GetDeviceInfo
{
"name": null,
"id": "shelly1pmminig3-xxxxxx",
"mac": "xxxxxx",
"slot": 0,
"model": "S3SW-001P8EU",
"gen": 3,
"fw_id": "20240130-115216/1.2.0-beta1-gd959786",
"ver": "1.2.0-beta1",
"app": "Mini1PMG3",
"auth_en": true,
"auth_domain": "shelly1pmminig3-xxxxx"
}
*/