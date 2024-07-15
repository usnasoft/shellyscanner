package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.Roller;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.ModulesHolder;

public class ShellyPro2PM extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public final static String ID = "Pro2PM";
	private final static String MSG_RESTORE_MODE_ERROR = "msgRestoreCoverMode";
	private final static String MSG_RESTORE_MODE_SYNT_ERROR = "msgRestoreCoverModeSynt";
	private boolean modeRelay;
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay0, relay1;
	private Relay[] relaysArray;
	private Roller roller;
	private Roller[] rollersArray;
	private float internalTmp;
	private float power0, power1;
	private float voltage0, voltage1;
	private float current0, current1;
	private float pf0, pf1;
	private Meters meters0, meters1;
	private Meters[] meters;

	private final static String MODE_RELAY = "switch";

	public ShellyPro2PM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters0 = new Meters() {
			public Type[] getTypes() {
				return SUPPORTED_MEASURES;
			}

			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power0;
				} else if(t == Meters.Type.I) {
					return current0;
				} else if(t == Meters.Type.PF) {
					return pf0;
				} else {
					return voltage0;
				}
			}
		};
		meters1 = new Meters() {
			public Type[] getTypes() {
				return SUPPORTED_MEASURES;
			}

			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power1;
				} else if(t == Meters.Type.I) {
					return current1;
				} else if(t == Meters.Type.PF) {
					return pf1;
				} else {
					return voltage1;
				}
			}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro 2PM";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getModulesCount() {
		return modeRelay ? 2 : 1;
	}
	
	@Override
	public DeviceModule getModule(int index) {
		if(modeRelay) {
			return (index == 0) ? relay0 : relay1;
		} else {
			return roller;
		}
	}

	@Override
	public DeviceModule[] getModules() {
		return modeRelay ? relaysArray : rollersArray;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	//	public float getPower() {
	//		return power0;
	//	}
	//
	//	public float getVoltage() {
	//		return voltage0;
	//	}
	//
	//	public float getCurrent() {
	//		return current0;
	//	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		modeRelay = configuration.get("sys").get("device").get("profile").asText().equals(MODE_RELAY);
		if(modeRelay) {
			if(relay0 == null /*|| relay1 == null*/) {
				relay0 = new Relay(this, 0);
				relay1 = new Relay(this, 1);
				relaysArray = new Relay[] {relay0, relay1};
				meters = new Meters[] {meters0, meters1};
				roller = null; // modeRelay change
				rollersArray = null;
			}
			relay0.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
			relay1.fillSettings(configuration.get("switch:1"), configuration.get("input:1"));
		} else {
			if(roller == null) {
				roller = new Roller(this, 0);
				rollersArray = new Roller[] {roller};
				meters = new Meters[] {meters0};
				relay0 = relay1 = null; // modeRelay change
				relaysArray = null;
			}
			roller.fillSettings(configuration.get("cover:0"));
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(modeRelay) {
			JsonNode switchStatus0 = status.get("switch:0");
			relay0.fillStatus(switchStatus0, status.get("input:0"));
			power0 = switchStatus0.get("apower").floatValue();
			voltage0 = switchStatus0.get("voltage").floatValue();
			current0 = switchStatus0.get("current").floatValue();
			pf0 = switchStatus0.get("pf").floatValue();

			JsonNode switchStatus1 = status.get("switch:1");
			relay1.fillStatus(switchStatus1, status.get("input:1"));
			power1 = switchStatus1.get("apower").floatValue();
			voltage1 = switchStatus1.get("voltage").floatValue();
			current1 = switchStatus1.get("current").floatValue();
			pf1 = switchStatus1.get("pf").floatValue();

			internalTmp = switchStatus0.path("temperature").path("tC").floatValue();
		} else {
			JsonNode cover = status.get("cover:0");
			power0 = cover.get("apower").floatValue();
			voltage0 = cover.get("voltage").floatValue();
			current0 = cover.get("current").floatValue();
			pf0 = cover.get("pf").floatValue();
			internalTmp = cover.path("temperature").path("tC").floatValue();
			roller.fillStatus(cover);
		}
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<Restore, Object> res) {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		boolean backModeRelay = MODE_RELAY.equals(devInfo.get("profile").asText());
		if(backModeRelay != modeRelay) {
			res.put(Restore.ERR_RESTORE_MSG, MSG_RESTORE_MODE_ERROR);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		final boolean backModeRelay = MODE_RELAY.equals(configuration.at("/sys/device/profile").asText());
		if(backModeRelay == modeRelay) {
			errors.add(Input.restore(this, configuration, "0"));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(Input.restore(this,configuration, "1"));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			if(backModeRelay) {
				errors.add(relay0.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(relay1.restore(configuration));
			} else {
				errors.add(roller.restore(configuration));
			}
		} else {
			errors.add(MSG_RESTORE_MODE_SYNT_ERROR);
		}
	}

	@Override
	public String toString() {
		if(modeRelay) {
			return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
		} else {
			return super.toString() + " Cover: " + roller;
		}
	}
}

/*
{
"name" : null,
"id" : "shellypro2pm-xxx",
"mac" : "xxx",
"model" : "SPSW-202PE16EU",
"gen" : 2,
"fw_id" : "20221206-143638/0.12.0-gafc2404",
"ver" : "0.12.0",
"app" : "Pro2PM",
"auth_en" : false,
"auth_domain" : null,
"profile" : "switch"
}
 */