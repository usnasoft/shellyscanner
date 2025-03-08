package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.RestoreMsg;

public class Shelly3EM63 extends AbstractG3Device implements InternalTmpHolder {
	public final static String ID = "S3EMG3";
	private float internalTmp;
	private float power[] = new float[3], totPower;
	private float apparent[] = new float[3], totApparent;
	private float voltage[] = new float[3];
	private float current[] = new float[3], totCurrent;
	private float pf[] = new float[3];
	private float freq[] = new float[3];
	private String meterName[] = new String[3];
	private Meters meters[];
	private boolean triphase;
	
	private final static String MODE_TRIPHASE = "triphase";

	public Shelly3EM63(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	protected void init(JsonNode devInfo) throws IOException {
		this.init(devInfo.get("profile").textValue().equals(MODE_TRIPHASE));
		super.init(devInfo);
	}
	
	private void init(boolean triphase) {
		this.triphase = triphase;
		if(triphase) {
			meters = new Meters[] {new EMMeters(0), new EMMeters(1), new EMMeters(2), new TotalEMMeters()};
			meterName[1] = meterName[2] = "";
		} else {
			meters = new Meters[] {new EMMeters(0), new EMMeters(1), new EMMeters(2)};
		}
	}

	@Override
	public String getTypeName() {
		return "Shelly 3EM-63";
	}

	@Override
	public String getTypeID() {
		return ID;
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
		boolean current3phase = configuration.get("sys").get("device").get("profile").asText().equals(MODE_TRIPHASE);
		if(current3phase != triphase) {
			init(current3phase);
		}
		if(current3phase) {
			meterName[0] = configuration.get("em:0").get("name").asText("");
		} else {
			meterName[0] = configuration.get("em1:0").get("name").asText("");
			meterName[1] = configuration.get("em1:1").get("name").asText("");
			meterName[2] = configuration.get("em1:2").get("name").asText("");
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(triphase) {
			JsonNode em_0 = status.get("em:0");
			
			power[0] = em_0.get("a_act_power").floatValue();
			apparent[0] = em_0.get("a_aprt_power").floatValue();
			current[0] = em_0.get("a_current").floatValue();
			pf[0] = em_0.get("a_pf").floatValue();
			voltage[0] = em_0.get("a_voltage").floatValue();
			freq[0] = em_0.get("a_freq").floatValue();
			
			power[1] = em_0.get("b_act_power").floatValue();
			apparent[1] = em_0.get("b_aprt_power").floatValue();
			current[1] = em_0.get("b_current").floatValue();
			pf[1] = em_0.get("b_pf").floatValue();
			voltage[1] = em_0.get("b_voltage").floatValue();
			freq[1] = em_0.get("b_freq").floatValue();
			
			power[2] = em_0.get("c_act_power").floatValue();
			apparent[2] = em_0.get("c_aprt_power").floatValue();
			current[2] = em_0.get("c_current").floatValue();
			pf[2] = em_0.get("c_pf").floatValue();
			voltage[2] = em_0.get("c_voltage").floatValue();
			freq[2] = em_0.get("c_freq").floatValue();

			totPower = em_0.path("total_act_power").floatValue();
			totApparent = em_0.path("total_aprt_power").floatValue();
			totCurrent = em_0.path("total_current").floatValue();
		} else {
			JsonNode em1_0 = status.get("em1:0");
			power[0] = em1_0.get("act_power").floatValue();
			apparent[0] = em1_0.get("aprt_power").floatValue();
			current[0] = em1_0.get("current").floatValue();
			pf[0] = em1_0.get("pf").floatValue();
			voltage[0] = em1_0.get("voltage").floatValue();
			freq[0] = em1_0.get("freq").floatValue();

			JsonNode em1_1 = status.get("em1:1");
			power[1] = em1_1.get("act_power").floatValue();
			apparent[1] = em1_1.get("aprt_power").floatValue();
			current[1] = em1_1.get("current").floatValue();
			pf[1] = em1_1.get("pf").floatValue();
			voltage[1] = em1_1.get("voltage").floatValue();
			freq[1] = em1_1.get("freq").floatValue();

			JsonNode em1_2 = status.get("em1:2");
			power[2] = em1_2.get("act_power").floatValue();
			apparent[2] = em1_2.get("aprt_power").floatValue();
			current[2] = em1_2.get("current").floatValue();
			pf[2] = em1_2.get("pf").floatValue();
			voltage[2] = em1_2.get("voltage").floatValue();
			freq[2] = em1_2.get("freq").floatValue();
		}

		internalTmp = status.path("temperature:0").path("tC").floatValue();
	}
	
	private class EMMeters extends Meters implements LabelHolder {
		private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.VA, Meters.Type.PF, Meters.Type.V, Meters.Type.I, Meters.Type.FREQ};
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
			} else if(t == Type.VA) {
				return apparent[ind];
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

	private class TotalEMMeters extends Meters {
		private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.VA, Meters.Type.I};

		@Override
		public Type[] getTypes() {
			return SUPPORTED_MEASURES;
		}

		@Override
		public float getValue(Type t) {
			if(t == Type.W) {
				return totPower;
			} else if(t == Type.VA) {
				return totApparent;
			} else { //if(t == Type.I)
				return totCurrent;
			}
		}
		
		@Override
		public String toString() {
			return Type.W + "=" + totPower + " " + Type.VA + "=" + totApparent + " " + Type.I + "=" + totCurrent;
		}
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		boolean backModeTriphase = MODE_TRIPHASE.equals(devInfo.get("profile").asText());
		if(backModeTriphase != triphase) {
			res.put(RestoreMsg.ERR_RESTORE_MODE_TRIPHASE, null);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
		if(triphase) {
			ObjectNode conf = createIndexedRestoreNode(config, "em", 0);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM.SetConfig", conf));
		} else {
			ObjectNode conf = createIndexedRestoreNode(config, "em1", 0);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM1.SetConfig", conf));
			
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			conf = createIndexedRestoreNode(config, "em1", 1);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM1.SetConfig", conf));
			
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			conf = createIndexedRestoreNode(config, "em1", 2);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM1.SetConfig", conf));
		}
	}
}