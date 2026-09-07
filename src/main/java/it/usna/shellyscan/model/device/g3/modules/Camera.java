package it.usna.shellyscan.model.device.g3.modules;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g3.ShellyCamera;
import it.usna.shellyscan.model.device.modules.CameraInterface;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class Camera implements CameraInterface {
	private static final Logger LOG = LoggerFactory.getLogger(Camera.class);
	private static final int CAMERA_ID = 0; 
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
		// http://<device>/camera/0/whep/0
		return null;
	}
	
	public static String deleteZone(ShellyCamera d, int id) {
		return d.postCommand("Camera.DeleteZone", "{\"id\":0, \"zone_id\":" + id + "}");
	}
	
	public static String addZone(ShellyCamera d, JsonNode conf) {
		return d.postCommand("Camera.AddZone", conf);
	}
	
	public static String configureZone(ShellyCamera d, int zoneId, JsonNode conf) {
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", zoneId).set("config", conf);
		return d.postCommand("CameraZone.SetConfig", out);
	}
	
	public static void restoreZones(ShellyCamera d, JsonNode backupVirtualComp, List<String> errors) throws InterruptedException {
		try {
			// Store existing zone keys
			HashSet<String> currentZones = new HashSet<>();
			Iterator<JsonNode> compIt = d.getJSONIterator("/rpc/Shelly.GetComponents?dynamic_only=true&include=[%22config%22]", "components");
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
						errors.add(configureZone(d, backId, data));
						currentZones.remove(key);
					} else { // Create new zones
						ObjectNode data = ((ObjectNode)comp.get("config").deepCopy());
						data.put("id", CAMERA_ID);
						errors.add(addZone(d, data));
					}
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
				}
			}
			
			// Delete zones not into the backup
			for(String key: currentZones) {
				errors.add(deleteZone(d, Integer.parseInt(key.substring(11))));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}
		} catch (IOException e) {
			LOG.error("ShellyPresenceG4 restore", e);
			errors.add(e.getMessage());
		}
	}
}