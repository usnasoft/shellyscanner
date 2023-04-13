package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyDW2 extends AbstractBatteryG1Device {
	public final static String ID = "SHDW-2";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT, Meters.Type.T, Meters.Type.L};
	private boolean open;
	private float temp;
	private int lux;
	private Meters[] meters;
	
	public ShellyDW2(InetAddress address, int port, String hostname) {
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
						} else if(t == Meters.Type.T) {
							return temp;
						} else {
							return lux;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly DW2";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public float getTemp() {
		return temp;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
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
		open = status.get("sensor").get("state").asText("").equals("open");
		bat = status.get("bat").get("value").asInt();
		lux = status.get("lux").get("value").asInt();
		temp = (float)status.get("tmp").get("tC").doubleValue();
	}
	
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" +
				jsonNodeToURLPar(settings, "dark_threshold", "twilight_threshold",
				"led_status_disable", "lux_wakeup_enable", "tilt_enabled", "vibration_enabled", "vibration_sensitivity", "reverse_open_close", "temperature_offset") + "&" +
				jsonNodeToURLPar(settings.get("sensor"), "temperature_threshold", "temperature_units")));
	}
}