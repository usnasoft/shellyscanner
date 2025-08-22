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
	public static final String ID = "SHEM-3";
	private Relay relay = new Relay(this, 0);
	private Relay[] relayArray = new Relay[] {relay};
	private float[] power = new float[3];
	private float[] current = new float[3];
	private float[] pf = new float[3];
	private float[] voltage = new float[3];
	private String[] meterName = new String[3];
	private Meters[] meters;

	public Shelly3EM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		class EM3Meters extends Meters implements LabelHolder {
			private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
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

	@Override
	public Relay[] getModules() {
		return relayArray;
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
		meterName[0] = eMeters.get(0).path("name").asText("");
		meterName[1] = eMeters.get(1).path("name").asText("");
		meterName[2] = eMeters.get(2).path("name").asText("");
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay.fillStatus(status.get("relays").get(0));

		JsonNode eMeters = status.get("emeters");
		JsonNode eMeters0 = eMeters.get(0);
		power[0] = eMeters0.get("power").floatValue();
		current[0] = eMeters0.get("current").floatValue();
		pf[0] = eMeters0.get("pf").floatValue();
		voltage[0] = eMeters0.get("voltage").floatValue();
		JsonNode eMeters1 = eMeters.get(1);
		power[1] = eMeters1.get("power").floatValue();
		current[1] = eMeters1.get("current").floatValue();
		pf[1] = eMeters1.get("pf").floatValue();
		voltage[1] = eMeters1.get("voltage").floatValue();
		JsonNode eMeters2 = eMeters.get(2);
		power[2] = eMeters2.get("power").floatValue();
		current[2] = eMeters2.get("current").floatValue();
		pf[2] = eMeters2.get("pf").floatValue();
		voltage[2] = eMeters2.get("voltage").floatValue();
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "wifirecovery_reboot_enabled")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(settings.get("relays").get(0)));
		
		JsonNode storedMeters = settings.get("emeters");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings/emeters/0?" + jsonNodeToURLPar(storedMeters.get(0), "name", "appliance_type", "max_power")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings/emeters/1?" + jsonNodeToURLPar(storedMeters.get(1), "name", "appliance_type", "max_power")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings/emeters/2?" + jsonNodeToURLPar(storedMeters.get(2), "name", "appliance_type", "max_power")));
	}
}