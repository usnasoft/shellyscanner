package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class Shelly3EM extends AbstractG1Device implements RelayCommander {
	public final static String ID = "SHEM-3";
	private Relay relay = new Relay(this, 0);

	public Shelly3EM(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly 3EM";
	}
	
	@Override
	public RelayInterface getRelay(int index) {
		return relay;
	}
	
	@Override
	public RelayInterface[] getRelays() {
		return new RelayInterface[] {relay};
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		relay.fillSettings(settings.get("relays").get(0));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay.fillStatus(status.get("relays").get(0));
//		JsonNode eMeters = status.get("emeters").get(0);
//		eMeters.get("power").asDouble();
//		eMeters.get("reactive").asDouble();
//		eMeters.get("voltage").asDouble();
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable")));
		JsonNode meters = settings.get("emeters");
		errors.add(sendCommand("/settings/emeters/0?" + jsonNodeToURLPar(meters.get(0), "appliance_type", "max_power")));
		errors.add(sendCommand("/settings/emeters/1?" + jsonNodeToURLPar(meters.get(1), "appliance_type", "max_power")));
		errors.add(sendCommand("/settings/emeters/2?" + jsonNodeToURLPar(meters.get(2), "appliance_type", "max_power")));
		errors.add(relay.restore(settings.get("relays").get(0)));
	}
}