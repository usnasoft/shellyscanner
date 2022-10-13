package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyUNI extends AbstractG1Device implements RelayCommander {
	public final static String ID = "SHUNI-1";
	private boolean modeRelay;
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);

	public ShellyUNI(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}

	@Override
	public String getTypeName() {
		return "Shelly UNI";
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
	
	public boolean modeRelay() {
		return modeRelay;
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
		relay0.fillStatus(ralaysStatus.get(0));
		relay1.fillStatus(ralaysStatus.get(1));
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "longpush_time", "factory_reset_from_switch" /*"mode"/**/)));
		errors.add(relay0.restore(settings.get("relays").get(0)));
		errors.add(relay1.restore(settings.get("relays").get(1)));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
	}
}