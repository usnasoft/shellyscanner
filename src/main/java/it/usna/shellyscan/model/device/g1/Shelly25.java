package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.g1.modules.Roller;
import it.usna.shellyscan.model.device.meters.MetersPower;
import it.usna.shellyscan.model.device.modules.ModuleHolder;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.shellyscan.model.device.modules.RollerCommander;

public class Shelly25 extends AbstractG1Device implements ModuleHolder, RollerCommander, InternalTmpHolder {
	public final static String ID = "SHSW-25";
	private final static Meters.Type[] SUPPORTED_MEASURES_C1 = new Meters.Type[] {Meters.Type.W, Meters.Type.V};
	private boolean modeRelay;
	private Relay relay0, relay1;
	private Roller roller;
	private float internalTmp;
	private float power0, power1;
	private float voltage;
	private Meters[] meters;
	
	private final static String MODE_RELAY = "relay";

	public Shelly25(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public float getValue(Type t) {
						return t == Type.V ? voltage : power0;
					}

					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES_C1;
					}
				},
				new MetersPower() {
					@Override
					public float getValue(Type t) {
						return power1;
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly 2.5";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getModulesCount() {
		return modeRelay ? 2 : 0;
	}
	
	@Override
	public Relay getModule(int index) {
		return index == 0 ? relay0 : relay1;
	}

	@Override
	public Relay[] getModules() {
		return new Relay[] {relay0, relay1};
	}
	
	@Override
	public int getRollersCount() {
		return modeRelay ? 0 : 1;
	}
	
	@Override
	public Roller getRoller(int index) {
		return roller;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	public float getPower(int index) {
		return (index == 0) ? power0 : power1;
	}
	
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
			if(relay0 == null /*|| relay1 == null*/) {
				relay0 = new Relay(this, 0);
				relay1 = new Relay(this, 1);
				roller = null; // modeRelay change
			}
			JsonNode ralaysSetting = settings.get("relays");
			relay0.fillSettings(ralaysSetting.get(0));
			relay1.fillSettings(ralaysSetting.get(1));
		} else {
			if(roller == null) {
				roller = new Roller(this, 0);
				relay0 = relay1 = null; // modeRelay change
			}
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
//		internalTmp = (float)status.get("tmp").get("tC").asDouble();
		internalTmp = status.get("temperature").floatValue();
		final JsonNode meters = status.get("meters");
		power0 = meters.get(0).get("power").floatValue();
		power1 = meters.get(1).get("power").floatValue();
		voltage = status.path("voltage").floatValue();
		if(modeRelay) {
			JsonNode ralaysStatus = status.get("relays");
			JsonNode ralaysInputs = status.get("inputs");
			relay0.fillStatus(ralaysStatus.get(0), ralaysInputs.get(0));
			relay1.fillStatus(ralaysStatus.get(1), ralaysInputs.get(1));
		} else {
			roller.fillStatus(status.get("rollers").get(0));
		}
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "longpush_time", "factory_reset_from_switch", "mode", "wifirecovery_reboot_enabled"/*, "max_power"*/)));
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