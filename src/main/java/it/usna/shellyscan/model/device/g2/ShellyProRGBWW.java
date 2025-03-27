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
import it.usna.shellyscan.model.device.g2.meters.MetersWVI;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightCCT;
import it.usna.shellyscan.model.device.g2.modules.LightRGB;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Shelly PRO RGBWW PM model 
 * @author usna
 */
public class ShellyProRGBWW extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
//	private final static Logger LOG = LoggerFactory.getLogger(ShellyProRGBWW.class);
	public final static String ID = "ProRGBWWPM";
	public final static String MODEL ="SPDC-0D5PE16EU";
	public enum Profile {
		LIGHT("light"), RGB2L("rgbx2light"), RGB_CCT("rgbcct"), CCT_CCT("cctx2");
		
		private final String code;
		
		private Profile(String code) {
			this.code = code;
		}
	};
	private Profile profile;
	private float internalTmp;
	private LightWhite light0, light1, light2, light3, light4;
	private DeviceModule[] commands;
	private LightRGB rgbLight;
	private LightCCT cct0, cct1;

	private MetersWVI meters0 = new MetersWVI(); // power0 if calibrated (white); current0 if calibrated (white)
	private MetersWVI meters1 = new MetersWVI();
	private MetersWVI meters2 = new MetersWVI();
	private MetersWVI meters3 = new MetersWVI();
	private MetersWVI meters4 = new MetersWVI();
	private Meters[] meters;

	public ShellyProRGBWW(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();

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
		return commands.length;
	}

	@Override
	public DeviceModule[] getModules() {
		return commands;
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
				commands = new LightWhite[] {light0, light1, light2, light3, light4};
				rgbLight = null;
				cct0 = cct1 = null;
				meters = new Meters[] {meters0, meters1, meters2, meters3, meters4};
			}
			light0.fillSettings(configuration.get("light:0"));
			light1.fillSettings(configuration.get("light:1"));
			light2.fillSettings(configuration.get("light:2"));
			light3.fillSettings(configuration.get("light:3"));
			light4.fillSettings(configuration.get("light:4"));
		} else if(prof.equals(Profile.RGB_CCT.code)) {
			if(profile != Profile.RGB_CCT) {
				profile = Profile.RGB_CCT;
				rgbLight = new LightRGB(this, 0);
				cct0 = new LightCCT(this, 0);
				commands = new DeviceModule[] {rgbLight, cct0};
				light0 = light1 = light2 = light3 = light4 = null;
				cct1 = null;
				meters = new Meters[] {meters0, meters2};
			}
			rgbLight.fillConfig(configuration.get("rgb:0"));
			cct0.fillSettings(configuration.get("cct:0"));
		} else if(prof.equals(Profile.CCT_CCT.code)) {
			if(profile != Profile.CCT_CCT) {
				profile = Profile.CCT_CCT;
				cct0 = new LightCCT(this, 0);
				cct1 = new LightCCT(this, 1);
				commands = new DeviceModule[] {cct0, cct1};
				light0 = light1 = light2 = light3 = light4 = null;
				rgbLight = null;
				meters = new Meters[] {meters0, meters2}; // meter indexes as input indexes
			}
			cct0.fillSettings(configuration.get("cct:0"));
			cct1.fillSettings(configuration.get("cct:1"));
		} else if(prof.equals(Profile.RGB2L.code)) {
			if(profile != Profile.RGB2L) {
				profile = Profile.RGB2L;
				rgbLight = new LightRGB(this, 0);
				light0 = new LightWhite(this, 0);
				light1 = new LightWhite(this, 1);
				commands = new DeviceModule[] {rgbLight, light0, light1};
				light2 = light3 = light4 = null;
				cct0 = cct1 = null;
				meters = new Meters[] {meters0, meters2, meters3}; // meter indexes as input indexes
			}
			rgbLight.fillConfig(configuration.get("rgb:0"));
			light0.fillSettings(configuration.get("light:0"));
			light1.fillSettings(configuration.get("light:1"));
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(profile == Profile.LIGHT) {
			JsonNode lightStatus0 = status.get("light:0");
			meters0.fill(lightStatus0);
			light0.fillStatus(lightStatus0, status.get("input:0"));
			
			JsonNode lightStatus1 = status.get("light:1");
			meters1.fill(lightStatus1);
			light1.fillStatus(lightStatus1, status.get("input:1"));
			
			JsonNode lightStatus2 = status.get("light:2");
			meters2.fill(lightStatus2);
			light2.fillStatus(lightStatus2, status.get("input:2"));
			
			JsonNode lightStatus3 = status.get("light:3");
			meters3.fill(lightStatus3);
			light3.fillStatus(lightStatus3, status.get("input:3"));
			
			JsonNode lightStatus4 = status.get("light:4");
			meters4.fill(lightStatus4);
			light4.fillStatus(lightStatus4, status.get("input:4"));
			
			internalTmp = lightStatus0.get("temperature").path("tC").floatValue();
		} else if(profile == Profile.RGB_CCT) {
			JsonNode rgb = status.get("rgb:0");
			meters0.fill(rgb);
			rgbLight.fillStatus(rgb, status.get("input:0"));
			
			JsonNode lightStatus0 = status.get("cct:0");
			meters2.fill(lightStatus0);
			cct0.fillStatus(lightStatus0, status.get("input:2")); // cct:0 - input:2
			
			internalTmp = rgb.get("temperature").path("tC").floatValue();
		} else if(profile == Profile.CCT_CCT) {
			JsonNode cctStatus0 = status.get("cct:0");
			meters0.fill(cctStatus0);
			cct0.fillStatus(cctStatus0, status.get("input:0")); // cct:0 - input:0
			
			JsonNode cctStatus1 = status.get("cct:1");
			meters2.fill(cctStatus1);
			cct1.fillStatus(cctStatus1, status.get("input:2")); // cct:1 - input:2
			
			internalTmp = cctStatus0.get("temperature").path("tC").floatValue();
		} else if(profile == Profile.RGB2L) {
			JsonNode rgb = status.get("rgb:0");
			meters0.fill(rgb);
			rgbLight.fillStatus(rgb, status.get("input:0"));
			
			JsonNode lightStatus0 = status.get("light:0");
			meters2.fill(lightStatus0);
			light0.fillStatus(lightStatus0, status.get("input:2")); // light:0 - input:2
			
			JsonNode lightStatus1 = status.get("light:1");
			meters3.fill(lightStatus1);
			light1.fillStatus(lightStatus1, status.get("input:3")); // light:1 - input:3
			
			internalTmp = rgb.get("temperature").path("tC").floatValue();
		}
	}

	public void setProfile(Profile mode) {
		postCommand("Shelly.SetProfile", "{\"name\":\"" + mode.code  +"\"}");
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		if(profile.code.equals(devInfo.get("profile").asText()) == false) {
			res.put(RestoreMsg.ERR_RESTORE_PROFILE, new String[] {profile.code, devInfo.get("profile").asText()});
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
			} else if(profile == Profile.RGB_CCT) {
				errors.add(rgbLight.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(cct0.restore(configuration));
			} else if(profile == Profile.CCT_CCT) {
				errors.add(cct0.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(cct1.restore(configuration));
			} else /*if(profile == Profile.RGB2L)*/ {
				errors.add(rgbLight.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(light0.restore(configuration));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				errors.add(light1.restore(configuration));
			}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		} else {
			errors.add(RestoreMsg.ERR_RESTORE_PROFILE.name());
		}
		
		// TODO ?
//		final boolean hf = configuration.get("plusrgbwpm").get("hf_mode").booleanValue();
//		errors.add(postCommand("PlusRGBWPM.SetConfig", "{\"config\":{\"hf_mode\":" + hf + "}}"));
	}
	
	@Override
	public String toString() {
		if(profile == Profile.LIGHT) {
			return super.toString() + ": " + light0 + " / " + light1 + " / " + light2 + " / " +  light3 + " / " +  light4;
		} else if(profile == Profile.RGB_CCT) {
			return super.toString() + ": " + rgbLight + ": " + cct0;
		} else if(profile == Profile.CCT_CCT) {
			return super.toString() + ": " + cct0 + " / " + cct1;
		} else { // RGB2L
			return super.toString() + ": " + rgbLight + " / " + light0 + " / " + light1;
		}
	}
}