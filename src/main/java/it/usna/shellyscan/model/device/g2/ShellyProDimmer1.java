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
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Pro Dimmer 1PM model
 * @author usna
 */
public class ShellyProDimmer1 extends AbstractProDevice implements InternalTmpHolder, ModulesHolder {
	public static final String ID = "ProDimmerx";
	public static final String MODEL = "SPDM-001PE01EU";
	private float internalTmp;
	private MetersWVI meters = new MetersWVI();
	private Meters[] metersArray = new Meters[] {meters};
	private LightWhite light = new LightWhite(this, 0);
	private LightWhite[] lightArray = new LightWhite[] {light};

	public ShellyProDimmer1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
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
		return metersArray;
	}

	@Override
	public DeviceModule[] getModules() {
		return lightArray;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		light.fillSettings(configuration.get("light:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode lightStatus = status.get("light:0");
		internalTmp = lightStatus.get("temperature").get("tC").floatValue();
		meters.fill(lightStatus);
		light.fillStatus(lightStatus, status.get("input:0"));
	}
	
	@Override
	protected void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> resp) {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		if(MODEL.equals(devInfo.get("model").textValue()) == false) {
			resp.put(RestoreMsg.ERR_RESTORE_MODEL, null);
		}
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