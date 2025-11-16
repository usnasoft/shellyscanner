package it.usna.shellyscan.model.device.g2.modules;

import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import tools.jackson.databind.JsonNode;

//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonServices/Shelly#shellyupdate
public class FirmwareManagerG2 implements FirmwareManager {
	private final AbstractG2Device d;
	private String current;
	private String stable;
	private String beta;
	private boolean updating;
	private boolean valid;
	
	public static final String STAGE_STABLE = "stable";
	public static final String STAGE_BETA = "beta";
	
	public FirmwareManagerG2(AbstractG2Device d) /*throws IOException*/ {
		this.d = d;
		init();
	}

	private void init() {
		try {
			JsonNode node = d.getJSON("/rpc/Shelly.CheckForUpdate");
			stable = node.at("/stable/build_id").asString(null);
			beta = node.at("/beta/build_id").asString(null);
			if(d instanceof BatteryDeviceInterface == false) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			JsonNode nodeDevInfo = d.getJSON("/rpc/Shelly.GetDeviceInfo");
			current = nodeDevInfo.get("fw_id").asString(null);
			valid = true;
			updating = false;
		} catch(/*IO*/Exception e) {
			valid = updating = false;
			current = stable = beta = null;
			JsonNode node;
			if(d instanceof BatteryDeviceInterface batteryDevice) {
				if((node = batteryDevice.getStoredJSON("/rpc/Shelly.CheckForUpdate")) != null) {
					stable = node.at("/stable/build_id").asString(null);
					beta = node.at("/beta/build_id").asString(null);
				} else if((node = batteryDevice.getStoredJSON("/rpc/Shelly.GetStatus")) != null) {
					node = node.at("/sys/available_updates");
					stable = node.at("/stable/version").asString(null); // not id
					beta = node.at("/beta/version").asString(null); // not id
				}
				if((node = batteryDevice.getStoredJSON("/rpc/Shelly.GetConfig")) != null) { // probably fresher than "/rpc/Shelly.GetDeviceInfo"
					current = node.at("/sys/device/fw_id").asString(null);
				} else if((node = batteryDevice.getStoredJSON("/shelly")) != null) {
					current = node.path("fw_id").asString(null);
				}
			}
		}
	}

	@Override
	public void chech() {
		init();
	}
	
	@Override
	public String current() {
		return current;
	}
	
	@Override
	public String newBeta() {
		return beta;
	}
	
	@Override
	public String newStable() {
		return stable;
	}
	
	@Override
	public String update(boolean stable) {
		updating = true;
		String res = d.postCommand("Shelly.Update", stable ? "{\"stage\":\"" + STAGE_STABLE + "\"}" : "{\"stage\":\"" + STAGE_BETA + "\"}");
		if(res != null && res.isEmpty() == false) {
			updating = false;
		}
		return res;
	}

	@Override
	public boolean upadating() {
		return updating;
	}

	public void upadating(boolean upd) {
		updating = upd;
	}

	@Override
	public boolean isValid() {
		return valid;
	}
}
