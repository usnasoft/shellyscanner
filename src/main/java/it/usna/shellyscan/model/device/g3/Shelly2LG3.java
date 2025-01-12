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
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Shelly 2L G3 model 
 * @author usna
 */
public class Shelly2LG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder/*, SensorAddOnHolder*/ {
//	private final static Logger LOG = LoggerFactory.getLogger(Shelly2LG3.class);
	public final static String ID = "S2LG3";
//	private boolean modeRelay;
//	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay[] relaysArray = new Relay[] {relay0, relay1};
//	private Roller roller;
//	private Roller[] rollersArray;
	private float internalTmp;
//	private float power0, power1;
//	private float voltage0, voltage1;
//	private float current0, current1;
//	private Meters meters0, meters1;
//	private float pf0, pf1;
//	private Meters[] meters;
//	private SensorAddOn addOn;

//	private final static String MODE_RELAY = "switch";

	public Shelly2LG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		
//		final JsonNode config = configure();
		
//		meters0 = new Meters() {
//			public Type[] getTypes() {
//				return SUPPORTED_MEASURES;
//			}
//
//			@Override
//			public float getValue(Type t) {
//				if(t == Meters.Type.W) {
//					return power0;
//				} else if(t == Meters.Type.I) {
//					return current0;
//				} else if(t == Meters.Type.PF) {
//					return pf0;
//				} else {
//					return voltage0;
//				}
//			}
//		};
//		meters1 = new Meters() {
//			public Type[] getTypes() {
//				return SUPPORTED_MEASURES;
//			}
//
//			@Override
//			public float getValue(Type t) {
//				if(t == Meters.Type.W) {
//					return power1;
//				} else if(t == Meters.Type.I) {
//					return current1;
//				} else if(t == Meters.Type.PF) {
//					return pf1;
//				} else {
//					return voltage1;
//				}
//			}
//		};
		
		fillSettings(getJSON("/rpc/Shelly.GetConfig"));
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
//	private JsonNode configure() throws IOException {
//		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
//		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
//			addOn = new SensorAddOn(this);
//		} else {
//			addOn = null;
//		}
//		return config;
//	}

	@Override
	public String getTypeName() {
		return "Shelly 2L G3";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getModulesCount() {
		return 2/*modeRelay ? 2 : 1*/;
	}
	
	@Override
	public DeviceModule getModule(int index) {
//		if(modeRelay) {
			return (index == 0) ? relay0 : relay1;
//		} else {
//			return roller;
//		}
	}

	@Override
	public DeviceModule[] getModules() {
		return relaysArray /*modeRelay ? relaysArray : rollersArray*/;
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
//
//	@Override
//	public Meters[] getMeters() {
//		return meters;
//	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
//		modeRelay = configuration.get("sys").get("device").get("profile").asText().equals(MODE_RELAY);
//		if(modeRelay) {
//			if(relay0 == null /*|| relay1 == null*/) {
//				relay0 = new Relay(this, 0);
//				relay1 = new Relay(this, 1);
//				relaysArray = new Relay[] {relay0, relay1};
////				meters = (addOn == null || addOn.getTypes().length == 0) ? new Meters[] {meters0, meters1} : new Meters[] {meters0, meters1, addOn};
//				roller = null; // modeRelay change
//				rollersArray = null;
//			}
			relay0.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
			relay1.fillSettings(configuration.get("switch:1"), configuration.get("input:1"));
//		} else {
//			if(roller == null) {
//				roller = new Roller(this, 0);
//				rollersArray = new Roller[] {roller};
//				meters = (addOn == null || addOn.getTypes().length == 0) ? new Meters[] {meters0} : new Meters[] {meters0, addOn};
//				relay0 = relay1 = null; // modeRelay change
//				relaysArray = null;
//			}
//			roller.fillSettings(configuration.get("cover:0"));
//		}
//		if(addOn != null) {
//			addOn.fillSettings(configuration);
//		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
//		if(modeRelay) {
			JsonNode switchStatus0 = status.get("switch:0");
			relay0.fillStatus(switchStatus0, status.get("input:0"));
//			power0 = switchStatus0.path("apower").floatValue();
//			voltage0 = switchStatus0.path("voltage").floatValue();
//			current0 = switchStatus0.path("current").floatValue();
//			pf0 = switchStatus0.path("pf").floatValue();
//
			JsonNode switchStatus1 = status.get("switch:1");
			relay1.fillStatus(switchStatus1, status.get("input:1"));
//			power1 = switchStatus1.path("apower").floatValue();
//			voltage1 = switchStatus1.path("voltage").floatValue();
//			current1 = switchStatus1.path("current").floatValue();
//			pf1 = switchStatus1.path("pf").floatValue();

			internalTmp = (float)switchStatus0.path("temperature").path("tC").floatValue();
//		} else {
//			JsonNode cover = status.get("cover:0");
//			power0 = cover.path("apower").floatValue();
//			voltage0 = cover.path("voltage").floatValue();
//			current0 = cover.path("current").floatValue();
//			pf0 = cover.path("pf").floatValue();
//			internalTmp = cover.path("temperature").path("tC").floatValue();
//			roller.fillStatus(cover);
//		}
//		if(addOn != null) {
//			addOn.fillStatus(status);
//		}
	}
	
//	@Override
//	public String[] getInfoRequests() {
//		final String[] cmd = super.getInfoRequests();
//		return (addOn != null) ? SensorAddOn.getInfoRequests(cmd) : cmd;
//	}
	
//	public void setProfile(boolean cover) {
//		postCommand("Shelly.SetProfile", "{\"name\":\"" + (cover ? "cover" : "switch")  +"\"}");
//	}
	
//	@Override
//	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
//		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
//		boolean backModeRelay = MODE_RELAY.equals(devInfo.get("profile").asText());
//		if(backModeRelay != modeRelay) {
//			res.put(RestoreMsg.ERR_RESTORE_MODE_COVER, null);
//		}
//		try {
//			configure(); // maybe useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
//		} catch (IOException e) {
//			LOG.error("restoreCheck", e);
//		}
//		SensorAddOn.restoreCheck(this, backupJsons, res);
//	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
//		final boolean backModeRelay = MODE_RELAY.equals(configuration.at("/sys/device/profile").asText());
//		if(backModeRelay == modeRelay) {
			errors.add(Input.restore(this,configuration, 0));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(Input.restore(this,configuration, 1));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			if(backModeRelay) {
				errors.add(relay0.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(relay1.restore(configuration));
//			} else {
//				errors.add(roller.restore(configuration));
//			}
//		} else {
//			errors.add(RestoreMsg.ERR_RESTORE_MODE_COVER.name());
//		}

//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		SensorAddOn.restore(this, backupJsons, errors);
	}
	
//	@Override
//	public SensorAddOn getSensorAddOn() {
//		return addOn;
//	}

	@Override
	public String toString() {
//		if(modeRelay) {
			return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
//		} else {
//			return super.toString() + " Cover: " + roller;
//		}
	}
}