package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g1.modules.Relay;

public class Shelly3EM extends AbstractG1Device implements ModulesHolder {
	public final static String ID = "SHEM-3";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay = new Relay(this, 0);
	private float power[] = new float[3];
	private float current[] = new float[3];
	private float pf[] = new float[3];
	private float voltage[] = new float[3];
	private String meterName[] = new String[3];
	private Meters meters[];

	public Shelly3EM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		class EM3Meters extends Meters implements LabelHolder {
			private int ind;
			private EM3Meters(int ind) {
				this.ind = ind;
			}
			
			@Override
			public Type[] getTypes() {
				return SUPPORTED_MEASURES;
			}

			@Override
			public float getValue(Type t) {
				if(t == Type.W) {
					return power[ind];
				} else if(t == Type.I) {
					return current[ind];
				} else if(t == Type.PF) {
					return pf[ind];
				} else {
					return voltage[ind];
				}
			}

			@Override
			public String getLabel() {
				return meterName[ind];
			}
			
			@Override
			public String toString() {
				return meterName[ind] + ": " + Type.W + "=" + power[ind] + " " + Type.I + "=" + current[ind] + " " + Type.PF + "=" + pf[ind] + " " + Type.V + "=" + voltage[ind];
			}
		}

		meters = new Meters[] {new EM3Meters(0), new EM3Meters(1), new EM3Meters(2)};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly 3EM";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
//	@Override
//	public RelayInterface getRelay(int index) {
//		return relay;
//	}
//	
//	@Override
//	public RelayInterface[] getRelays() {
//		return new RelayInterface[] {relay};
//	}
	
	@Override
	public Relay getModule(int index) {
		return relay;
	}

	@Override
	public Relay[] getModules() {
		return new Relay[] {relay};
	}
	
	public float getPower(int index) {
		return power[index];
	}
	
	public float getCurrent(int index) {
		return current[index];
	}
	
	public float getPF(int index) {
		return pf[index];
	}
	
	public float getVoltage(int index) {
		return voltage[index];
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		relay.fillSettings(settings.get("relays").get(0));
		
		JsonNode eMeters = settings.get("emeters");
		meterName[0] = eMeters.get(0).get("name").asText("");
		meterName[1] = eMeters.get(1).get("name").asText("");
		meterName[2] = eMeters.get(2).get("name").asText("");
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay.fillStatus(status.get("relays").get(0));

		JsonNode eMeters = status.get("emeters");
		JsonNode eMeters0 = eMeters.get(0);
		power[0] = (float)eMeters0.get("power").asDouble();
		current[0] = (float)eMeters0.get("current").asDouble();
		pf[0] = (float)eMeters0.get("pf").asDouble();
		voltage[0] = (float)eMeters0.get("voltage").asDouble();
		JsonNode eMeters1 = eMeters.get(1);
		power[1] = (float)eMeters1.get("power").asDouble();
		current[1] = (float)eMeters1.get("current").asDouble();
		pf[1] = (float)eMeters1.get("pf").asDouble();
		voltage[1] = (float)eMeters1.get("voltage").asDouble();
		JsonNode eMeters2 = eMeters.get(2);
		power[2] = (float)eMeters2.get("power").asDouble();
		current[2] = (float)eMeters2.get("current").asDouble();
		pf[2] = (float)eMeters2.get("pf").asDouble();
		voltage[2] = (float)eMeters2.get("voltage").asDouble();
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "wifirecovery_reboot_enabled")));
		JsonNode meters = settings.get("emeters");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings/emeters/0?" + jsonNodeToURLPar(meters.get(0), "name", "appliance_type", "max_power")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings/emeters/1?" + jsonNodeToURLPar(meters.get(1), "name", "appliance_type", "max_power")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings/emeters/2?" + jsonNodeToURLPar(meters.get(2), "name", "appliance_type", "max_power")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(settings.get("relays").get(0)));
	}
}