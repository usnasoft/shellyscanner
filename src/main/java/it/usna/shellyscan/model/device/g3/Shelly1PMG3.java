package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.SensorAddOnHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.meters.MetersWVI;

/**
 * Shelly 1PM G3 model
 * @author usna
 */
public class Shelly1PMG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder, SensorAddOnHolder {
	private final static Logger LOG = LoggerFactory.getLogger(Shelly1PMG3.class);
	public final static String ID = "S1PMG3";
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private float power;
	private float voltage;
	private float current;
	private Relay[] relays = new Relay[] {relay};
	private Meters[] meters;
	private SensorAddOn addOn;

	public Shelly1PMG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		
		final JsonNode config = configure();
		
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	private JsonNode configure() throws IOException {
		Meters baseMeasures = new MetersWVI() {
			@Override
			public float getValue(Type t) {
				if(t == Meters.Type.W) {
					return power;
				} else if(t == Meters.Type.I) {
					return current;
				} else {
					return voltage;
				}
			}
		};
		
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
			addOn = new SensorAddOn(this);
			meters = (addOn.getTypes().length > 0) ? new Meters[] {baseMeasures, addOn} : new Meters[] {baseMeasures};
		} else {
			addOn = null;
			meters = new Meters[] {baseMeasures};
		}
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly 1PM G3";
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
	
	public float getPower() {
		return power;
	}
	
	public float getVoltage() {
		return voltage;
	}
	
	public float getCurrent() {
		return current;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		if(addOn != null) {
			addOn.fillSettings(configuration);
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get("input:0"));
		internalTmp = switchStatus.path("temperature").path("tC").floatValue();
		power = switchStatus.get("apower").floatValue();
		voltage = switchStatus.get("voltage").floatValue();
		current = switchStatus.get("current").floatValue();
		if(addOn != null) {
			addOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		final String[] cmd = super.getInfoRequests();
		return (addOn != null) ? SensorAddOn.getInfoRequests(cmd) : cmd;
	}
	
	@Override
	public SensorAddOn getSensorAddOn() {
		return addOn;
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		try {
			configure(); // maybe useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		} catch (IOException e) {
			LOG.error("restoreCheck", e);
		}
		SensorAddOn.restoreCheck(this, backupJsons, res);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		SensorAddOn.restore(this, backupJsons, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}