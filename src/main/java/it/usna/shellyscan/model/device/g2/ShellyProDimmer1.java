package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.meters.MetersWVI;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOnPro;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import tools.jackson.databind.JsonNode;

/**
 * Pro Dimmer 1PM model
 * @author usna
 */
public class ShellyProDimmer1 extends AbstractProDevice implements InternalTmpHolder, ModulesHolder {
	public static final String ID = "ProDimmerx";
	public static final String ID_ADDON = "ProDimmerxProAddon";
	public static final String MODEL = "SPDM-001PE01EU";
	private float internalTmp;
//	private MetersWVI meters = new MetersWVI();
//	private Meters[] metersArray = new Meters[] {meters};
	private MetersWVI baseMeasures = new MetersWVI();
	private Meters[] meters;
	private LightWhite light = new LightWhite(this, 0);
	private LightWhite[] lightArray = new LightWhite[] {light};
	private SensorAddOnPro sensorAddOn;

	public ShellyProDimmer1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asString("");
		this.mac = devInfo.get("mac").asString("");
		
		final JsonNode config = configure();
			
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	private JsonNode configure() throws IOException {
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		final String addOnType = config.get("sys").get("device").path("addon_type").asString("");
		if(SensorAddOnPro.ADDON_TYPE.equals(addOnType)) {
			sensorAddOn = new SensorAddOnPro(this);
			Meters[] m = sensorAddOn.getMetersArray();
			ArrayList<Meters> metersList = new ArrayList<Meters>(3);
			metersList.add(baseMeasures);
			for(Meters met: m) {
				if(met.getTypes().length > 0) {
					metersList.add(met);
				}
			}
			meters = metersList.toArray(Meters[]::new);
		} else {
			sensorAddOn = null;
			meters = new Meters[] {baseMeasures};
		}
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Pro Dimmer 1PM";
	}
	
	@Override
	public String getTypeID() {
		return ID;
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
	public DeviceModule[] getModules() {
		return lightArray;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		light.fillSettings(configuration.get("light:0"));
		if(sensorAddOn != null) {
			sensorAddOn.fillSettings(configuration);
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode lightStatus = status.get("light:0");
		internalTmp = lightStatus.get("temperature").get("tC").floatValue();
		light.fillStatus(lightStatus, status.get("input:0"));
		baseMeasures.fill(lightStatus);
		if(sensorAddOn != null) {
			sensorAddOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		final String[] cmd = super.getInfoRequests();
		return (sensorAddOn != null) ? SensorAddOnPro.getInfoRequests(cmd) : cmd;
	}
	
	@Override
	protected void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> resp) {
		// todo addon
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		if(MODEL.equals(devInfo.get("model").asString("")) == false) {
			resp.put(RestoreMsg.ERR_RESTORE_MODEL, null);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		// todo addon
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