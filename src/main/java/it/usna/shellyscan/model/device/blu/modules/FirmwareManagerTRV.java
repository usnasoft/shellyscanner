package it.usna.shellyscan.model.device.blu.modules;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.blu.BluTRV;
import it.usna.shellyscan.model.device.modules.FirmwareManager;

//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonServices/Shelly#shellyupdate
public class FirmwareManagerTRV implements FirmwareManager {

	private final BluTRV d;
	private String current;
	private String stable;
//	private String beta;
	private boolean updating;
	private boolean valid;
	
	public FirmwareManagerTRV(BluTRV d) {
		this.d = d;
		init();
	}

	private void init() {
		try {
			JsonNode deviceInfoNode = d.getJSON("/rpc/BluTrv.GetRemoteDeviceInfo?id=\"" + d.getIndex());
			current = deviceInfoNode.path("fw_id").textValue();
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			
			JsonNode newFwNode = d.getJSON("/rpc/BluTrv.CheckForUpdates?id=" + d.getIndex());
			String lastFW = newFwNode.path("fw_id").textValue();
			
			if(lastFW != null && lastFW.equals(current) == false) {
				this.stable = lastFW;
			}
			valid = true;
			updating = false;
		} catch(/*IO*/Exception e) {
			valid = updating = false;
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
		return null;
	}
	
	@Override
	public String newStable() {
		return stable;
	}
	
	@Override
	public String update(boolean dummy) {
		updating = true;
//		String res = d.postCommand("BluTrv.UpdateFirmware", "{\"id\":" + d.getIndex() + "}");
//		if(res == null || res.isEmpty()) {
//			updating = false;
//		}
//		System.out.println("res " + res + " - " + d.getStatus());
		return "res";
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