package it.usna.shellyscan.model.device.blu.modules;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class Sensor {
	private final int id;
	private final int objID;
	private Meters.Type mType = null;
	private String name;
	private float value;
	
	Sensor(String component, int objID) {
		this.id = Integer.parseInt(component.substring(13));
		this.objID = objID;
		this.mType = switch(objID) {
		case 0x01 -> Meters.Type.BAT;
		case 0x2E -> Meters.Type.H;
		case 0x45 -> Meters.Type.T;
		default -> null;
		};
	}
	
	public int getId() {
		return id;
	}
	
	public void fillSConfig(JsonNode config) {
		name = config.path("name").asText("");
	}
	
	public void fillStatus(JsonNode status) {
		value = status.path("value").floatValue();
	}
	
	public String getName() {
		return name;
	}
	
	public float getValue() {
		return value;
	}
	
	public Meters.Type getMeterType() {
		return mType;
	}
	
	@Override
	public String toString() {
		return name + " - " + objID;
	}
}
