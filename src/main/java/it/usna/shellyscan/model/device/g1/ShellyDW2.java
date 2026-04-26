package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DWInterface;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import tools.jackson.databind.JsonNode;

public class ShellyDW2 extends AbstractBatteryG1Device implements ModulesHolder, DWInterface {
	public static final String ID = "SHDW-2";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT, Meters.Type.T, Meters.Type.L};
	private boolean open;
	private float temp;
	private int lux;
	private Meters[] meters;
	private DWInterface[] dwModule;
	
	public ShellyDW2(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		dwModule = new DWInterface[] {this};
		
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
	public boolean open() {
		return open;
	}

	@Override
	public DeviceModule[] getModules() {
		return dwModule;
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
		open = status.get("sensor").get("state").asString("").equals("open");
		bat = status.get("bat").get("value").asInt();
		lux = status.get("lux").get("value").asInt();
		temp = (float)status.get("tmp").get("tC").doubleValue();
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" +
				jsonNodeToURLPar(settings, "dark_threshold", "twilight_threshold",
				"led_status_disable", "lux_wakeup_enable", "tilt_enabled", "vibration_enabled", "vibration_sensitivity", "reverse_open_close", "temperature_offset") + "&" +
				jsonNodeToURLPar(settings.get("sensor"), "temperature_threshold", "temperature_units")));
	}
}