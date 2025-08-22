package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyHT extends AbstractBatteryG1Device {
	public static final String ID = "SHHT-1";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.BAT};
	private float temp;
	private int humidity;
	private Meters[] meters;
	
	public ShellyHT(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.BAT) {
							return bat;
						} else if(t == Meters.Type.H) {
							return humidity;
						} else {
							return temp;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly H&T";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		this.stSettings = settings;
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.stStatus = status;
		temp = status.get("tmp").get("tC").floatValue();
		humidity = status.get("hum").get("value").intValue();
		bat = status.get("bat").get("value").intValue();
	}

//	public float getTemp() {
//		return temp;
//	}
//	
//	public float getHumidity() {
//		return humidity;
//	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException {
		JsonNode sensors = settings.get("sensors");
		errors.add(sendCommand("/settings?" +
				jsonNodeToURLPar(settings, "external_power", "temperature_offset", "humidity_offset") + "&" +
				jsonNodeToURLPar(sensors, "temperature_threshold", "humidity_threshold") + "&" +
				"temperature_units=" + sensors.get("temperature_unit").asText())); // temperature_units vs temperature_unit !!!
	}
}