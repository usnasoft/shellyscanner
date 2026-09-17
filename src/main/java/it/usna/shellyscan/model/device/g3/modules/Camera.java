package it.usna.shellyscan.model.device.g3.modules;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.CameraInterface;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class Camera implements CameraInterface {
	private static final Logger LOG = LoggerFactory.getLogger(Camera.class);
	private static final int CAMERA_ID = 0; 
	private final AbstractG2Device parent;
	private boolean motionDetected;
	private boolean privacy;
	
	public Camera(AbstractG2Device parent) {
		this.parent = parent;
	}
	
	public void fillStatus(JsonNode cameraStatus) throws IOException {
		motionDetected = cameraStatus.get("motion").booleanValue(false);
		privacy = cameraStatus.get("privacy").booleanValue(false);
	}
	
	@Override
	public boolean motion() {
		return motionDetected;
	}
	
	@Override
	public boolean getPrivacy() {
		return privacy;
	}

	@Override
	public String setPrivacy(boolean enable) {
		return parent.postCommand("Camera.Set", "{\"id\":" + CAMERA_ID + ",\"privacy\":" + enable + "}");
	}

//	@Override
//	public String getWebStream(int index) {
//		// TODO Auto-generated method stub
//		// http://<device>/camera/0/whep/0
//		return null;
//	}
	
	@Override
	public String toString() {
		return "motion: " + motionDetected; 
	}

	public String deleteZone(int id) {
		return parent.postCommand("Camera.DeleteZone", "{\"id\":0, \"zone_id\":" + id + "}");
	}
	
	public String addZone(JsonNode conf) {
		return parent.postCommand("Camera.AddZone", conf);
	}
	
	public String configureZone(int zoneId, JsonNode conf) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", zoneId).set("config", conf);
		return parent.postCommand("CameraZone.SetConfig", out);
	}
	
	public void restore(JsonNode backupConfiguration, List<String> errors) throws InterruptedException {
		errors.add(parent.postCommand("Camera.SetConfig", RestoreUtil.createIndexedRestoreNode(backupConfiguration, "camera", CAMERA_ID)));
	}
	
	public void restoreZones(JsonNode backupVirtualComp, List<String> errors) throws InterruptedException {
		try {
			// Store existing zone keys
			HashSet<String> currentZones = new HashSet<>();
			Iterator<JsonNode> compIt = parent.getJSONIterator("/rpc/Shelly.GetComponents?dynamic_only=true&include=[%22config%22]", "components");
			while (compIt.hasNext()) {
				JsonNode comp = compIt.next();
				final String key = comp.path("key").asString("");
				if(key.startsWith("camerazone:")) {
					currentZones.add(key);
				}
			}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

			JsonNode compArray = backupVirtualComp.get("components");
			for(JsonNode comp: compArray) {
				final String key = comp.path("key").asString("");
				if(key.startsWith("camerazone:")) {
					if(currentZones.contains(key)) { // Configure existing zones
						ObjectNode data = (ObjectNode)comp.get("config").deepCopy();
						int backId = data.remove("id").asInt(); // backId = currentId
						errors.add(configureZone(backId, data));
						currentZones.remove(key);
					} else { // Create new zones
						ObjectNode data = ((ObjectNode)comp.get("config").deepCopy());
						data.put("id", CAMERA_ID);
						errors.add(addZone(data));
					}
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			}
			
			// Delete zones not into the backup
			for(String key: currentZones) {
				errors.add(deleteZone(Integer.parseInt(key.substring(11))));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
		} catch (IOException e) {
			LOG.error("ShellyPresenceG4 restore", e);
			errors.add(e.getMessage());
		}
	}
}