package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.MotionSensor;

public class ShellyMotion extends AbstractG1Device implements MotionSensor {
	public final static String ID = "SHMOS-01";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {/*Meters.Type.T,*/ Meters.Type.L, Meters.Type.BAT};
	private int lux;
//	private float temp;
	private Meters[] meters;
	private boolean motion;
	protected int bat;

	public ShellyMotion(InetAddress address, int port, String hostname) {
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
//						} else if(t == Meters.Type.T) {
//							return temp;
						} else {
							return lux;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Motion 1";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public boolean motion() {
		return motion;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
//	@Override
//	protected void fillSettings(JsonNode settings) throws IOException {
//		super.fillSettings(settings);
//	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		motion = status.get("sensor").get("motion").asBoolean();
		bat = status.get("bat").get("value").intValue();
		lux = status.get("lux").get("value").intValue();
//		temp = status.path("tmp").path("value").floatValue();
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException {
		JsonNode motion = settings.path("motion");
		String mSensitivity = motion.get("sensitivity").asText();
		String mBlind = motion.get("blind_time_minutes").asText();
		String mPulseCount = motion.get("pulse_count").asText();
		String mOperatingMode = motion.get("operating_mode").asText();
		String mEnabled = motion.get("enabled").asText();
//		JsonNode sensors = settings.path("sensors");
//		String tempUnit = sensors.path("temperature_unit").asText();
//		String temptTreshohld = sensors.path("temperature_threshohld").asText();
		// sleep_time is a temporary parameter
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "tamper_sensitivity", "dark_threshold", "twilight_threshold"/*, "temperature_offset"*/) +
				"&motion.sensitivity=" + mSensitivity +
				"&motion.blind_time_minutes=" + mBlind +
				"&motion.pulse_count=" + mPulseCount +
				"&motion.operating_mode=" + mOperatingMode +
				"&motion.enabled=" + mEnabled));
//				"&sensors.temperature_unit=" + tempUnit +
//				"&sensors.temperature_threshohld=" + temptTreshohld));
	}
}