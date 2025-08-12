package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyDW extends AbstractBatteryG1Device {
	public static final String ID = "SHDW-1";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT};
	private boolean open;
	private Meters[] meters;

	public ShellyDW(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						return bat;
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly DW";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
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
		open = status.get("sensor").get("state").asText("").equals("open");
		bat = status.get("bat").get("value").asInt();
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "dark_threshold", "twilight_threshold",
				"led_status_disable", "lux_wakeup_enable", "tilt_enabled", "vibration_enabled", "vibration_sensitivity", "reverse_open_close")));
	}
}