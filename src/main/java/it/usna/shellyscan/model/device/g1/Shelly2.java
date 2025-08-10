package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g1.meters.MetersPower;
import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.g1.modules.Roller;
import it.usna.shellyscan.model.device.modules.DeviceModule;

public class Shelly2 extends AbstractG1Device implements ModulesHolder {
	public final static String ID = "SHSW-21";
	private boolean modeRelay;
	private Relay relay0, relay1;
	private Roller roller;
	private float power;
	private Meters[] meters;
	
	private final String MODE_RELAY = "relay";

	public Shelly2(InetAddress address, int port, String hostname) {
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
		return "Shelly 2";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getModulesCount() {
		return modeRelay ? 2 : 1;
	}

	@Override
	public DeviceModule[] getModules() {
		if(modeRelay) {
			return new Relay[] {relay0, relay1};
		} else {
			return new Roller[] {roller};
		}
	}
	
//	public float getPower() {
//		return power;
//	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	public boolean modeRelay() {
		return modeRelay;
	}

	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		modeRelay = MODE_RELAY.equals(settings.get("mode").asText());
		if(modeRelay) {
			JsonNode ralaysSetting = settings.get("relays");
			if(relay0 == null) {
				relay0 = new Relay(this, 0);
			}
			relay0.fillSettings(ralaysSetting.get(0));
			if(relay1 == null) {
				relay1 = new Relay(this, 1);
			}
			relay1.fillSettings(ralaysSetting.get(1));
			roller = null; // modeRelay change
		} else {
			if(roller == null) {
				roller = new Roller(this, 0);
			}
			relay0 = relay1 = null; // modeRelay change
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		final JsonNode meters = status.get("meters");
		power = (float)meters.get(0).get("power").asDouble(0);
		if(modeRelay) {
			JsonNode ralaysStatus = status.get("relays");
			relay0.fillStatus(ralaysStatus.get(0), status.get("inputs").get(0));
			relay1.fillStatus(ralaysStatus.get(1), status.get("inputs").get(1));
		} else {
			roller.fillStatus(status.get("rollers").get(0));
		}
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "longpush_time", "factory_reset_from_switch", "mode", "wifirecovery_reboot_enabled"/*, "max_power"*/)));
		final boolean backModeRelay = MODE_RELAY.equals(settings.get("mode").asText());
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		if(backModeRelay) {
			Relay rel = new Relay(this, 0); // just for restore; object is later refreshed (fill called)
			errors.add(rel.restore(settings.get("relays").get(0)));
			rel = new Relay(this, 1);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(rel.restore(settings.get("relays").get(1)));
		} else {
			final Roller roller = new Roller(this, 0); // just for restore; object is later refreshed (fill called)
			errors.add(roller.restore(settings.get("rollers").get(0)));
		}
	}

	@Override
	public String toString() {
		if(modeRelay) {
			return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
		} else {
			return super.toString() + " Roller: " + roller;
		}
	}
}