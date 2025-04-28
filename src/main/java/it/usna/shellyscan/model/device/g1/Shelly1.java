package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g1.meters.MetersPower;
import it.usna.shellyscan.model.device.g1.modules.Relay;

/**
 * Shelly 1 model
 * @author usna
 */
public class Shelly1 extends AbstractG1Device implements ModulesHolder {
	public final static String ID = "SHSW-1";
	private final static Meters.Type[] SUPPORTED_MEASURES_H = new Meters.Type[] { Meters.Type.T, Meters.Type.H };
	private final static Meters.Type[] MEASURES_EXT_SWITCH = new Meters.Type[] { Meters.Type.EX };
	private Relay relay = new Relay(this, 0);
	private float extT0, extT1, extT2;
	private int humidity;
	private int extSwitchStatus;
	private boolean extSwitchRev;
	private Meters[] meters = null;

	public Shelly1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	protected void init() throws IOException {
		JsonNode settings = getJSON("/settings");
		this.hostname = settings.get("device").get("hostname").asText("");
		fillSettings(settings);
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		JsonNode status = getJSON("/status");
		fillStatus(status);

		ArrayList<Meters> m = new ArrayList<>(2);
		JsonNode extTNode;
		if (settings.path("ext_humidity").size() > 0) {
			m.add(new Meters() {
				@Override
				public Type[] getTypes() {
					return SUPPORTED_MEASURES_H;
				}

				@Override
				public float getValue(Type t) {
					return (t == Type.T) ? extT0 : humidity;
				}
			});
		} else if ((extTNode = settings.path("ext_temperature")).size() > 0) {
			final ArrayList<Meters.Type> tt = new ArrayList<>(3);
			if (extTNode.has("0"))
				tt.add(Meters.Type.T);
			if (extTNode.has("1"))
				tt.add(Meters.Type.T1);
			if (extTNode.has("2"))
				tt.add(Meters.Type.T2);
			final Meters.Type[] mTypes = tt.toArray(Meters.Type[]::new);
			m.add(new Meters() {
				@Override
				public Type[] getTypes() {
					return mTypes;
				}

				@Override
				public float getValue(Type t) {
					if (t == Type.T) {
						return extT0;
					} else if (t == Type.T1) {
						return extT1;
					} else {
						return extT2;
					}
				}// ext_switch_reverse
			});
		} else if (status.path("ext_switch").size() > 0) { // status
			m.add(new Meters() {
				@Override
				public Type[] getTypes() {
					return MEASURES_EXT_SWITCH;
				}

				@Override
				public float getValue(Type t) {
					if (extSwitchRev) {
						return extSwitchStatus == 0 ? 1f : 0f;
					} else {
						return extSwitchStatus;
					}
				}
			});
		}
		float pow = settings.get("relays").get(0).path("power").floatValue();
		if (pow > 0f) {
			m.add(new MetersPower() {
				@Override
				public float getValue(Type t) {
					return relay.isOn() ? pow : 0f;
				}
			});
		}
		
		if(m.size() > 0) {
			meters = m.toArray(Meters[]::new);
		}
	}

	@Override
	public String getTypeName() {
		return "Shelly 1";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	public Relay[] getModules() {
		return new Relay[] { relay };
	}

	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		relay.fillSettings(settings.get("relays").get(0));
		extSwitchRev = settings.path("ext_switch_reverse").asBoolean();
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay.fillStatus(status.get("relays").get(0), status.get("inputs").get(0));

		JsonNode extTNode = status.path("ext_temperature");
		if (extTNode.size() > 0) {
			extT0 = (float) extTNode.path("0").path("tC").asDouble();
			extT1 = (float) extTNode.path("1").path("tC").asDouble();
			extT2 = (float) extTNode.path("2").path("tC").asDouble();
		}
		JsonNode extHNode = status.path("ext_humidity");
		if (extHNode.size() > 0) {
			humidity = extHNode.path("0").path("hum").asInt();
		}

		JsonNode extSwitchNode = status.path("ext_switch");
		if (extSwitchNode.size() > 0) {
			extSwitchStatus = extSwitchNode.path("0").path("input").asInt();
		}
	}

	public String setPower(float power) {
		return sendCommand("/settings/power/0?power=" + power);
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "longpush_time", "factory_reset_from_switch",
				"wifirecovery_reboot_enabled", "ext_switch_enable", "ext_switch_reverse"/*, "eco_mode_enabled"*/) +
				"&ext_sensors_temperature_unit=" + settings.path("ext_sensors").path("temperature_unit")));

		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(settings.get("relays").get(0)));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); // Shelly 1 specific
		errors.add(setPower(settings.get("relays").get(0).get("power").floatValue()));

		for (int i = 0; i < 3; i++) {
			JsonNode extT = settings.path("ext_temperature").path(i + "");
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			errors.add(sendCommand("/settings/ext_temperature/" + i + "?" + jsonEntryIteratorToURLPar(extT.fields())));
			errors.add(sendCommand("/settings/ext_temperature/" + i + "?" + jsonEntrySetToURLPar(extT.properties())));
		}
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		errors.add(sendCommand("/settings/ext_humidity/0?" + jsonEntryIteratorToURLPar(settings.path("ext_humidity").path("0").fields())));
		errors.add(sendCommand("/settings/ext_humidity/0?" + jsonEntrySetToURLPar(settings.path("ext_humidity").path("0").properties())));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		errors.add(sendCommand("/settings/ext_switch/0?" + jsonEntryIteratorToURLPar(settings.path("ext_switch").path("0").fields())));
		errors.add(sendCommand("/settings/ext_switch/0?" + jsonEntrySetToURLPar(settings.path("ext_switch").path("0").properties())));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}

/*
settings example with add-on:

"ext_sensors" : {
  "temperature_unit" : "C"
},
"ext_temperature" : {
  "0" : {
    "overtemp_threshold_tC" : 0.0,
    "overtemp_threshold_tF" : 32.0,
    "undertemp_threshold_tC" : 0.0,
    "undertemp_threshold_tF" : 32.0,
    "overtemp_act" : "disabled",
    "undertemp_act" : "disabled",
    "offset_tC" : 0.0,
    "offset_tF" : 0.0
  }
},
"ext_humidity" : { },


status example with add-on:

 "ext_sensors" : {
  "temperature_unit" : "C"
},
"ext_temperature" : {
  "0" : {
    "hwID" : "281863eb2f1901ad",
    "tC" : 22.5,
    "tF" : 72.5
  }
},
"ext_humidity" : { },
*/