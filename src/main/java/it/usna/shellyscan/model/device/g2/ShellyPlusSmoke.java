package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyPlusSmoke extends AbstractBatteryG2Device {
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT};
	private Meters[] meters;
	private boolean alarm;
	
//	public ShellyPlusSmoke(InetAddress address, int port, JsonNode shelly, String hostname) {
//		this(address, port, hostname);
//		this.shelly = shelly;
//	}
	
	public ShellyPlusSmoke(InetAddress address, int port, String hostname) {
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

	public static final String ID = "PlusSmoke";
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Smoke";
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	public boolean getAlarm() {
		return alarm;
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
		bat = status.path("devicepower:0").path("battery").path("percent").intValue();
		alarm = status.path("smoke:0").path("alarm").asBoolean();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws JsonProcessingException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(postCommand("Smoke.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("smoke:0")) + "}"));
	}
}