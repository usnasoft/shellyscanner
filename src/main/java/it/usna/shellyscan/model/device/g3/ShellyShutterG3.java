package it.usna.shellyscan.model.device.g3;

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
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Roller;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g3.modules.LoRaAddOn;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Shelly Shutter G3 model 
 * @author usna
 */
public class ShellyShutterG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	private final static Logger LOG = LoggerFactory.getLogger(ShellyShutterG3.class);
	public final static String ID = "S2PMG3Shutter";
	public final static String MODEL = "S3SH-0A2P4EU";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Roller roller = new Roller(this, 0);
	private Roller[] rollersArray = new Roller[] {roller};
	private float internalTmp;
	private float power0;
	private float voltage0;
	private float current0;
	private float pf0;
	private Meters meters0;
	private Meters[] meters;
	private SensorAddOn sensorAddOn;
	private boolean loraAddOn;

	public ShellyShutterG3(InetAddress address, int port, String hostname) {
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
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		final String addOn = config.get("sys").get("device").path("addon_type").asText();
		if(SensorAddOn.ADDON_TYPE.equals(addOn)) {
			sensorAddOn = new SensorAddOn(this);
			meters = (sensorAddOn.getTypes().length > 0) ? new Meters[] {meters0, sensorAddOn} : new Meters[] {meters0};
		} else {
			sensorAddOn = null;
			meters = new Meters[] {meters0};
		}
		loraAddOn = LoRaAddOn.ADDON_TYPE.equals(addOn);
		return config;
	}

	@Override
	public String getTypeName() {
		return "Shelly Shutter G3";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public DeviceModule[] getModules() {
		return rollersArray;
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
		roller.fillSettings(configuration.get("cover:0"));

		if(sensorAddOn != null) {
			sensorAddOn.fillSettings(configuration);
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode cover = status.get("cover:0");
		power0 = cover.path("apower").floatValue();
		voltage0 = cover.path("voltage").floatValue();
		current0 = cover.path("current").floatValue();
		pf0 = cover.path("pf").floatValue();
		internalTmp = cover.path("temperature").path("tC").floatValue();
		roller.fillStatus(cover);

		if(sensorAddOn != null) {
			sensorAddOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		final String[] cmd = super.getInfoRequests();
		if(sensorAddOn != null) {
			return SensorAddOn.getInfoRequests(cmd);
		} else if(loraAddOn) {
			return LoRaAddOn.getInfoRequests(cmd);
		} else {
			return cmd;
		}
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		try {
			configure(); // maybe useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		} catch (IOException e) {
			LOG.error("restoreCheck", e);
		}
		SensorAddOn.restoreCheck(this, sensorAddOn, backupJsons, res);
		LoRaAddOn.restoreCheck(this, loraAddOn, backupJsons, res);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this,configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(roller.restore(configuration));

		SensorAddOn.restore(this, sensorAddOn, backupJsons, errors);
		LoRaAddOn.restore(this, loraAddOn, configuration, errors);
	}

	@Override
	public String toString() {
		return super.toString() + " Cover: " + roller;
	}
}