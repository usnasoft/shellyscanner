package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;

/**
 * Shelly mini 1 G3 model
 * @author usna
 */
public class ShellyMini1G3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "Mini1G3";
	public static final String MODEL = "S4SW-001X8EU";
	private Relay relay = new Relay(this, 0);
	private Relay[] relays = new Relay[] {relay};
	private float internalTmp;

	public ShellyMini1G3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Mini 1 G3";
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
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get("input:0"));

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
  "name" : null,
  "id" : "shelly1minig3-ZZZZZZZZZZZZ",
  "mac" : "ZZZZZZZZZZZZ",
  "slot" : 0,
  "model" : "S3SW-001X8EU",
  "gen" : 3,
  "fw_id" : "20240223-141905/1.2.2-g7c39781",
  "ver" : "1.2.2",
  "app" : "Mini1G3",
  "auth_en" : false,
  "auth_domain" : null
}

------

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
  "input:0" : {
    "id" : 0,
    "name" : null,
    "type" : "switch",
    "enable" : true,
    "invert" : false,
    "factory_reset" : true
  },
  "mqtt" : {
    "enable" : false,
    "server" : null,
    "client_id" : "shelly1minig3-ZZZZZZZZZZZZ",
    "user" : null,
    "ssl_ca" : null,
    "topic_prefix" : "shelly1minig3-ZZZZZZZZZZZZ",
    "rpc_ntf" : true,
    "status_ntf" : false,
    "use_client_cert" : false,
    "enable_rpc" : true,
    "enable_control" : true
  },
  "switch:0" : {
    "id" : 0,
    "name" : null,
    "in_mode" : "follow",
    "initial_state" : "match_input",
    "auto_on" : false,
    "auto_on_delay" : 60.0,
    "auto_off" : false,
    "auto_off_delay" : 60.0
  },
  "sys" : {
    "device" : {
      "name" : null,
      "mac" : "ZZZZZZZZZZZZ",
      "fw_id" : "20240223-141905/1.2.2-g7c39781",
      "discoverable" : true,
      "eco_mode" : false
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
    "cfg_rev" : 14
  },
  "wifi" : {
    "ap" : {
      "ssid" : "shelly1minig3-ZZZZZZZZZZZZ",
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

---------

{
  "ble" : { },
  "cloud" : {
    "connected" : true
  },
  "input:0" : {
    "id" : 0,
    "state" : false
  },
  "mqtt" : {
    "connected" : false
  },
  "switch:0" : {
    "id" : 0,
    "source" : "init",
    "output" : false,
    "temperature" : {
      "tC" : 45.8,
      "tF" : 114.5
    }
  },
  "sys" : {
    "mac" : "ZZZZZZZZZZZZ",
    "restart_required" : false,
    "time" : "14:11",
    "unixtime" : 1708776675,
    "uptime" : 236,
    "ram_size" : 259956,
    "ram_free" : 147928,
    "fs_size" : 1048576,
    "fs_free" : 716800,
    "cfg_rev" : 14,
    "kvs_rev" : 0,
    "schedule_rev" : 0,
    "webhook_rev" : 0,
    "available_updates" : { },
    "reset_reason" : 3
  },
  "wifi" : {
    "sta_ip" : "192.168.0.108",
    "status" : "got ip",
    "ssid" : "NotYourSSID",
    "rssi" : -81
  },
  "ws" : {
    "connected" : false
  }
}
 */