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
import it.usna.shellyscan.model.device.g2.modules.Roller;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOnHolder;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RollerCommander;

public class ShellyPlus2PM extends AbstractG2Device implements RelayCommander, RollerCommander, InternalTmpHolder, SensorAddOnHolder {
	public final static String ID = "Plus2PM";
	private boolean modeRelay;
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay0, relay1;
	private Roller roller;
	private float internalTmp;
	private float power0, power1;
	private float voltage0, voltage1;
	private float current0, current1;
	private Meters meters0, meters1;
	private float pf0, pf1;
	private Meters[] meters;
	private SensorAddOn addOn;
	
	private final static String MSG_RESTORE_MODE_ERROR = "msgRestorePlus2PMMode";

	private final static String MODE_RELAY = "switch";

	public ShellyPlus2PM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
			addOn = new SensorAddOn(this);
		}
		
		meters0 = new Meters() {
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
		};
		meters1 = new Meters() {
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
		};
		
		// default init(...)
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}

	@Override
	public String getTypeName() {
		return "Shelly +2PM";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public int getRelayCount() {
		return modeRelay ? 2 : 0;
	}

	@Override
	public int getRollerCount() {
		return modeRelay ? 0 : 1;
	}

	@Override
	public Relay getRelay(int index) {
		return (index == 0) ? relay0 : relay1;
	}

	@Override
	public Relay[] getRelays() {
		return new Relay[] {relay0, relay1};
	}

	@Override
	public Roller getRoller(int index) {
		return roller;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	public float getPower() {
		return power0;
	}

	public float getVoltage() {
		return voltage0;
	}

	public float getCurrent() {
		return current0;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		modeRelay = configuration.get("sys").get("device").get("profile").asText().equals(MODE_RELAY);
		if(modeRelay) {
			if(relay0 == null /*|| relay1 == null*/) {
				relay0 = new Relay(this, 0);
				relay1 = new Relay(this, 1);
				meters = (addOn == null) ? new Meters[] {meters0, meters1} : new Meters[] {meters0, meters1, addOn};
				roller = null; // modeRelay change
			}
			relay0.fillSettings(configuration.get("switch:0"));
			relay1.fillSettings(configuration.get("switch:1"));
		} else {
			if(roller == null) {
				roller = new Roller(this, 0);
				meters = (addOn == null) ? new Meters[] {meters0} : new Meters[] {meters0, addOn};
				relay0 = relay1 = null; // modeRelay change
			}
			roller.fillSettings(configuration.get("cover:0"));
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(modeRelay) {
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

			internalTmp = (float)switchStatus0.path("temperature").path("tC").floatValue();
		} else {
			JsonNode cover = status.get("cover:0");
			power0 = cover.get("apower").floatValue();
			voltage0 = cover.get("voltage").floatValue();
			current0 = cover.get("current").floatValue();
			pf0 = cover.get("pf").floatValue();
			internalTmp = cover.path("temperature").path("tC").floatValue();
			roller.fillStatus(cover);
		}
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
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<Restore, String> res) throws IOException {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		boolean backModeRelay = MODE_RELAY.equals(devInfo.get("profile").asText());
		if(backModeRelay != modeRelay) {
			res.put(Restore.ERR_RESTORE_MSG, MSG_RESTORE_MODE_ERROR);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, ArrayList<String> errors) throws IOException, InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		final boolean backModeRelay = MODE_RELAY.equals(configuration.get("sys").get("device").get("profile").asText());
		errors.add(Input.restore(this,configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, "1"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		if(backModeRelay) {
			errors.add(relay0.restore(configuration));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(relay1.restore(configuration));
		} else {
			errors.add(roller.restore(configuration));
		}
		
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		SensorAddOn.restore(this, backupJsons, errors);
	}
	
	@Override
	public SensorAddOn getSensorAddOn() {
		return addOn;
	}

	@Override
	public String toString() {
		if(modeRelay) {
			return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
		} else {
			return super.toString() + " Cover: " + roller;
		}
	}
}