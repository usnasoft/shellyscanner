package it.usna.shellyscan.model.device.g4;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.DeviceAPIException;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.g4.modules.PresenceZoneG4;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.PresenceZoneInterface;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Shelly Presence model
 * @author usna
 */
public class ShellyPresenceG4 extends AbstractG4Device implements ModulesHolder {
	private static final Logger LOG = LoggerFactory.getLogger(ShellyPresenceG4.class);
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.LD};
	public static final String ID = "PresenceG4";
	public static final String MODEL = "S4SN-0U61X";
	private float illumination;
	private Meters[] meters;
	private PresenceZoneG4 mainZone = new PresenceZoneG4(this);
	private final PresenceZoneInterface[] sensors = {mainZone};
	
	public ShellyPresenceG4(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						return illumination;
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Presence G4";
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
		return sensors;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		mainZone.setId(configuration.get("presence").get("main_zone").asString("presencezone:200").substring(13));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		
		String illDesc = status.get("illuminance:0").get("illumination").asString(null);
		if("dark".equals(illDesc)) {
			illumination = 0;
		} else if("twilight".equals(illDesc)) {
			illumination = 1;
		} else if("bright".equals(illDesc)) {
			illumination = 2;
		} else {
			illumination = -1;
		}
		
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		mainZone.fillStatus();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode backupConfiguration = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Illuminance.SetConfig", RestoreUtil.createIndexedRestoreNode(backupConfiguration, "illuminance", 0)));

		ObjectNode out = JsonNodeFactory.instance.objectNode();
		ObjectNode presenceCopy = ((ObjectNode)backupConfiguration.get("presence")).deepCopy();
		String backMainZoneKey = presenceCopy.remove("main_zone").asString();

		// "sensor" data to be restored depends by "sensitivity"
		ObjectNode sensorCopy = (ObjectNode)presenceCopy.get("sensor");
		String sensitivity = sensorCopy.get("sensitivity").asString();
		if(sensitivity.equals("custom") == false) {
			sensorCopy.without("points").without("velocity").without("snr").without("max_velocity").without("state");
		} else {
			sensorCopy.without("sensitivity");
		}

		out.set("config", presenceCopy);
		errors.add(postCommand("Presence.SetConfig", out));

		// --- Zones
		JsonNode backupComponents = backupJsons.get("Shelly.GetComponents.json");
		errors.add(mainZone.restore(backupComponents, backMainZoneKey));

		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		Map<String, JsonNode> currentZones = new HashMap<String, JsonNode>(); // without main_zone

		try {
			// Collection of the current zones excluding main zone
			Iterator<JsonNode> compIt = getJSONIterator("/rpc/Shelly.GetComponents?dynamic_only=true&include=[%22config%22]", "components");
			while (compIt.hasNext()) {
				JsonNode comp = compIt.next();
				final String key = comp.path("key").asString("");
				if(key.startsWith("presencezone:") && key.equals("presencezone:" + mainZone.getId()) == false) {
					currentZones.put(key, comp);
				}
			}
			// If a stored zone (not main) have the same id of a current zone -> restore
			ArrayNode compArray = (ArrayNode)backupComponents.get("components"); // file -> no fragments
			for(JsonNode comp: compArray) {
				final String key = comp.path("key").asString("");
				if(key.startsWith("presencezone:") && key.equals(backMainZoneKey) == false) {
					if(currentZones.containsKey(key)) {
						errors.add(new PresenceZoneG4(this, key.substring(13)).restore(backupComponents, key));
						currentZones.remove(key);
					} else {
						try {
							PresenceZoneG4.addZone(this, (ObjectNode)comp.deepCopy().path("config"));
						} catch(DeviceAPIException e) {
							errors.add(e.getMessage());
						} catch(Exception e) {
							errors.add("Error adding stored " + key);
						}
					}
				}
			}
			for(String key: currentZones.keySet()) {
				errors.add(postCommand("Presence.DeleteZone", "{\"id\":" + key.substring(13) + "}"));
			}
		} catch (IOException e) {
			LOG.error("ShellyPresenceG4 restore", e);
			errors.add(e.getMessage());
		}
	}

	@Override
	public String toString() {
		return super.toString() + " Objects: " + mainZone.numObjects();
	}
}