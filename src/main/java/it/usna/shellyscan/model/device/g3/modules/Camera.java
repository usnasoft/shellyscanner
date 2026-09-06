package it.usna.shellyscan.model.device.g3.modules;

import java.io.IOException;
import java.util.List;

import it.usna.shellyscan.model.device.g3.ShellyCamera;
import it.usna.shellyscan.model.device.modules.CameraInterface;
import tools.jackson.databind.JsonNode;

public class Camera implements CameraInterface {
	private boolean motionDetected;
	
	public void fillStatus(JsonNode cameraStatus) throws IOException {
		motionDetected = cameraStatus.get("motion").booleanValue(false);
	}
	
	@Override
	public boolean motion() {
		return motionDetected;
	}
	
	@Override
	public String toString() {
		return "motion: " + motionDetected; 
	}

	@Override
	public String getWebStream(int index) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void restoreZones(ShellyCamera d, JsonNode backupVirtualComp, List<String> errors) {
		// TODO verifica e integrazione (camerazone ...)
	}
}
