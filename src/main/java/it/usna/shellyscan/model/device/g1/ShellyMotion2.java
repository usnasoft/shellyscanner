package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.MotionInterface;
import tools.jackson.databind.JsonNode;

public class ShellyMotion2 extends AbstractG1Device implements ModulesHolder {
	public static final String ID = "SHMOS-02";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.L, Meters.Type.BAT};
	private int lux;
	private float temp;
	private boolean motion;
	protected int bat;
	private final Meters[] meters;
	private final MotionInterface[] sensor;

	public ShellyMotion2(InetAddress address, int port, String hostname) {
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
		
		sensor = new MotionInterface[] {
				new MotionInterface() {
					@Override
					public boolean motion() {
						return motion;
					}
					
					@Override
					public String toString() {
						return "motion: " + motion; 
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Motion";
	}
	
	@Override
	public String getTypeID() {
		return ID;
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
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		motion = status.get("sensor").get("motion").booleanValue(false);
		bat = status.get("bat").get("value").intValue(0);
		lux = status.get("lux").get("value").intValue(0);
		temp = status.get("tmp").get("value").floatValue();
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException {
		JsonNode motion = settings.path("motion");
		String mSensitivity = motion.get("sensitivity").asString("");
		String mBlind = motion.get("blind_time_minutes").asString("");
		String mPulseCount = motion.get("pulse_count").asString("");
		String mOperatingMode = motion.get("operating_mode").asString("");
		String mEnabled = motion.get("enabled").asString("");
		JsonNode sensors = settings.get("sensors");
		String tempUnit = sensors.get("temperature_unit").asString("");
		String temptTreshohld = sensors.path("temperature_threshohld").asString("");
		// sleep_time is a temporary parameter
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "tamper_sensitivity", "dark_threshold", "twilight_threshold", "temperature_offset") +
				"&motion.sensitivity=" + mSensitivity +
				"&motion.blind_time_minutes=" + mBlind +
				"&motion.pulse_count=" + mPulseCount +
				"&motion.operating_mode=" + mOperatingMode +
				"&motion.enabled=" + mEnabled +
				"&sensors.temperature_unit=" + tempUnit +
				"&sensors.temperature_threshohld=" + temptTreshohld));
	}
}