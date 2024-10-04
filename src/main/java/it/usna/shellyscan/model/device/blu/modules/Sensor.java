package it.usna.shellyscan.model.device.blu.modules;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class Sensor {
	private float value;
	
	public void fillStatus(JsonNode status) {
		//todo
	}
	
	public float getValue() {
		return value;
	}
	
	public Meters.Type getMeterType() {
		return null;
	}
}
