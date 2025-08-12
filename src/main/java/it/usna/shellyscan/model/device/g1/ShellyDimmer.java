package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g1.meters.MetersPower;
import it.usna.shellyscan.model.device.g1.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.DeviceModule;

public class ShellyDimmer extends AbstractG1Device implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "SHDM-1";
	private float internalTmp;
	private boolean calibrated;
	private LightWhite light = new LightWhite(this, "/light/", 0);
	private LightWhite[] lightArray = new LightWhite[] {light};
	private float power;
	private Meters[] meters;

	public ShellyDimmer(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new MetersPower() {
					@Override
					public float getValue(Type t) {
						return power;
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Dimmer";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public DeviceModule[] getModules() {
		return lightArray;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
//	public float getPower() {
//		return power;
//	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		light.fillSettings(settings.get("lights").get(0));
		calibrated = settings.get("calibrated").asBoolean();
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		light.fillStatus(status.get("lights").get(0), status.get("inputs").get(0));
		internalTmp = status.at("/tmp/tC").floatValue(); //status.get("tmp").get("tC").asDouble();
		power = status.get("meters").get(0).get("power").floatValue();
	}
	
	public boolean calibrated() {
		return calibrated;
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "factory_reset_from_switch", "pulse_mode", "transition", "fade_rate", "min_brightness", "zcross_debounce")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		JsonNode nightMode = settings.get("night_mode");
		if(nightMode.get("enabled").asBoolean()) {
			errors.add(sendCommand("/settings/night_mode?" + jsonNodeToURLPar(nightMode, "enabled", "start_time", "end_time", "brightness")));
		} else {
			errors.add(sendCommand("/settings/night_mode?enabled=false"));
		}
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		JsonNode warmUp = settings.get("warm_up");
		if(warmUp.get("enabled").asBoolean()) {
			errors.add(sendCommand("/settings/warm_up?" + jsonNodeToURLPar(warmUp, "enabled", "brightness", "time")));
		} else {
			errors.add(sendCommand("/settings/warm_up?enabled=false"));
		}
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(light.restore(settings.get("lights").get(0)));
	}

	@Override
	public String toString() {
		return super.toString() + ": " + light;
	}
}