package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.FloodInterface;
import tools.jackson.databind.JsonNode;

public class ShellyFlood extends AbstractBatteryG1Device implements ModulesHolder {
	public static final String ID = "SHWT-1";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT, Meters.Type.T};
	private boolean flood;
	private float temp;
	private Meters[] meters;
	private final FloodInterface[] sensor;
	
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
		
		sensor = new FloodInterface[] {
				new FloodInterface() {
					@Override
					public boolean flood() {
						return flood && status == Status.ON_LINE;
					}

					@Override
					public String toString() {
						return "flood: " + flood; 
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
		this.stSettings = settings;
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.stStatus = status;
		flood = status.get("flood").asBoolean();
		temp = (float)status.get("tmp").get("tC").doubleValue();
		bat = status.get("bat").get("value").asInt();
	}
	
//	public boolean flood() {
//		return flood;
//	}
	
	public float getTemp() {
		return temp;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public DeviceModule[] getModules() {
		return sensor;
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException {
		JsonNode sensors = settings.get("sensors");
		errors.add(sendCommand("/settings?" +
				jsonNodeToURLPar(sensors, "temperature_units", "temperature_threshold")) + "&" +
				jsonNodeToURLPar(settings, "rain_sensor", "temperature_offset"));
	}
}