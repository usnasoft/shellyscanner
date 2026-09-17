package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.g3.modules.Camera;
import it.usna.shellyscan.model.device.modules.CameraInterface;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import tools.jackson.databind.JsonNode;

/**
 * Shelly Camera
 * @author usna
 */
public class ShellyCamera extends AbstractG3Device implements ModulesHolder {
	public static final String ID = "Camera";
	public static final String MODEL = "S1CM-0DXW00";
	private final Camera cam = new Camera(this);
	private final CameraInterface[] motion = new CameraInterface[] {cam};

	public ShellyCamera(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Camera";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getModelID() {
		return MODEL;
	}
	
	@Override
	public DeviceModule[] getModules() {
		return motion;
	}

//	@Override
//	protected void fillSettings(JsonNode configuration) throws IOException {
//		super.fillSettings(configuration);
//	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		cam.fillStatus(status.get("camera:0"));
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode backupConfiguration = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Storage.SetConfig", RestoreUtil.createIndexedRestoreNode(backupConfiguration, "storage", 0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		cam.restore(backupConfiguration, errors);
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		JsonNode backupVirtualComp = backupJsons.get("Shelly.GetComponents.json");
		cam.restoreZones(backupVirtualComp, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Motion: " + cam.motion();
	}
}