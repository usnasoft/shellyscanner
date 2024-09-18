package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g1.modules.LightWhite;
import it.usna.shellyscan.model.device.meters.MetersPower;
import it.usna.shellyscan.model.device.modules.DeviceModule;

public class ShellyDUO extends AbstractG1Device implements ModulesHolder {
	public final static String ID = "SHBDUO-1";
	private LightWhite light = new LightWhite(this, "/light/", 0);
	private LightWhite[] lightArray = new LightWhite[] {light};
	private float power;
	private Meters[] meters;

	public ShellyDUO(InetAddress address, int port, String hostname) {
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
		return "DUO";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public DeviceModule getModule(int index) {
		return light;
	}

	@Override
	public DeviceModule[] getModules() {
		return lightArray;
	}
	
	public float getPower() {
		return power;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		light.fillSettings(settings.get("lights").get(0));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		light.fillStatus(status.get("lights").get(0));
		power = (float)status.get("meters").get(0).get("power").asDouble(0);
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "transition")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		JsonNode nightMode = settings.get("night_mode");
		if(nightMode.get("enabled").asBoolean()) {
			errors.add(sendCommand("/settings/night_mode?" + jsonNodeToURLPar(nightMode, "enabled", "start_time", "end_time", "brightness")));
		} else {
			errors.add(sendCommand("/settings/night_mode?enabled=false"));
		}
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(light.restore(settings.get("lights").get(0)));
	}

	@Override
	public String toString() {
		return super.toString() + " Light: " + light;
	}
}