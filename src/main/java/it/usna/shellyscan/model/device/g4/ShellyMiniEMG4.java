package it.usna.shellyscan.model.device.g4;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.g2.modules.EM1Manager;
import it.usna.shellyscan.model.device.g4.meters.EM1MiniMeters;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Em mini Gen 4 model
 */
public class ShellyMiniEMG4 extends AbstractG4Device {
//	private static final Logger LOG = LoggerFactory.getLogger(ShellyMiniEMG4.class);
	public static final String ID = "MiniEMG4";
	public static final String MODEL = "S4EM-001PXCEU16";
	private EM1MiniMeters meters0;
	private Meters meters[];

	public ShellyMiniEMG4(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		meters0 = new EM1MiniMeters(new EM1Manager(this, 0));
		meters = new Meters[] {meters0};
	}

	@Override
	public String getTypeName() {
		return "Shelly Mini EM G4";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getModelID() {
		return MODEL;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		meters0.fillSettings(configuration.get("em1:0"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		meters0.fillStatus(status.get("em1:0"));
	}
	
	@Override
	public String[] getInfoRequests() {
		return EM1Manager.getInfoRequests(super.getInfoRequests(), 0);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

		ObjectNode conf = RestoreUtil.createIndexedRestoreNode(config, "em1", 0);
		errors.add(postCommand("EM1.SetConfig", conf));
	}
}

/*
Get device Info
{
   "name" : "Consumo ufficio",
   "id" : "shellyemminig4-xxx",
   "mac" : "xxx",
   "slot" : 0,
   "batch" : "2536-Broadwell",
   "fw_sbits" : "00",
   "model" : "S4EM-001PXCEU16",
   "gen" : 4,
   "fw_id" : "20250915-120902/gb95ce8e",
   "ver" : "1.7.0-miniemg4prod0",
   "app" : "MiniEMG4",
   "auth_en" : false,
   "auth_domain" : null,
   "matter" : false
}

Get Config
{
   "ble" : {
     "enable" : false,
     "rpc" : {
       "enable" : true
     }
   },
   "bthome" : { },
   "cloud" : {
     "enable" : true,
     "server" : "shelly-195-eu.shelly.cloud:6022/jrpc"
   },
   "em1:0" : {
     "id" : 0,
     "name" : "Ufficio",
     "reverse" : false
   },
   "em1data:0" : { },
   "matter" : {
     "enable" : false
   },
   "modbus" : {
     "enable" : true
   },
   "mqtt" : {
     "enable" : false,
     "server" : null,
     "client_id" : "shellyemminig4-xxx",
     "user" : null,
     "ssl_ca" : null,
     "topic_prefix" : "shellyemminig4-xxx",
     "rpc_ntf" : true,
     "status_ntf" : false,
     "use_client_cert" : false,
     "enable_rpc" : true,
     "enable_control" : true
   },
   "sys" : {
     "device" : {
       "name" : "Consumo ufficio",
       "mac" : "xxx",
       "fw_id" : "20250915-120902/gb95ce8e",
       "discoverable" : true,
       "eco_mode" : false
     },
     "location" : {
       "tz" : "Europe/Rome",
       "lat" : 45.8638,
       "lon" : 12.4148
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
       "server" : "time.cloudflare.com"
     },
     "cfg_rev" : 14
   },
   "wifi" : {
     "ap" : {
       "ssid" : "ShellyEMMiniG4-xxx",
       "is_open" : true,
       "enable" : false,
       "range_extender" : {
         "enable" : false
       }
     },
     "sta" : {
       "ssid" : "asdnetmaya",
       "is_open" : false,
       "enable" : true,
       "ipv4mode" : "dhcp",
       "ip" : null,
       "netmask" : null,
       "gw" : null,
       "nameserver" : null
     },
     "sta1" : {
       "ssid" : "EOLO_243437",
       "is_open" : false,
       "enable" : true,
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

Get Staus
{
   "ble" : { },
   "bthome" : {
     "errors" : [ "bluetooth_disabled" ]
   },
   "cloud" : {
     "connected" : true
   },
   "em1:0" : {
     "id" : 0,
     "voltage" : 235.0,
     "current" : 0.625,
     "act_power" : 86.1,
     "freq" : 49.9,
     "calibration" : "factory"
   },
   "em1data:0" : {
     "id" : 0,
     "total_act_energy" : 36459.41,
     "total_act_ret_energy" : 0.0
   },
   "matter" : {
     "num_fabrics" : 0,
     "commissionable" : false
   },
   "modbus" : { },
   "mqtt" : {
     "connected" : false
   },
   "sys" : {
     "mac" : "xxx",
     "restart_required" : false,
     "time" : "20:16",
     "unixtime" : 1772651767,
     "last_sync_ts" : 1772010563,
     "uptime" : 1404502,
     "ram_size" : 345524,
     "ram_free" : 200664,
     "ram_min_free" : 188816,
     "fs_size" : 786432,
     "fs_free" : 335872,
     "cfg_rev" : 14,
     "kvs_rev" : 0,
     "schedule_rev" : 3,
     "webhook_rev" : 0,
     "btrelay_rev" : 0,
     "available_updates" : { },
     "reset_reason" : 1,
     "utc_offset" : 3600
   },
   "wifi" : {
     "sta_ip" : "192.168.1.168",
     "status" : "got ip",
     "ssid" : "asdnetmaya",
     "bssid" : "b0:19:21:7c:25:8d",
     "rssi" : -58,
     "sta_ip6" : [ "fe80::da85:acff:feee:bdfc" ]
   },
   "ws" : {
     "connected" : false
   }
}
*/