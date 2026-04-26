package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.SmokeInterface;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

public class ShellyPlusSmoke extends AbstractBatteryG2Device implements ModulesHolder, SmokeInterface {
	public static final String ID = "PlusSmoke";
	public static final String MODEL = "SNSN-0031Z";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT};
	private Meters[] meters;
	private boolean alarm;
	private SmokeInterface[] smokeModule;

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
		
		smokeModule = new SmokeInterface[] {this};
	}

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
	
	@Override
	public boolean smoke() {
		return alarm;
	}

	@Override
	public SmokeInterface[] getModules() {
		return smokeModule;
	}

	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		this.settingsJ = settings;
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.statusJ = status;
		bat = status.path("devicepower:0").path("battery").path("percent").intValue(0);
		alarm = status.path("smoke:0").path("alarm").asBoolean();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws JacksonException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(postCommand("Smoke.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("smoke:0")) + "}"));
	}
}