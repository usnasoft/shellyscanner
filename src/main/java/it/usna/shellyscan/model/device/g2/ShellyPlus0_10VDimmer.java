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
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.modules.WhiteCommander;

/**
 * Shelly plus dimmer 0-10 model
 * @author usna
 */
public class ShellyPlus0_10VDimmer extends AbstractG2Device implements /*InternalTmpHolder,*/ WhiteCommander, SensorAddOnHolder {
	private final static Logger LOG = LoggerFactory.getLogger(ShellyPlus0_10VDimmer.class);
	public final static String ID = "Plus10V";
//	private float internalTmp;
//	private float power;
//	private float voltage;
//	private float current;
	private Meters[] meters;
	private LightWhite light = new LightWhite(this, 0);
	private LightWhite[] lightArray = new LightWhite[] {light};
	private SensorAddOn addOn;

	public ShellyPlus0_10VDimmer(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		
		final JsonNode config = configure();
		
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	private JsonNode configure() throws IOException {
//		Meters baseMeasures = new MetersWVI() {
//			@Override
//			public float getValue(Type t) {
//				if(t == Meters.Type.W) {
//					return power;
//				} else if(t == Meters.Type.I) {
//					return current;
//				} else {
//					return voltage;
//				}
//			}
//		};
		
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
			addOn = new SensorAddOn(this);
			meters = new Meters[] {addOn}; //(addOn.getTypes().length > 0) ? new Meters[] {baseMeasures, addOn} : new Meters[] {baseMeasures};
		} else {
			addOn = null;
			meters = null; //new Meters[] {baseMeasures};
		}
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly +Dimmer 0-10V";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
//	@Override
//	public float getInternalTmp() {
//		return internalTmp;
//	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public LightWhite getWhite(int index) {
		return light;
	}

	@Override
	public LightWhite[] getWhites() {
		return lightArray;
	}

	@Override
	public int getWhitesCount() {
		return 1;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		light.fillSettings(configuration.get("light:0"));
		if(addOn != null) {
			addOn.fillSettings(configuration);
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode lightStatus = status.get("light:0");
//		internalTmp = lightStatus.get("temperature").get("tC").floatValue();
//		power = lightStatus.get("apower").floatValue();
//		voltage = lightStatus.get("voltage").floatValue();
//		current = lightStatus.get("current").floatValue();
		light.fillStatus(lightStatus, status.get("input:0"));
		if(addOn != null) {
			addOn.fillStatus(status);
		}
	}
	
	@Override
	public SensorAddOn getSensorAddOn() {
		return addOn;
	}

	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		try {
			configure(); // maybe useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		} catch (IOException e) {
			LOG.error("restoreCheck", e);
		}
		SensorAddOn.restoreCheck(this, backupJsons, res);
	}
	
	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(light.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Light: " + light;
	}
}