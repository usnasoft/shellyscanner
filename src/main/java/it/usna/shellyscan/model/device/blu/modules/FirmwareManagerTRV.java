package it.usna.shellyscan.model.device.blu.modules;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.DeviceOfflineException;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.blu.BluTRV;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import tools.jackson.databind.JsonNode;

public class FirmwareManagerTRV implements FirmwareManager {
	private static final Logger LOG = LoggerFactory.getLogger(FirmwareManagerTRV.class);

	private final BluTRV d;
	private String currentBuild;
	private String current;
	private String stableBuild;
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
			JsonNode deviceInfoNode = d.getJSON("/rpc/BluTrv.GetRemoteDeviceInfo?id=" + d.getComponentIndex());
			currentBuild = deviceInfoNode.at("/device_info/fw_id").asString("");
			current = deviceInfoNode.at("/device_info/ver").asString(null);
			if(current == null) {
				current = FirmwareManager.getShortVersion(currentBuild);
			}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			
			JsonNode newFwNode = d.getJSON("/rpc/BluTrv.CheckForUpdates?id=" + d.getComponentIndex());
			String lastFWBuild = newFwNode.path("fw_id").asString("");
			
			if(lastFWBuild != null && lastFWBuild.isEmpty() == false && lastFWBuild.equals(currentBuild) == false) {
				this.stableBuild = lastFWBuild;
			} else {
				this.stableBuild = null;
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
	public String currentBuild() {
		return currentBuild;
	}
	
	@Override
	public String current() {
		return current;
	}
	
	@Override
	public String newBetaBuild() {
		return null;
	}
	
	@Override
	public String newBeta() {
		return null;
	}
	
	@Override
	public String newStableBuild() {
		return stableBuild;
	}
	
	@Override
	public String newStable() {
		return FirmwareManager.getShortVersion(stableBuild);
	}
	
	@Override
	public String update(boolean dummy) {
		updating = true;
//		new Thread(() -> {
//			String res = d.postCommand("BluTrv.UpdateFirmware", "{\"id\":" + d.getIndex() + "}");
//			if(res != null && res.isEmpty() == false) {
//				updating = false;
//				LOG.error("FirmwareManagerTRV.update {}", res);
//			}
//		}).start();
		new Thread(() -> {
			try {
				d.getJSON("/rpc/BluTrv.UpdateFirmware?id=" + d.getComponentIndex()); // this call is blocking -> DeviceOfflineException
			} catch (DeviceOfflineException e) {
				LOG.trace("FirmwareManagerTRV.update timeout");
				updating = false;
			} catch (IOException | RuntimeException e) {
				LOG.error("FirmwareManagerTRV.update", e);
				updating = false;
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