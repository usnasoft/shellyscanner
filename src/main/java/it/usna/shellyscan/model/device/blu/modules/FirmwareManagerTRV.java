package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.blu.BluTRV;
import it.usna.shellyscan.model.device.modules.FirmwareManager;

public class FirmwareManagerTRV implements FirmwareManager {
	private final static Logger LOG = LoggerFactory.getLogger(FirmwareManagerTRV.class);

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
		updating = false;
		try {
			JsonNode deviceInfoNode = d.getJSON("/rpc/BluTrv.GetRemoteDeviceInfo?id=" + d.getIndex());
			current = deviceInfoNode.at("/device_info/fw_id").textValue();
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			
			JsonNode newFwNode = d.getJSON("/rpc/BluTrv.CheckForUpdates?id=" + d.getIndex());
			String lastFW = newFwNode.path("fw_id").textValue();
			
			if(lastFW != null && lastFW.isEmpty() == false && lastFW.equals(current) == false) {
				this.stable = lastFW;
			} else {
				this.stable = null;
			}
			valid = true;
		} catch(/*IO*/Exception e) {
			valid = false;
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
//		return res;
		new Thread(() -> {
			try {
				d.getJSON("/rpc/BluTrv.UpdateFirmware?id=" + d.getIndex()); // this call is blocking -> DeviceOfflineException
			} catch (DeviceOfflineException e) {
				LOG.trace("FirmwareManagerTRV.update timeout");
			} catch (IOException | RuntimeException e) {
				LOG.error("FirmwareManagerTRV.update", e);
			}
		}).start();
		return null;
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