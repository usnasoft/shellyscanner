package it.usna.shellyscan.model.device.g4;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.FloodInterface;
import tools.jackson.databind.JsonNode;

/**
 * Shelly Flood S G4 model (same as Shelly Flood G4)
 */
public class ShellyFloodSG4 extends AbstractBatteryG4Device implements ModulesHolder {
	public static final String ID = "FloodSensorG4";
	public static final String MODEL = "S4SN-0071Z";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT};
	private boolean flood;
	private String floodName;
	private Meters[] meters;
	private final FloodInterface[] sensor;

	public ShellyFloodSG4(InetAddress address, int port, String hostname) {
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
		
		sensor = new FloodInterface[] {
				new FloodInterface() {
					@Override
					public boolean flood() {
						return flood && status == Status.ON_LINE;
					}
					
					@Override
					public String getLabel() {
						return floodName;
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
		return "Shelly Flood S G4";
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
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		this.settingsJ = settings;
		floodName = settings.path("flood:0").path("name").asString("");
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.statusJ = status;
		flood = status.path("flood:0").path("alarm").asBoolean(false);
		bat = status.path("devicepower:0").path("battery").path("percent").asInt(0);
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
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(postCommand("Flood.SetConfig", RestoreUtil.createIndexedRestoreNode(configuration, "flood", 0)));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Flood: " + flood;
	}
}