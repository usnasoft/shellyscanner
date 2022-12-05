package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyTRV extends AbstractG1Device {
	public final static String ID = "SHTRV-01";
	private final static Logger LOG = LoggerFactory.getLogger(ShellyTRV.class);
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT, Meters.Type.T};
	private float measuredTemp;
	private float targetTemp;
	private float position;
	private Meters[] meters;
	protected int bat;

	public ShellyTRV(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		try {
			JsonNode settings = getJSON("/settings");
			fillOnce(settings);
			fillSettings(settings);
			fillStatus(getJSON("/status"));
		} catch(Exception e) {
			status = Status.ERROR;
			LOG.error(getTypeName(), e);
		}
		
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
							return measuredTemp;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "TRV";
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
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		JsonNode target = settings.get("thermostats").get(0).get("target_t");
		targetTemp = target.get("enabled").asBoolean() ? (float)target.get("value").doubleValue() : 0f;
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		bat = status.get("bat").get("value").asInt();
		JsonNode therm = status.get("thermostats").get(0);
		measuredTemp = (float)therm.get("tmp").get("value").doubleValue();
		position = (float)therm.get("position").doubleValue();
		
	}
	
	public float getMeasuredTemp() {
		return measuredTemp;
	}
	
	public float geTtargetTemp() {
		return targetTemp;
	}
	
	public float getPosition() {
		return position;
	}

	// TODO
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
//		JsonNode motion = settings.path("motion");
//		String mSensitivity = motion.get("sensitivity").asText();
//		String mBlind = motion.get("blind_time_minutes").asText();
//		String mPulseCount = motion.get("pulse_count").asText();
//		String mOperatingMode = motion.get("operating_mode").asText();
//		String mEnabled = motion.get("enabled").asText();
//		// sleep_time is a temporary parameter
//		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "tamper_sensitivity", "dark_threshold", "twilight_threshold", "temperature_offset") +
//				"&motion.sensitivity=" + mSensitivity +
//				"&motion.blind_time_minutes=" + mBlind +
//				"&motion.pulse_count=" + mPulseCount +
//				"&motion.operating_mode=" + mOperatingMode +
//				"&motion.enabled=" + mEnabled));
	}
}