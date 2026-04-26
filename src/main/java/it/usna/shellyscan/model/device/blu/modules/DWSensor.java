package it.usna.shellyscan.model.device.blu.modules;

import it.usna.shellyscan.model.device.modules.DWInterface;
import tools.jackson.databind.JsonNode;

public class DWSensor extends Sensor implements DWInterface {
	public static final int OBJ_ID = 0x2d; // dec. 45
	private boolean open;

	DWSensor(int id, JsonNode sensorConf) {
		super(id, sensorConf);
		this.objID = OBJ_ID;
		this.mType = null;
	}
	
	@Override
	public void fill(JsonNode comp) {
		name = comp.path("config").path("name").asString("");
		open = comp.path("status").path("value").asBoolean(); // description: 0 (False = Closed) - 1 (True = Open)
	}

	@Override
	public boolean open() {
		return open;
	}
}

/* --- Blu Door Window

"key" : "bthomedevice:200",
"status" : {
  "id" : 200,
  "rssi" : -59,
  "battery" : 100,
  "packet_id" : 242,
  "last_updated_ts" : 1776872889,
  "paired" : false,
  "rpc" : false,
  "rsv" : -1,
  "fw_ver" : null
},
"config" : {
  "id" : 200,
  "addr" : "mac_addr",
  "name" : null,
  "key" : null,
  "meta" : {
    "ui" : {
      "view" : "regular"
    }
  }
},
"attrs" : {
  "flags" : 0,
  "model_id" : 2
}
}, {
"key" : "bthomesensor:200",
"status" : {
  "id" : 200,
  "value" : 100,
  "last_updated_ts" : 1776872889
},
"config" : {
  "id" : 200,
  "addr" : "mac_addr",
  "name" : null,
  "obj_id" : 1,
  "idx" : 0,
  "meta" : {
    "ui" : {
      "icon" : null
    }
  }
}
}, {
"key" : "bthomesensor:201",
"status" : {
  "id" : 201,
  "value" : 3.0,
  "last_updated_ts" : 1776872889
},
"config" : {
  "id" : 201,
  "addr" : "mac_addr",
  "name" : null,
  "obj_id" : 5,
  "idx" : 0,
  "meta" : null
}
}, {
"key" : "bthomesensor:202",
"status" : {
  "id" : 202,
  "value" : false,
  "last_updated_ts" : 1776872889
},
"config" : {
  "id" : 202,
  "addr" : "mac_addr",
  "name" : null,
  "obj_id" : 45,
  "idx" : 0,
  "meta" : null
}
}, {
"key" : "bthomesensor:203",
"status" : {
  "id" : 203,
  "value" : 0.0,
  "last_updated_ts" : 1776872889
},
"config" : {
  "id" : 203,
  "addr" : "mac_addr",
  "name" : null,
  "obj_id" : 63,
  "idx" : 0,
  "meta" : null
}
}
*/