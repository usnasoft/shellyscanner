package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.RollerInterface;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.shellyscan.model.device.modules.RollerCommander;

public class Shelly2 extends AbstractG1Device implements RelayCommander, RollerCommander {
	public final static String ID = "SHSW-21";
	private boolean modeRelay;
	private Relay relay0, relay1;
	private Roller roller;
	private float power;
	
	private final String MODE_RELAY = "relay";

	public Shelly2(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}

	@Override
	public String getTypeName() {
		return "Shelly 2";
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
	public RelayInterface getRelay(int index) {
		return index == 0 ? relay0 : relay1;
	}
	
	@Override
	public RelayInterface[] getRelays() {
		return new RelayInterface[] {relay0, relay1};
	}
	
	@Override
	public Roller getRoller(int index) {
		return roller;
	}
	
	public float getPower() {
		return power;
	}
	
	public boolean modeRelay() {
		return modeRelay;
	}

	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		modeRelay = MODE_RELAY.equals(settings.get("mode").asText());
		if(modeRelay) {
			JsonNode ralaysSetting = settings.get("relays");
			if(relay0 == null) {
				relay0 = new Relay(this, 0);
			}
			relay0.fillSettings(ralaysSetting.get(0));
			if(relay1 == null) {
				relay1 = new Relay(this, 1);
			}
			relay1.fillSettings(ralaysSetting.get(1));
			roller = null; // modeRelay change
		} else {
			if(roller == null) {
				roller = new Roller(this, 0);
			}
			relay0 = relay1 = null; // modeRelay change
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		final JsonNode meters = status.get("meters");
		power = (float)meters.get(0).get("power").asDouble(0);
		if(modeRelay) {
			JsonNode ralaysStatus = status.get("relays");
			relay0.fillStatus(ralaysStatus.get(0));
			relay1.fillStatus(ralaysStatus.get(1));
		} else {
			roller.fillStatus(status.get("rollers").get(0));
		}
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "longpush_time", "factory_reset_from_switch", "mode"/*, "max_power"*/)));
		final boolean backModeRelay = MODE_RELAY.equals(settings.get("mode").asText());
		if(backModeRelay) {
			Relay rel = new Relay(this, 0); // just for restore; object is later refreshed (fill called)
			errors.add(rel.restore(settings.get("relays").get(0)));
			rel = new Relay(this, 1);
			errors.add(rel.restore(settings.get("relays").get(1)));
		} else {
			final Roller roller = new Roller(this, 0); // just for restore; object is later refreshed (fill called)
			errors.add(roller.restore(settings.get("rollers").get(0)));
		}
	}

	@Override
	public String toString() {
		if(modeRelay) {
			return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
		} else {
			return super.toString() + " Roller: " + roller;
		}
	}
}