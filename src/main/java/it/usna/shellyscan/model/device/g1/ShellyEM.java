package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.ModulesHolder;

public class ShellyEM extends AbstractG1Device implements ModulesHolder {
	public final static String ID = "SHEM";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.VAR, Meters.Type.PF, Meters.Type.V};
	private Relay relay = new Relay(this, 0);
	private float power[] = new float[2];
	private float reactive[] = new float[2];
	private float pf[] = new float[2];
	private float voltage[] = new float[2];
	private String meterName[] = new String[2];
	private Meters meters[];

	public ShellyEM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		class EMMeters extends Meters implements LabelHolder {
			private int ind;
			private EMMeters(int ind) {
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
				} else if(t == Type.VAR) {
					return reactive[ind];
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
				return meterName[ind] + ": " + Type.W + "=" + power[ind] + " " + Type.VAR + "=" + reactive[ind] + " " + Type.PF + "=" + pf[ind] + " " + Type.V + "=" + voltage[ind];
			}
		}
		
		meters = new Meters[] {new EMMeters(0), new EMMeters(1)};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly EM";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
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
	
	public float getReactive(int index) {
		return reactive[index];
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
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay.fillStatus(status.get("relays").get(0));
		
		final JsonNode eMeters = status.get("emeters");
		final JsonNode eMeters0 = eMeters.get(0);
		power[0] = eMeters0.get("power").floatValue();
		reactive[0] = eMeters0.get("reactive").floatValue();
		pf[0] = eMeters0.get("pf").floatValue();
		voltage[0] = eMeters0.get("voltage").floatValue();
		final JsonNode eMeters1 = eMeters.get(1);
		power[1] = eMeters1.get("power").floatValue();
		reactive[1] = eMeters1.get("reactive").floatValue();
		pf[1] = eMeters1.get("pf").floatValue();
		voltage[1] = eMeters1.get("voltage").floatValue();
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
		errors.add(relay.restore(settings.get("relays").get(0)));
	}
}