package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyPlus1PM extends AbstractG2Device implements RelayCommander, InternalTmpHolder {
	public final static String ID = "Plus1PM";
//	private final static JsonPointer SW_TEMP_P = JsonPointer.valueOf("/temperature/tC");
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.V, Meters.Type.I};
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private float power;
	private float voltage;
	private float current;
	private Meters[] meters;

	public ShellyPlus1PM(InetAddress address, String hostname) {
		super(address, hostname);
		
		meters = new Meters[] {
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power;
						} else if(t == Meters.Type.I) {
							return current;
						} else {
							return voltage;
						}
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly +1PM";
	}
	
	@Override
	public Relay getRelay(int index) {
		return relay;
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public RelayInterface[] getRelays() {
		return new Relay[] {relay};
	}
	
	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	public float getPower() {
		return power;
	}
	
	public float getVoltage() {
		return voltage;
	}
	
	public float getCurrente() {
		return current;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay.fillSettings(configuration.get("switch:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get("input:0"));
		internalTmp = (float)switchStatus.path("temperature").path("tC").asDouble();
		power = (float)switchStatus.get("apower").asDouble(0);
		voltage = (float)switchStatus.get("voltage").asDouble(0);
		current = (float)switchStatus.get("current").asDouble(0);
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(Input.restore(this, configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}