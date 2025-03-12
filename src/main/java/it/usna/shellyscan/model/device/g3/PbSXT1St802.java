package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;

public class PbSXT1St802 extends XT1 {
	public final static String MODEL = "S3XT-0S";
	public final static String SVC0_TYPE = "linkedgo-st-802-hvac";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H};
	private final static String CURRENT_TEMP_KEY = "number:201";
	private final static String CURRENT_HUM_KEY = "number:200";
//	private final static String TARGET_TEMP_KEY = "number:203";
//	private final static String TARGET_HUM_KEY = "number:202";
	private float temp;
	private float humidity;
	private Meters[] meters;
	

	public PbSXT1St802(InetAddress address, int port, String hostname) {
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
		return "LinkedGo ST802";
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		
		try { Thread.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		JsonNode sensors = getJSON("/rpc/Shelly.GetComponents?keys=[%22boolean:202%22,%22number:200%22,%22number:201%22,%22number:202%22,%22number:203%22]");
		for(JsonNode sensor: sensors.path("components")) {
			if(CURRENT_TEMP_KEY.equals(sensor.get("key").textValue())) {
				boolean celsius = "°C".equals(sensor.path("config").path("meta").path("ui").path("unit").textValue());
				if(celsius) {
					temp = sensor.path("status").path("value").floatValue();
				} else {
					temp = (sensor.path("status").path("value").floatValue() - 32f) * (5f / 9f);
				}
			} else if(CURRENT_HUM_KEY.equals(sensor.get("key").textValue())) {
				humidity = sensor.path("status").path("value").floatValue();
			}
		}
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
}

/*
http://192.168.1.xxx/rpc/Shelly.GetComponents?keys=["boolean:202","number:200","number:201","number:202","number:203"]
{
    "cfg_rev": 50,
    "components": [
        {
            "attrs": {
                "owner": "service:0",
                "role": "current_humidity"
            },
            "config": {
                "access": "crw",
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
                "last_update_ts": 1741446533,
                "source": "sys",
                "value": 62
            }
        },
        {
            "attrs": {
                "owner": "service:0",
                "role": "current_temperature"
            },
            "config": {
                "access": "crw",
                "default_value": 20,
                "id": 201,
                "max": 35,
                "meta": {
                    "cloud": [
                        "measurement",
                        "log"
                    ],
                    "ui": {
                        "step": 0.5,
                        "unit": "°C",
                        "view": "label"
                    }
                },
                "min": 5,
                "name": "Current temperature",
                "owner": "service:0",
                "persisted": false
            },
            "key": "number:201",
            "status": {
                "last_update_ts": 1741447434,
                "source": "sys",
                "value": 20.1
            }
        },
        {
            "attrs": {
                "owner": "service:0",
                "role": "target_humidity"
            },
            "config": {
                "access": "crw",
                "default_value": 45,
                "id": 202,
                "max": 75,
                "meta": {
                    "cloud": [
                        "log"
                    ],
                    "ui": {
                        "unit": "%",
                        "view": "slider"
                    }
                },
                "min": 40,
                "name": "Target humidity",
                "owner": "service:0",
                "persisted": false
            },
            "key": "number:202",
            "status": {
                "last_update_ts": 1741277090,
                "source": "sys",
                "value": 54
            }
        },
        {
            "attrs": {
                "owner": "service:0",
                "role": "target_temperature"
            },
            "config": {
                "access": "crw",
                "default_value": 20,
                "id": 203,
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
                "min": 5,
                "name": "Target temperature",
                "owner": "service:0",
                "persisted": false
            },
            "key": "number:203",
            "status": {
                "last_update_ts": 1741277090,
                "source": "sys",
                "value": 23
            }
        }
    ],
    "offset": 0,
    "total": 4
}
*/