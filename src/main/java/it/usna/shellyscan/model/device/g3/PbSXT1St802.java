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
import it.usna.shellyscan.model.device.modules.RelayInterface;

/**
 * LinkedGo ST802 (PbS) model
 */
public class PbSXT1St802 extends XT1 implements ModulesHolder {
	private final static Logger LOG = LoggerFactory.getLogger(PbSXT1St802.class);
	public final static String MODEL = "S3XT-0S";
	public final static String SVC0_TYPE = "linkedgo-st-802-hvac";
	public enum Mode { COOL, DRY, HEAT, VENTILATION };
	private Mode mode;
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H};
	private final static String CURRENT_TEMP_KEY = "number:201";
	private final static String CURRENT_HUM_KEY = "number:200";
	private final static String TARGET_TEMP_ID = "203";
	private final static String TARGET_TEMP_KEY = "number:" + TARGET_TEMP_ID;
//	private final static String TARGET_HUM_KEY = "number:202";
	private final static String ENABLED_ID = "201";
	private final static String ENABLED_KEY = "boolean:" + ENABLED_ID;
	private final static String MODE_KEY = "enum:201";
	private float temp;
	private float humidity;
	private Meters[] meters;
	private final XT1Thermostat thermostat;
	private XT1Thermostat[] thermostats;
	private RelayInterface humOnOff;
	private RelayInterface[] HumOnOffArray;

	public PbSXT1St802(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		thermostat = new XT1Thermostat(this, ENABLED_ID, TARGET_TEMP_ID);
		humOnOff = new ThermostatHumOnOff();

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
		JsonNode sensors = getJSON("/rpc/Shelly.GetComponents?keys=[%22boolean:201%22,%22number:200%22,%22number:201%22,%22number:202%22,%22number:203%22,%22enum:201%22]");
		for(JsonNode sensor: sensors.path("components")) {
			try {
				String key = sensor.get("key").textValue();
				if(MODE_KEY.equals(key)) {
					this.mode = Mode.valueOf(sensor.path("status").path("value").textValue().toUpperCase());
					if(mode == Mode.HEAT || mode == Mode.COOL || mode == Mode.VENTILATION) {
						if(thermostats == null) {
							thermostats = new XT1Thermostat[] {thermostat};
							HumOnOffArray = null;
						}
					} else if(HumOnOffArray == null) { // mode == Mode.DRY
						HumOnOffArray = new RelayInterface[] {humOnOff};
						thermostats = null;
					}
				} else if(CURRENT_TEMP_KEY.equals(key)) {
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
		return thermostats != null ? thermostats : HumOnOffArray;
	}
	
	@Override
	protected void backup(ZipOutputStream out) throws IOException, InterruptedException {
		sectionToStream("/rpc/Service.GetConfig?id=0", "Service.GetConfig.json", out);
	}
	
	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws IOException, InterruptedException {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", 0);
		ObjectNode config = (ObjectNode)backupJsons.get("Service.GetConfig.json");
		config.remove("id");
		config.remove("thermostat_mode"); // do not restore mode
		out.set("config", config);
		errors.add(postCommand("Service.SetConfig", out));
	}
	
	
	private class ThermostatHumOnOff implements RelayInterface {
//		private ThermostatInterface thermostat;
//		public ThermostatHumOnOff(ThermostatInterface thermostat) {
//			this.thermostat = thermostat;
//		}

		@Override
		public String getLabel() {
			return "Humidity";
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public boolean toggle() throws IOException {
			boolean en = thermostat.isEnabled() == false;
			thermostat.setEnabled(en);
			return en;
		}

		@Override
		public void change(boolean on) throws IOException {
			thermostat.setEnabled(on);
		}

		@Override
		public boolean isOn() {
			return thermostat.isEnabled();
		}

		@Override
		public boolean isInputOn() {
			return thermostat.isRunning();
		}
		
		@Override
		public String toString() {
			return "Humidity \"on\": " + isOn();
		}
	}
}

/*
{
    "cfg_rev": 80,
    "components": [
        {
            "attrs": {
                "owner": "service:0",
                "role": "enable"
            },
            "config": {
                "access": "crw",
                "default_value": false,
                "id": 201,
                "meta": {
                    "cloud": [
                        "log"
                    ],
                    "ui": {
                        "titles": [
                            "Disabled",
                            "Enabled"
                        ],
                        "view": "toggle"
                    }
                },
                "name": "Enable thermostat",
                "owner": "service:0",
                "persisted": false
            },
            "key": "boolean:201",
            "status": {
                "last_update_ts": 1742912337,
                "source": "rpc",
                "value": true
            }
        },
        {
            "attrs": {
                "owner": "service:0",
                "role": "working_mode"
            },
            "config": {
                "access": "crw",
                "default_value": "cool",
                "id": 201,
                "meta": {
                    "ui": {
                        "titles": {
                            "boost": "Boost",
                            "cool": "Cool",
                            "dry": "Dry",
                            "floor_heating": "Floor heating",
                            "heat": "Heat",
                            "ventilation": "Ventilation"
                        },
                        "view": "select"
                    }
                },
                "name": "Working mode",
                "options": [
                    "cool",
                    "dry",
                    "heat",
                    "ventilation"
                ],
                "owner": "service:0",
                "persisted": false
            },
            "key": "enum:201",
            "status": {
                "last_update_ts": 1742912340,
                "source": "sys",
                "value": "cool"
            }
        },
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
                "last_update_ts": 1742913469,
                "source": "sys",
                "value": 60
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
                "last_update_ts": 1742913589,
                "source": "sys",
                "value": 22.5
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
                "last_update_ts": 1742911915,
                "source": "rpc",
                "value": 56
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
                "last_update_ts": 1742911898,
                "source": "rpc",
                "value": 20.5
            }
        }
    ],
    "offset": 0,
    "total": 6
}
*/