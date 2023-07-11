package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOnHolder;
import it.usna.shellyscan.model.device.modules.RelayCommander;

public class ShellyPlus1PM extends AbstractG2Device implements RelayCommander, InternalTmpHolder, SensorAddOnHolder {
	public final static String ID = "Plus1PM";
//	private final static JsonPointer SW_TEMP_P = JsonPointer.valueOf("/temperature/tC");
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private float power;
	private float voltage;
	private float current;
	private float pf;
	private Relay[] relays = new Relay[] {relay};
	private Meters[] meters;
	private SensorAddOn addOn;

	public ShellyPlus1PM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		Meters m0 = new Meters() {
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
		};
		
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
			addOn = new SensorAddOn(this);
			meters = new Meters[] {m0, addOn};
		} else {
			meters = new Meters[] {m0};
		}
		
		// default init(...)
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly +1PM";
	}
	
	@Override
	public Relay getRelay(int index) {
		return relay;
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public Relay[] getRelays() {
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
		relay.fillSettings(configuration.get("switch:0"));
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
		pf = switchStatus.get("pf").floatValue();
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
	protected void restore(Map<String, JsonNode> backupJsons, ArrayList<String> errors) throws IOException, InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		SensorAddOn.restore(this, backupJsons, errors);
	}
	
	@Override
	public SensorAddOn getSensorAddOn() {
		return addOn;
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}