package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Shelly plus 2PM model 
 * @author usna
 */
public class ShellyPlusRGBW extends AbstractG2Device implements ModulesHolder, InternalTmpHolder, SensorAddOnHolder {
	private final static Logger LOG = LoggerFactory.getLogger(ShellyPlusRGBW.class);
	public enum Mode {
		LIGHT("light"),
		RGB("rgb"),
		RGBW("rgbw");
		
		private final String code;
		
		private Mode(String code) {
			this.code = code;
		}
	};
	public final static String ID = "PlusRGBWPM";
	//	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.V}; // not calibrated
	private Mode profile;
//	private Relay relay0, relay1;
//	private Relay[] relaysArray;
	private float internalTmp;
	private float power0, power1;
	private float voltage0, voltage1, voltage2, voltage3;
	private float current0, current1;
//	private float pf0, pf1;
	private LightWhite light0, light1, light2, light3;
	private LightWhite[] lights;
	private Meters meters0, meters1, meters2, meters3;
	private Meters[] meters;
	private SensorAddOn addOn;

//	private final static String MODE_LIGHT = "light";
//	private final static String MODE_RGB = "rgb";

	public ShellyPlusRGBW(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();

		final JsonNode config = configure();

		meters0 = new Meters() {
			public Type[] getTypes() {
				return SUPPORTED_MEASURES;
			}

			@Override
			public float getValue(Type t) {
				//				if(t == Meters.Type.W) {
				//					return power0;
				//				} else if(t == Meters.Type.I) {
				//					return current0;
				//				} else if(t == Meters.Type.PF) {
				//					return pf0;
				//				} else {
				return voltage0;
				//				}
			}
		};
		meters1 = new Meters() {
			public Type[] getTypes() {
				return SUPPORTED_MEASURES;
			}

			@Override
			public float getValue(Type t) {
				//				if(t == Meters.Type.W) {
				//					return power1;
				//				} else if(t == Meters.Type.I) {
				//					return current1;
				//				} else if(t == Meters.Type.PF) {
				//					return pf1;
				//				} else {
				return voltage1;
				//				}
			}

		};
		meters2 = new Meters() {
			public Type[] getTypes() {
				return SUPPORTED_MEASURES;
			}

			@Override
			public float getValue(Type t) {
				//				if(t == Meters.Type.W) {
				//					return power1;
				//				} else if(t == Meters.Type.I) {
				//					return current1;
				//				} else if(t == Meters.Type.PF) {
				//					return pf1;
				//				} else {
				return voltage2;
				//				}
			}
		};
		meters3 = new Meters() {
			public Type[] getTypes() {
				return SUPPORTED_MEASURES;
			}

			@Override
			public float getValue(Type t) {
				//					if(t == Meters.Type.W) {
				//						return power1;
				//					} else if(t == Meters.Type.I) {
				//						return current1;
				//					} else if(t == Meters.Type.PF) {
				//						return pf1;
				//					} else {
				return voltage3;
				//					}
			}
		};

		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	private JsonNode configure() throws IOException {
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
			addOn = new SensorAddOn(this);
		} else {
			addOn = null;
		}
		return config;
	}

	@Override
	public String getTypeName() {
		return "Shelly +RGBW";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

		@Override
		public int getModulesCount() {
			if(profile == Mode.LIGHT) {
				return 4;
			} else {
				return 1;
			}
		}

	@Override
	public DeviceModule getModule(int index) {
		if(profile != Mode.LIGHT) {
			return lights[index];
		} else {
			return null; //todo
		}
	}

	@Override
	public DeviceModule[] getModules() {
		if(profile == Mode.LIGHT) {
			return lights;
		} else {
			return null; //todo
		}
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	public float getPower() {
		return power0;
	}

	public float getVoltage() {
		return voltage0;
	}

	public float getCurrent() {
		return current0;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		String prof = configuration.get("sys").get("device").get("profile").asText();
		if(prof.equals(Mode.LIGHT.code)) {
			if(profile != Mode.LIGHT) {
				profile = Mode.LIGHT;
				light0 = new LightWhite(this, 0);
				light1 = new LightWhite(this, 1);
				light2 = new LightWhite(this, 2);
				light3 = new LightWhite(this, 3);
				lights = new LightWhite[] {light0, light1, light2, light3};
				meters = new Meters[] {meters0, meters1, meters2, meters3};
				meters = (addOn == null || addOn.getTypes().length == 0) ? new Meters[] {meters0, meters1, meters2, meters3} : new Meters[] {meters0, meters1, meters2, meters3, addOn};
			}
			light0.fillSettings(configuration.get("light:0"));
			light1.fillSettings(configuration.get("light:1"));
			light2.fillSettings(configuration.get("light:2"));
			light3.fillSettings(configuration.get("light:3"));
		} else if(prof.equals(Mode.RGB.code)) {
			if(profile != Mode.RGB) {
				profile = Mode.RGB;
				
				light0 =  light1 =  light2 = light3 = null;
				
				meters = new Meters[] {meters0};
				meters = (addOn == null || addOn.getTypes().length == 0) ? new Meters[] {meters0} : new Meters[] {meters0, addOn};
			}
			// fill
		} else /*if(prof.equals(Mode.RGBW.code))*/ {
			if(profile != Mode.RGBW) {
				profile = Mode.RGBW;
				
				light0 =  light1 =  light2 = light3 = null;
				
				meters = new Meters[] {meters0};
				meters = (addOn == null || addOn.getTypes().length == 0) ? new Meters[] {meters0} : new Meters[] {meters0, addOn};
			}
			// fill
		} 
		if(addOn != null) {
			addOn.fillSettings(configuration);
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(profile == Mode.LIGHT) {
			JsonNode lightStatus0 = status.get("light:0");
			voltage0 = lightStatus0.get("voltage").floatValue();
			light0.fillStatus(lightStatus0, status.get("input:0"));
			
			JsonNode lightStatus1 = status.get("light:1");
			voltage1 = lightStatus1.get("voltage").floatValue();
			light1.fillStatus(lightStatus1, status.get("input:1"));
			
			JsonNode lightStatus2 = status.get("light:2");
			voltage2 = lightStatus2.get("voltage").floatValue();
			light2.fillStatus(lightStatus2, status.get("input:2"));
			
			JsonNode lightStatus3 = status.get("light:3");
			voltage3 = lightStatus3.get("voltage").floatValue();
			light3.fillStatus(lightStatus3, status.get("input:3"));
			
			internalTmp = lightStatus0.get("temperature").path("tC").floatValue();
		} else if(profile == Mode.RGB) {
			JsonNode rgb = status.get("rgb:0");
			voltage0 = rgb.get("voltage").floatValue();
			internalTmp = rgb.get("temperature").path("tC").floatValue();
		} else /*if(profile == Mode.RGBW)*/ {
			JsonNode rgbw = status.get("rgbw:0");
			voltage0 = rgbw.get("voltage").floatValue();
			internalTmp = rgbw.get("temperature").path("tC").floatValue();
		}
		//		if(modeRelay) {
		//			JsonNode switchStatus0 = status.get("switch:0");
		//			relay0.fillStatus(switchStatus0, status.get("input:0"));
		//			power0 = switchStatus0.path("apower").floatValue();
		//			voltage0 = switchStatus0.path("voltage").floatValue();
		//			current0 = switchStatus0.path("current").floatValue();
		//			pf0 = switchStatus0.path("pf").floatValue();
		//
		//			JsonNode switchStatus1 = status.get("switch:1");
		//			relay1.fillStatus(switchStatus1, status.get("input:1"));
		//			power1 = switchStatus1.path("apower").floatValue();
		//			voltage1 = switchStatus1.path("voltage").floatValue();
		//			current1 = switchStatus1.path("current").floatValue();
		//			pf1 = switchStatus1.path("pf").floatValue();
		//
		//			internalTmp = (float)switchStatus0.path("temperature").path("tC").floatValue();
		//		} else {
		//			JsonNode cover = status.get("cover:0");
		//			power0 = cover.path("apower").floatValue();
		//			voltage0 = cover.path("voltage").floatValue();
		//			current0 = cover.path("current").floatValue();
		//			pf0 = cover.path("pf").floatValue();
		//			internalTmp = cover.path("temperature").path("tC").floatValue();
		//			roller.fillStatus(cover);
		//		}
		if(addOn != null) {
			addOn.fillStatus(status);
		}
	}

	@Override
	public String[] getInfoRequests() {
		final String[] cmd = super.getInfoRequests();
		return (addOn != null) ? SensorAddOn.getInfoRequests(cmd) : cmd;
	}

//	public void setProfile(Mode mode) {
//		postCommand("Shelly.SetProfile", "{\"name\":\"" + (cover ? "cover" : "switch")  +"\"}");
//	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
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
		SensorAddOn.restoreCheck(this, backupJsons, res);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		//		final boolean backModeRelay = MODE_RELAY.equals(configuration.at("/sys/device/profile").asText());
		//		if(backModeRelay == modeRelay) {
		//			errors.add(Input.restore(this,configuration, 0));
		//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		//			errors.add(Input.restore(this,configuration, 1));
		//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		//			if(backModeRelay) {
		//				errors.add(relay0.restore(configuration));
		//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		//				errors.add(relay1.restore(configuration));
		//			} else {
		//				errors.add(roller.restore(configuration));
		//			}
		//		} else {
		//			errors.add(RestoreMsg.ERR_RESTORE_MODE_COVER.name());
		//		}

		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		SensorAddOn.restore(this, backupJsons, errors);
	}

	@Override
	public SensorAddOn getSensorAddOn() {
		return addOn;
	}

	//	@Override
	//	public String toString() {
	//		if(modeRelay) {
	//			return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
	//		} else {
	//			return super.toString() + " Cover: " + roller;
	//		}
	//	}
	
	@Override
	public String toString() {
		if(profile == Mode.LIGHT) {
			return super.toString() + ": " + light0 + " / " + light1 + " / " + light2 + " / " +  light3;
		} else {
			return super.toString();
		}
	}
}