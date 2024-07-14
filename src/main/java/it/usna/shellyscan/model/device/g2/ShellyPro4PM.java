package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.modules.ModuleHolder;

public class ShellyPro4PM extends AbstractProDevice implements ModuleHolder, InternalTmpHolder {
	public final static String ID = "Pro4PM";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay relay2 = new Relay(this, 2);
	private Relay relay3 = new Relay(this, 3);
	private float internalTmp;
	private float power0, power1, power2, power3;
	private float voltage0, voltage1, voltage2, voltage3;
	private float current0, current1, current2, current3;
	private float pf0, pf1, pf2, pf3;
	private Meters[] meters;
	private Relay[] relays = new Relay[] {relay0, relay1, relay2, relay3};

	public ShellyPro4PM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

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
						} else if(t == Meters.Type.PF) {
							return pf0;
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
						} else if(t == Meters.Type.PF) {
							return pf1;
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
						} else if(t == Meters.Type.PF) {
							return pf2;
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
						} else if(t == Meters.Type.PF) {
							return pf3;
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
	public int getModulesCount() {
		return 4;
	}

	@Override
	public Relay getModule(int index) {
		return relays[index];
	}

	@Override
	public Relay[] getModules() {
		return relays;
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
	//	public float getCurrent() {
	//		return current0;
	//	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		relay1.fillSettings(configuration.get("switch:1"), configuration.get("input:1"));
		relay2.fillSettings(configuration.get("switch:2"), configuration.get("input:2"));
		relay3.fillSettings(configuration.get("switch:3"), configuration.get("input:3"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0, status.get("input:0"));
		power0 = switchStatus0.get("apower").floatValue();
		voltage0 = switchStatus0.get("voltage").floatValue();
		current0 = switchStatus0.get("current").floatValue();
		pf0 = switchStatus0.get("pf").floatValue();

		JsonNode switchStatus1 = status.get("switch:1");
		relay1.fillStatus(switchStatus1, status.get("input:1"));
		power1 = switchStatus1.get("apower").floatValue();
		voltage1 = switchStatus1.get("voltage").floatValue();
		current1 = switchStatus1.get("current").floatValue();
		pf1 = switchStatus1.get("pf").floatValue();

		JsonNode switchStatus2 = status.get("switch:2");
		relay2.fillStatus(switchStatus2, status.get("input:1"));
		power2 = switchStatus2.get("apower").floatValue();
		voltage2 = switchStatus2.get("voltage").floatValue();
		current2 = switchStatus2.get("current").floatValue();
		pf2 = switchStatus2.get("pf").floatValue();

		JsonNode switchStatus3 = status.get("switch:3");
		relay3.fillStatus(switchStatus3, status.get("input:1"));
		power3 = switchStatus3.get("apower").floatValue();
		voltage3 = switchStatus3.get("voltage").floatValue();
		current3 = switchStatus3.get("current").floatValue();
		pf3 = switchStatus3.get("pf").floatValue();

		internalTmp = switchStatus0.path("temperature").path("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws JsonProcessingException, InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
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
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("Ui.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("ui")) + "}"));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1 + "; Relay2: " + relay2 + "; Relay3: " + relay3;
	}
}