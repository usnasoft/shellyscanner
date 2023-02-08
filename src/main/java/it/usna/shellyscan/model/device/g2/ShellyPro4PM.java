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

public class ShellyPro4PM extends AbstractG2Device implements RelayCommander, InternalTmpHolder {
	public final static String ID = "Pro4PM";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.V, Meters.Type.I};
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay relay2 = new Relay(this, 2);
	private Relay relay3 = new Relay(this, 3);
	private float internalTmp;
	private float power0, power1, power2, power3;
	private float voltage0, voltage1, voltage2, voltage3;
	private float current0, current1, current2, current3;
	private Meters[] meters;
	RelayInterface[] relays = new RelayInterface[] {relay0, relay1, relay2, relay3};

	public ShellyPro4PM(InetAddress address, String hostname) {
		super(address, hostname);

		meters = new Meters[] {
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power0;
						} else if(t == Meters.Type.I) {
							return current0;
						} else {
							return voltage0;
						}
					}
				},
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power1;
						} else if(t == Meters.Type.I) {
							return current1;
						} else {
							return voltage1;
						}
					}
				},
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power2;
						} else if(t == Meters.Type.I) {
							return current2;
						} else {
							return voltage2;
						}
					}
				},
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power3;
						} else if(t == Meters.Type.I) {
							return current3;
						} else {
							return voltage3;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro 4PM";
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
	public Relay getRelay(int index) {
		return (index == 0) ? relay0 : relay1;
	}

	@Override
	public RelayInterface[] getRelays() {
		return relays;//new RelayInterface[] {relay0, relay1};
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	//	public float getPower() {
	//		return power0;
	//	}
	//
	//	public float getVoltage() {
	//		return voltage0;
	//	}
	//
	//	public float getCurrente() {
	//		return current0;
	//	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"));
		relay1.fillSettings(configuration.get("switch:1"));
		relay2.fillSettings(configuration.get("switch:2"));
		relay3.fillSettings(configuration.get("switch:3"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0, status.get("input:0"));
		power0 = (float)switchStatus0.get("apower").asDouble();
		voltage0 = (float)switchStatus0.get("voltage").asDouble();
		current0 = (float)switchStatus0.get("current").asDouble();

		JsonNode switchStatus1 = status.get("switch:1");
		relay1.fillStatus(switchStatus1, status.get("input:1"));
		power1 = (float)switchStatus1.get("apower").asDouble();
		voltage1 = (float)switchStatus1.get("voltage").asDouble();
		current1 = (float)switchStatus1.get("current").asDouble();

		JsonNode switchStatus2 = status.get("switch:2");
		relay2.fillStatus(switchStatus2, status.get("input:1"));
		power2 = (float)switchStatus2.get("apower").asDouble();
		voltage2 = (float)switchStatus2.get("voltage").asDouble();
		current2 = (float)switchStatus2.get("current").asDouble();

		JsonNode switchStatus3 = status.get("switch:3");
		relay3.fillStatus(switchStatus3, status.get("input:1"));
		power3 = (float)switchStatus3.get("apower").asDouble();
		voltage3 = (float)switchStatus3.get("voltage").asDouble();
		current3 = (float)switchStatus3.get("current").asDouble();

		internalTmp = (float)switchStatus0.path("temperature").path("tC").asDouble();
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(Input.restore(this,configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, "1"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, "2"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, "3"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

		errors.add(relay0.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay1.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay2.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay3.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1 + "; Relay2: " + relay2 + "; Relay3: " + relay3;
	}
}