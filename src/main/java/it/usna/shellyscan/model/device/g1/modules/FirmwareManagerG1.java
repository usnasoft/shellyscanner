package it.usna.shellyscan.model.device.g1.modules;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.FirmwareManager;

public class FirmwareManagerG1 implements FirmwareManager {
	private static final String STATUS_UPDATING = "updating";

	private final AbstractG1Device d;
	private String current;
	private String stable;
	private String beta;
	private boolean updating;
	private boolean valid;
	
	public FirmwareManagerG1(AbstractG1Device d) /*throws IOException*/ {
		this.d = d;
		init();
	}

	private void init() {
		valid = false;
		try {
			d.sendCommand("/ota/check");
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			JsonNode otaNode = d.getJSON("/ota");
			updating = STATUS_UPDATING.equals(otaNode.get("status").asText());
			current = otaNode.get("old_version").asText();
			stable = otaNode.get("has_update").booleanValue() ? otaNode.get("new_version").asText() : null;
			final JsonNode betaNode = otaNode.get("beta_version");
			boolean hasBeta = betaNode != null && betaNode.asText().equals(current) == false;
			beta = hasBeta ? betaNode.asText() : null;
			valid = true;
		} catch(/*IO*/Exception e) {
			valid = updating = false;
			current = stable = beta = null;
			if(d instanceof BatteryDeviceInterface batteryDevice) {
				JsonNode node = batteryDevice.getStoredJSON("/status");
				if(node != null) {
					node = node.get("update");
					current = node.get("old_version").asText();
					stable = node.get("has_update").booleanValue() ? node.get("new_version").asText() : null;
					final JsonNode betaNode = node.get("beta_version");
					boolean hasBeta = betaNode != null && betaNode.asText().equals(current) == false;
					beta = hasBeta ? betaNode.asText() : null;
				} else if((node = batteryDevice.getStoredJSON("/shelly")) != null) {
					current = node.path("fw").asText();
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
		String res = d.sendCommand(stable ? "/ota?update=true" : "/ota?beta=true");
		if(res != null && res.isEmpty() == false) {
			updating = false;
		}
		return res;
	}
	
	@Override
	public boolean upadating() {
		return updating;
	}
	
	@Override
	public void upadating(boolean upd) {
		updating = upd;
	}
	
	@Override
	public boolean isValid() {
		return valid;
	}
}