package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;

public class ShellyPro1PM extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "Pro1PM";
	public static final String MODEL = "SPSW-201PE16EU";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay = new Relay(this, 0);
	private String inputKey;
	private float internalTmp;
	private float power;
	private float voltage;
	private float current;
	private float pf;
	private Meters[] meters;
	private Relay[] relays = new Relay[] {relay};

	public ShellyPro1PM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

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
						} else if(t == Meters.Type.PF) {
							return pf;
						} else {
							return voltage;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro 1PM";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public Relay[] getModules() {
		return relays;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		
		JsonNode switchConf0 = configuration.get("switch:0");
		inputKey = switchConf0.path("input_id").intValue() == 0 ? "input:0" : "input:1";;
		relay.fillSettings(switchConf0, configuration.get(inputKey));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus0 = status.get("switch:0");
		relay.fillStatus(switchStatus0, status.get(inputKey));
		power = switchStatus0.get("apower").floatValue();
		voltage = switchStatus0.get("voltage").floatValue();
		current = switchStatus0.get("current").floatValue();
		pf = switchStatus0.get("pf").floatValue();

		internalTmp = switchStatus0.get("temperature").get("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}