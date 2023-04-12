package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyFlood extends AbstractBatteryG1Device {
	public final static String ID = "SHWT-1";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT, Meters.Type.T};
	private boolean flood;
	private float temp;
	private Meters[] meters;

	public ShellyFlood(InetAddress address, int port, JsonNode shelly, String hostname) {
		this(address, port, hostname);
		this.shelly = shelly;
	}
	
	public ShellyFlood(InetAddress address, int port, String hostname) {
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
						} else {
							return temp;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Flood";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		this.settings = settings;
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.status = status;
		flood = status.get("flood").asBoolean();
		temp = (float)status.get("tmp").get("tC").doubleValue();
		bat = status.get("bat").get("value").asInt();
	}
	
	public boolean flood() {
		return flood;
	}
	
	public float getTemp() {
		return temp;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		JsonNode sensors = settings.get("sensors");
		errors.add(sendCommand("/settings?" +
				jsonNodeToURLPar(sensors, "temperature_units", "temperature_threshold")) + "&" +
				jsonNodeToURLPar(settings, "rain_sensor", "temperature_offset"));
	}
}