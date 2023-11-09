package it.usna.shellyscan.model.device.g2;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.FirmwareManager;

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
			JsonNode node = d.getJSON("/rpc/Shelly.GetDeviceInfo");
			current = node.get("fw_id").asText();
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			node = d.getJSON("/rpc/Shelly.CheckForUpdate");
			stable = node.at("/stable/build_id").asText(null);
			beta = node.at("/beta/build_id").asText(null);
			valid = true;
			updating = false;
		} catch(/*IO*/Exception e) {
			valid = updating = false;
			current = stable = beta = null;
			JsonNode node;
			if(d instanceof BatteryDeviceInterface batteryDevice) {
				if((node = batteryDevice.getStoredJSON("/rpc/Shelly.CheckForUpdate")) != null) {
					stable = node.at("/stable/build_id").asText(null);
					beta = node.at("/beta/build_id").asText(null);
				} else if((node = batteryDevice.getStoredJSON("/rpc/Shelly.GetStatus")) != null) {
					node = node.at("/sys/available_updates");
					stable = node.at("/stable/version").asText(null); // not id
					beta = node.at("/beta/version").asText(null); // not id
				}
				if((node = batteryDevice.getStoredJSON("/shelly")) != null) {
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
		return d.postCommand("Shelly.Update", stable ? "{\"stage\":\"stable\"}" : "{\"stage\":\"beta\"}");
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