package it.usna.shellyscan.model.device.g2.modules;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.FirmwareManager;

//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonServices/Shelly#shellyupdate
public class FirmwareManagerG2 implements FirmwareManager {

	private final AbstractG2Device d;
	private String current;
	private String stable;
	private String beta;
	private boolean updating;
	private boolean valid;
	
	public FirmwareManagerG2(AbstractG2Device d) /*throws IOException*/ {
		this.d = d;
		init();
	}

	private void init() {
		try {
			JsonNode node = d.getJSON("/rpc/Shelly.CheckForUpdate");
			stable = node.at("/stable/build_id").textValue();
			beta = node.at("/beta/build_id").textValue();
			if(d instanceof BatteryDeviceInterface == false) {
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
			JsonNode nodeDevInfo = d.getJSON("/rpc/Shelly.GetDeviceInfo");
			current = nodeDevInfo.get("fw_id").asText();
			valid = true;
			updating = false;
		} catch(/*IO*/Exception e) {
			valid = updating = false;
			current = stable = beta = null;
			JsonNode node;
			if(d instanceof BatteryDeviceInterface batteryDevice) {
				if((node = batteryDevice.getStoredJSON("/rpc/Shelly.CheckForUpdate")) != null) {
					stable = node.at("/stable/build_id").textValue();
					beta = node.at("/beta/build_id").textValue();
				} else if((node = batteryDevice.getStoredJSON("/rpc/Shelly.GetStatus")) != null) {
					node = node.at("/sys/available_updates");
					stable = node.at("/stable/version").textValue(); // not id
					beta = node.at("/beta/version").textValue(); // not id
				}
				if((node = batteryDevice.getStoredJSON("/rpc/Shelly.GetConfig")) != null) { // this could fresher than "/rpc/Shelly.GetDeviceInfo"
					current = node.at("/sys/device/fw_id").asText();
				} else if((node = batteryDevice.getStoredJSON("/shelly")) != null) {
					current = node.path("fw_id").asText();
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
		String res = d.postCommand("Shelly.Update", stable ? "{\"stage\":\"stable\"}" : "{\"stage\":\"beta\"}");
		if(res != null && res.length() > 0) {
			updating = false;
		}
//		System.out.println("res " + res + " - " + d.getStatus());
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