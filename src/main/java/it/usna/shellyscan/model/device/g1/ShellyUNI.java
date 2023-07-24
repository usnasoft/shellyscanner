package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyUNI extends AbstractG1Device implements RelayCommander {
	public final static String ID = "SHUNI-1";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.V};
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private float voltage;
	private Meters[] meters;

	public ShellyUNI(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						return voltage;
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly UNI";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getRelayCount() {
		return 2;
	}
	
	@Override
	public RelayInterface getRelay(int index) {
		return index == 0 ? relay0 : relay1;
	}
	
	@Override
	public RelayInterface[] getRelays() {
		return new RelayInterface[] {relay0, relay1};
	}
	
	public float getVoltage() {
		return voltage;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		JsonNode ralaysSetting = settings.get("relays");
		relay0.fillSettings(ralaysSetting.get(0));
		relay1.fillSettings(ralaysSetting.get(1));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode ralaysStatus = status.get("relays");
		relay0.fillStatus(ralaysStatus.get(0), status.get("inputs").get(0));
		relay1.fillStatus(ralaysStatus.get(1), status.get("inputs").get(1));
		voltage = (float)status.get("adcs").get(0).get("voltage").asDouble();
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "longpush_time", "factory_reset_from_switch")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay0.restore(settings.get("relays").get(0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay1.restore(settings.get("relays").get(1)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		JsonNode adc0 = settings.get("adcs").get(0);
		try {
			errors.add(sendCommand("/settings/adc/0?range=" + adc0.get("range").asText()));
			JsonNode relAct = settings.get("relay_actions");
			if(relAct.size() > 0) {
				for(int index = 0; index < relAct.size(); index++) {
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					errors.add(sendCommand("/settings/adc/0/relay_actions." + index + "?" + AbstractG1Device.jsonEntryIteratorToURLPar(relAct.get(index).fields())));
				}
			}
		} catch(Exception e) {
			e.printStackTrace(); // experimental
		}
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
	}
}

/*
...
"fw": "20230503-102354/v1.13.0-g9aed950",
...
"adcs": [
		{
			"range": 12,
			"offset": 0.0,
			"relay_actions": [
				{
					"over_threshold": 0,
					"over_act": "disabled",
					"under_threshold": 0,
					"under_act": "disabled"
				},
				{
					"over_threshold": 0,
					"over_act": "disabled",
					"under_threshold": 0,
					"under_act": "disabled"
				}
			]
		}
	],
*/