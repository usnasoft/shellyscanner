package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.LabelHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;

/**
 * Shelly Shelly Plus UNI model
 * @author usna
 */
public class ShellyPlusUNI extends AbstractG2Device implements ModulesHolder {
	private final static Logger LOG = LoggerFactory.getLogger(ShellyPlusUNI.class);
	public final static String ID = "PlusUni";
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay[] relays = new Relay[] {relay0, relay1};
	private Meters[] meters;
	private String input2Name;
	private int input2Count;
	private int input2Freq;
	private SensorAddOn addOn; // integrated
	
	public ShellyPlusUNI(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	protected void init(JsonNode devInfo) throws IOException {
		configure();
		super.init(devInfo);
	}
	
	private void configure() throws IOException {
		addOn = new SensorAddOn(this);
		meters = (addOn.getTypes().length > 0) ? new Meters[] {new CounterMeters(), addOn} : new Meters[] {new CounterMeters()};
	}

	@Override
	public String getTypeName() {
		return "Shelly +UNI";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public int getModulesCount() {
		return 2;
	}
	
	@Override
	public Relay getModule(int index) {
		return (index == 0) ? relay0 : relay1;
	}

	@Override
	public Relay[] getModules() {
		return relays;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		relay1.fillSettings(configuration.get("switch:1"), configuration.get("input:1"));
		
		input2Name = configuration.get("input:2").get("name").asText("");
		
		addOn.fillSettings(configuration);
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay0.fillStatus(status.get("switch:0"), status.get("input:0"));
		relay1.fillStatus(status.get("switch:1"), status.get("input:1"));
		
		JsonNode input2 = status.get("input:2");
		input2Freq = input2.get("freq").intValue();
		input2Count = input2.get("counts").path("total").intValue();
		
		addOn.fillStatus(status);
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		try {
			configure(); // maybe useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		} catch (IOException e) {
			LOG.error("restoreCheck", e);
		}
		SensorAddOn.restoreCheck(this, addOn, backupJsons, res);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws IOException, InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 2));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		SensorAddOn.restore(this, addOn, backupJsons, errors);
	}
	
	private class CounterMeters extends Meters implements LabelHolder {
		private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.NUM, Meters.Type.FREQ};
		@Override
		public float getValue(Type t) {
			if(t == Meters.Type.FREQ) {
				return input2Freq;
			} else {
				return input2Count;
			}
		}

		@Override
		public Type[] getTypes() {
			return SUPPORTED_MEASURES;
		}

		@Override
		public String getLabel() {
			return input2Name;
		}
	}
}