package it.usna.shellyscan.model.device.g4.modules;

import java.io.IOException;

import it.usna.shellyscan.model.device.g4.AbstractG4Device;
import it.usna.shellyscan.model.device.modules.PresenceZoneInterface;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class PresenceZoneG4 implements PresenceZoneInterface {
	private final AbstractG4Device parent;
	private String id;
	private int presenceObjects;
	
	public PresenceZoneG4(AbstractG4Device parent) {
		this.parent = parent;
	}
	
	public PresenceZoneG4(AbstractG4Device parent, String id) {
		this.parent = parent;
		this.id = id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void fillStatus() throws IOException {
		JsonNode mainZoneStatus = parent.getJSON("/rpc/PresenceZone.GetStatus?id=" + id);
		presenceObjects = mainZoneStatus.get("num_objects").asInt();
	}

	@Override
	public int numObjects() {
		return presenceObjects;
	}
	
	/**
	 * Restore data from backupComponents with backKey key on the current (this.id) PresenceZone
	 * @param backupComponents
	 * @param backKey
	 * @return error description if any
	 */
	public String restore(JsonNode backupComponents, String backKey) {
		ArrayNode compArray = (ArrayNode)backupComponents.get("components"); // file -> no fragments
		for(JsonNode comp: compArray) {
			if(comp.path("key").asString("").equals(backKey)) {
				configZone(comp);
				break;
			}
		}
		return null;
	}
	
	private String configZone(JsonNode backZone) {
		ObjectNode data = (ObjectNode)backZone.get("config").deepCopy();
		/*int backId =*/ data.remove("id")/*.asInt()*/;
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("id", Integer.parseInt(this.id)).set("config", data);
		return parent.postCommand("PresenceZone.SetConfig", out);
	}

	@Override
	public String toString() {
		return "Objects: " + presenceObjects;
	}
}