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
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightRGB;
import it.usna.shellyscan.model.device.g2.modules.LightRGBW;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.meters.MetersWVI;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Shelly PRO RGBWW PM model 
 * @author usna
 */
// TODO a lot (CCT)
public class ShellyProRGBWW extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
//	private final static Logger LOG = LoggerFactory.getLogger(ShellyProRGBWW.class);
	public enum Profile {
		LIGHT("light"), RGB("rgb"), RGBW("rgbw");
		
		private final String code;
		
		private Profile(String code) {
			this.code = code;
		}
	};
	public final static String ID = "ProRGBWWPM";
	private Profile profile;
	private float internalTmp;
	private float power0, power1, power2, power3, power4; // if calibrated (white)
	private float voltage0, voltage1, voltage2, voltage3, voltage4;
	private float current0, current1, current2, current3, current4; // if calibrated (white)
	private LightWhite light0, light1, light2, light3, light4;
	private LightWhite[] lights;
	private LightRGB rgbLight;
	private LightRGB[] rgbs;
	private LightRGBW rgbwLight;
	private LightRGBW[] rgbws;
	
	private Meters meters0, meters1, meters2, meters3, meters4;
	private Meters[] meters;

	public ShellyProRGBWW(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();

		meters0 = new MetersWVI() {
			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power0;
				} else if(t == Meters.Type.I) {
					return current0;
				} else {
					return voltage0;
				}
			}
		};
		meters1 = new MetersWVI() {
			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power1;
				} else if(t == Meters.Type.I) {
					return current1;
				} else {
					return voltage1;
				}
			}

		};
		meters2 = new MetersWVI() {
			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power2;
				} else if(t == Meters.Type.I) {
					return current2;
				} else {
					return voltage2;
				}
			}
		};
		meters3 = new MetersWVI() {
			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power3;
				} else if(t == Meters.Type.I) {
					return current3;
				} else {
					return voltage3;
				}
			}
		};
		meters4 = new MetersWVI() {
			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power4;
				} else if(t == Meters.Type.I) {
					return current4;
				} else {
					return voltage4;
				}
			}
		};
		fillSettings(getJSON("/rpc/Shelly.GetConfig"));
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro RGBWW PM";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public int getModulesCount() {
		if(profile == Profile.LIGHT) {
			return 5;
		} else {
			return 1;
		}
	}

	@Override
	public DeviceModule getModule(int index) {
		if(profile != Profile.LIGHT) {
			return lights[index];
		} else if(profile == Profile.RGBW) {
			return rgbwLight;
		} else {
			return rgbLight;
		}
	}

	@Override
	public DeviceModule[] getModules() {
		if(profile == Profile.LIGHT) {
			return lights;
		} else if(profile == Profile.RGBW) {
			return rgbws;
		} else {
			return rgbs;
		}
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		String prof = configuration.get("sys").get("device").get("profile").asText();
		if(prof.equals(Profile.LIGHT.code)) {
			if(profile != Profile.LIGHT) {
				profile = Profile.LIGHT;
				light0 = new LightWhite(this, 0);
				light1 = new LightWhite(this, 1);
				light2 = new LightWhite(this, 2);
				light3 = new LightWhite(this, 3);
				light4 = new LightWhite(this, 4);
				lights = new LightWhite[] {light0, light1, light2, light3, light4};
				rgbLight = null;
				meters = new Meters[] {meters0, meters1, meters2, meters3, meters4};
			}
			light0.fillSettings(configuration.get("light:0"));
			light1.fillSettings(configuration.get("light:1"));
			light2.fillSettings(configuration.get("light:2"));
			light3.fillSettings(configuration.get("light:3"));
		} else if(prof.equals(Profile.RGB.code)) {
			if(profile != Profile.RGB) {
				profile = Profile.RGB;
				rgbLight = new LightRGB(this, 0);
				rgbs = new LightRGB[] {rgbLight};
				light0 =  light1 =  light2 = light3 = null;
				rgbwLight = null;
				meters = new Meters[] {meters0};
			}
			rgbLight.fillConfig(configuration.get("rgb:0"));
		} else if(prof.equals(Profile.RGBW.code)) {
			if(profile != Profile.RGBW) {
				profile = Profile.RGBW;
				rgbwLight = new LightRGBW(this, 0);
				rgbws = new LightRGBW[] {rgbwLight};
				light0 =  light1 =  light2 = light3 = null;
				rgbLight = null;
				meters = new Meters[] {meters0};
			}
			rgbwLight.fillConfig(configuration.get("rgbw:0"));
		} else {
			// todo
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(profile == Profile.LIGHT) {
			JsonNode lightStatus0 = status.get("light:0");
			voltage0 = lightStatus0.get("voltage").floatValue();
			current0 = lightStatus0.path("current").floatValue();
			power0 = lightStatus0.path("apower").floatValue();
			light0.fillStatus(lightStatus0, status.get("input:0"));
			
			JsonNode lightStatus1 = status.get("light:1");
			voltage1 = lightStatus1.get("voltage").floatValue();
			current1 = lightStatus1.path("current").floatValue();
			power1 = lightStatus1.path("apower").floatValue();
			light1.fillStatus(lightStatus1, status.get("input:1"));
			
			JsonNode lightStatus2 = status.get("light:2");
			voltage2 = lightStatus2.get("voltage").floatValue();
			current2 = lightStatus2.path("current").floatValue();
			power2 = lightStatus2.path("apower").floatValue();
			light2.fillStatus(lightStatus2, status.get("input:2"));
			
			JsonNode lightStatus3 = status.get("light:3");
			voltage3 = lightStatus3.get("voltage").floatValue();
			current3 = lightStatus3.path("current").floatValue();
			power3 = lightStatus3.path("apower").floatValue();
			light3.fillStatus(lightStatus3, status.get("input:3"));
			
			JsonNode lightStatus4 = status.get("light:4");
			voltage4 = lightStatus4.get("voltage").floatValue();
			current4 = lightStatus4.path("current").floatValue();
			power4 = lightStatus4.path("apower").floatValue();
			light4.fillStatus(lightStatus4, status.get("input:4"));
			
			internalTmp = lightStatus0.get("temperature").path("tC").floatValue();
		} else if(profile == Profile.RGB) {
			JsonNode rgb = status.get("rgb:0");
			voltage0 = rgb.get("voltage").floatValue();
			current0 = rgb.path("current").floatValue();
			power0 = rgb.path("apower").floatValue();
			rgbLight.fillStatus(rgb, status.get("input:0"));
			
			internalTmp = rgb.get("temperature").path("tC").floatValue();
		} else if(profile == Profile.RGBW) {
			JsonNode rgbw = status.get("rgbw:0");
			voltage0 = rgbw.get("voltage").floatValue();
			current0 = rgbw.path("current").floatValue();
			power0 = rgbw.path("apower").floatValue();
			rgbwLight.fillStatus(rgbw, status.get("input:0"));
			
			internalTmp = rgbw.get("temperature").path("tC").floatValue();
		}
	}

	public void setProfile(Profile mode) {
		postCommand("Shelly.SetProfile", "{\"name\":\"" + mode.code  +"\"}");
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		if(profile.code.equals(devInfo.get("profile").asText()) == false) {
			res.put(RestoreMsg.ERR_RESTORE_MODE_COLOR, profile.code);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 2));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 3));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 4));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		
		final String backMode = configuration.at("/sys/device/profile").asText();
		if(profile.code.equals(backMode)) {
			if(profile == Profile.LIGHT) {
				errors.add(light0.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(light1.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(light2.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(light3.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(light4.restore(configuration));
			} else if(profile == Profile.RGBW) {
				errors.add(rgbwLight.restore(configuration));
			} else /*if(profile == Mode.RGB)*/ {
				errors.add(rgbLight.restore(configuration));
			}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		} else {
			errors.add(RestoreMsg.ERR_RESTORE_MODE_COLOR.name());
		}
		
		final boolean hf = configuration.get("plusrgbwpm").get("hf_mode").booleanValue();
		errors.add(postCommand("PlusRGBWPM.SetConfig", "{\"config\":{\"hf_mode\":" + hf + "}}"));
	}
	
	@Override
	public String toString() {
		if(profile == Profile.LIGHT) {
			return super.toString() + ": " + light0 + " / " + light1 + " / " + light2 + " / " +  light3;
		} else if(profile == Profile.RGBW) {
			return super.toString() + ": " + rgbwLight;
		} else {
			return super.toString() + ": " + rgbLight;
		}
	}
}