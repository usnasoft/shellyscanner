package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.modules.Relay;

public class ShellyProEM50 extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public final static String ID = "ProEM";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.VAR, Meters.Type.PF, Meters.Type.V, Meters.Type.I, Meters.Type.FREQ};
	private Relay relay = new Relay(this, 0);
	private Relay[] relays = new Relay[] {relay};
	private float internalTmp;
	private float power[] = new float[2];
	private float reactive[] = new float[2];
	private float voltage[] = new float[2];
	private float current[] = new float[2];
	private float pf[] = new float[2];
	private float freq[] = new float[2];
	private String meterName[] = new String[2];
	private Meters meters[];

	public ShellyProEM50(InetAddress address, int port, String hostname) {
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
				} else if(t == Type.I) {
					return current[ind];
				} else if(t == Type.PF) {
					return pf[ind];
				} else if(t == Type.FREQ) {
					return freq[ind];
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
		
		meters = new Meters[] {new EMMeters(0), new EMMeters(1)};
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro EM-50";
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
		relay.fillSettings(configuration.get("switch:0"));
		
		meterName[0] = configuration.get("em1:0").get("name").asText("");
		meterName[1] = configuration.get("em1:1").get("name").asText("");
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus);

		JsonNode em1_0 = status.get("em1:0");
		power[0] = em1_0.get("act_power").floatValue();
		reactive[0] = em1_0.get("aprt_power").floatValue();
		current[0] = em1_0.get("current").floatValue();
		pf[0] = em1_0.get("pf").floatValue();
		voltage[0] = em1_0.get("voltage").floatValue();
		freq[0] = em1_0.get("freq").floatValue();
		
		JsonNode em1_1 = status.get("em1:1");
		power[1] = em1_1.get("act_power").floatValue();
		reactive[1] = em1_1.get("aprt_power").floatValue();
		current[1] = em1_1.get("current").floatValue();
		pf[1] = em1_1.get("pf").floatValue();
		voltage[1] = em1_1.get("voltage").floatValue();
		freq[1] = em1_1.get("freq").floatValue();

		internalTmp = switchStatus.get("temperature").get("tC").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
		errors.add(relay.restore(config));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		
		errors.add(postCommand("EM1.SetConfig", createIndexedRestoreNode(config, "em1", 0)));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("EM1.SetConfig", createIndexedRestoreNode(config, "em1", 1)));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}