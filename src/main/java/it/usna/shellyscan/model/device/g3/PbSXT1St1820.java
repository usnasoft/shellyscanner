package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g3.modules.XT1Thermostat;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * LinkedGo ST1820 (PbS) model
 */
public class PbSXT1St1820 extends XT1 implements ModulesHolder {
	private final static Logger LOG = LoggerFactory.getLogger(PbSXT1St1820.class);
	public final static String MODEL = "S3XT-0S";
	public final static String SVC0_TYPE = "linkedgo-st1820-floor-thermostat";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H};
	private final static String CURRENT_TEMP_KEY = "number:201";
	private final static String CURRENT_HUM_KEY = "number:200";
	private final static String TARGET_TEMP_ID = "202";
	private final static String TARGET_TEMP_KEY = "number:" + TARGET_TEMP_ID;
	private final static String ENABLED_ID = "202";
	private final static String ENABLED_KEY = "boolean:" + ENABLED_ID;
	private float temp;
	private float humidity;
	private Meters[] meters;
	private XT1Thermostat thermostat;
	private XT1Thermostat[] thermostats;

	public PbSXT1St1820(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		thermostat = new XT1Thermostat(this, ENABLED_ID, TARGET_TEMP_ID);
		thermostats = new XT1Thermostat[] {thermostat};
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.H) {
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
		return "LinkedGo ST1820";
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);

		try { Thread.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		JsonNode sensors = getJSON("/rpc/Shelly.GetComponents?keys=[%22boolean:202%22,%22number:200%22,%22number:201%22,%22number:202%22]");
		for(JsonNode sensor: sensors.path("components")) {
			try {
				String key = sensor.get("key").textValue();
				if(CURRENT_TEMP_KEY.equals(key)) {
					boolean celsius = "°C".equals(sensor.path("config").path("meta").path("ui").path("unit").textValue());
					if(celsius) {
						temp = sensor.path("status").path("value").floatValue();
					} else {
						temp = (sensor.path("status").path("value").floatValue() - 32f) * (5f / 9f);
					}
				} else if(CURRENT_HUM_KEY.equals(key)) {
					humidity = sensor.path("status").path("value").floatValue();
				} else if(TARGET_TEMP_KEY.equals(key)) {
					thermostat.configTargetTemperature(sensor);
				} else if(ENABLED_KEY.equals(key)) {
					thermostat.configEnabled(sensor);
				}
			} catch(Exception e) {
				LOG.debug("err", e);
			}
		}
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public DeviceModule[] getModules() {
		return thermostats;
	}
	
	@Override
	protected void backup(ZipOutputStream out) throws IOException {
		sectionToStream("/rpc/Service.GetConfig?id=0", "Service.GetConfig.json", out);
	}
	
	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws IOException {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", 0);
		ObjectNode config = (ObjectNode)backupJsons.get("Service.GetConfig.json");
		config.remove("id");
		out.set("config", config);
		errors.add(postCommand("Service.SetConfig", out));
	}
}

/*
http://192.168.1.xxx/rpc/Shelly.GetComponents?keys=["boolean:202","number:200","number:201","number:202"]

{
    "cfg_rev": 68,
    "components": [
        {
            "attrs": {
                "owner": "service:0",
                "role": "enable"
            },
            "config": {
                "access": "crw",
                "default_value": false,
                "id": 202,
                "meta": {
                    "cloud": [
                        "log"
                    ],
                    "ui": {
                        "titles": [
                            "Disabled",
                            "Enable"
                        ],
                        "view": "toggle"
                    }
                },
                "name": "Enable thermostat",
                "owner": "service:0",
                "persisted": false
            },
            "key": "boolean:202",
            "status": {
                "last_update_ts": 1741854228,
                "source": "sys",
                "value": true
            }
        },
        {
            "attrs": {
                "owner": "service:0",
                "role": "current_humidity"
            },
            "config": {
                "access": "cr",
                "default_value": 0,
                "id": 200,
                "max": 100,
                "meta": {
                    "cloud": [
                        "measurement",
                        "log"
                    ],
                    "ui": {
                        "unit": "%",
                        "view": "label"
                    }
                },
                "min": 0,
                "name": "Current humidity",
                "owner": "service:0",
                "persisted": false
            },
            "key": "number:200",
            "status": {
                "last_update_ts": 1742914624,
                "source": "sys",
                "value": 56
            }
        },
        {
            "attrs": {
                "owner": "service:0",
                "role": "current_temperature"
            },
            "config": {
                "access": "cr",
                "default_value": 25,
                "id": 201,
                "max": 35,
                "meta": {
                    "cloud": [
                        "measurement",
                        "log"
                    ],
                    "ui": {
                        "unit": "°C",
                        "view": "label"
                    }
                },
                "min": 15,
                "name": "Current temperature",
                "owner": "service:0",
                "persisted": false
            },
            "key": "number:201",
            "status": {
                "last_update_ts": 1742914624,
                "source": "sys",
                "value": 22.6
            }
        },
        {
            "attrs": {
                "owner": "service:0",
                "role": "target_temperature"
            },
            "config": {
                "access": "crw",
                "default_value": 25,
                "id": 202,
                "max": 35,
                "meta": {
                    "cloud": [
                        "log"
                    ],
                    "ui": {
                        "step": 0.5,
                        "unit": "°C",
                        "view": "slider"
                    }
                },
                "min": 15,
                "name": "Target temperature",
                "owner": "service:0",
                "persisted": false
            },
            "key": "number:202",
            "status": {
                "last_update_ts": 1741854228,
                "source": "sys",
                "value": 25
            }
        }
    ],
    "offset": 0,
    "total": 4
}
*/

/*
{"method":"service.setconfig","id":13,"src":"f36b4d94-afd1-4a11-8ee9-eacb5a775b57","params":{"id":0,"config":{"temp_offset":0}}}

http://192.168.1.15/rpc/Service.getconfig?id=0
{"temp_range":[15,35],"temp_hysteresis":1,"power_down_memory":true,"temp_anti_freeze":5,"temp_offset":0,"humidity_offset":0,"id":0}
*/