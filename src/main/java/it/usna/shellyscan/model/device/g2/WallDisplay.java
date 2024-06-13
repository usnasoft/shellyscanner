package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.ThermostatG2;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.Thermostat;
import it.usna.shellyscan.model.device.modules.ThermostatCommander;

/**
 * Shelly Wall Display
 * @author usna
 */
public class WallDisplay extends AbstractG2Device implements RelayCommander, ThermostatCommander {
	public final static String ID = "WallDisplay";
//	private boolean modeRelay; // otherwise "thermostat"
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.L};
	private float temp;
	private float humidity;
	private int lux;
	private Meters[] meters;
	private Relay relay = new Relay(this, 0);
	private Relay[] relays = new Relay[] {relay};
	private ThermostatG2 thermostat = new ThermostatG2(this);

	public WallDisplay(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.H) {
							return lux;
						} else if(t == Meters.Type.H) {
							return humidity;
						} else {
							return temp;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Wall Display";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getRelayCount() {
		return relays != null ? 1 : 0;
	}
	
	@Override
	public Relay getRelay(int index) {
		return relay;
	}
	
	@Override
	public Relay[] getRelays() {
		return relays;
	}
	
	@Override
	public Thermostat getThermostat() {
		return thermostat;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		JsonNode thermostatConf = configuration.get("thermostat:0");
		if(thermostatConf != null) {
			if(thermostat == null) {
				thermostat = new ThermostatG2(this);
				relay = null;
				relays = null;
			}
			thermostat.fillSettings(thermostatConf);
		} else {
			if(relay == null) {
				relay = new Relay(this, 0);
				relays = new Relay[] {relay};
				thermostat = null;
			}
			relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		temp = status.path("temperature:0").path("tC").floatValue();
		humidity = status.path("humidity:0").path("rh").floatValue();
		lux = status.path("illuminance:0").path("lux").intValue();
		if(relay != null) {
			relay.fillStatus(status.get("switch:0"), status.get("input:0"));
		} else {
			thermostat.fillStatus(status.get("thermostat:0"));
		}
	}
	
	public float getTemp() {
		return temp;
	}
	
	public float getHumidity() {
		return humidity;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
		// todo  "relay_in_thermostat" : true/false,
	}
	
	@Override
	public String toString() {
		if(relay != null) {
			return super.toString() + " Relay: " + relay;
		} else {
			//todo
			return super.toString();
		}
	}
}