package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class Shelly1L extends AbstractG1Device implements RelayCommander, InternalTmpHolder {
	public final static String ID = "SHSW-L";
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private float power;

	public Shelly1L(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly 1L";
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
	public float getInternalTmp() {
		return internalTmp;
	}
	
	public float getPower() {
		return power;
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
		internalTmp = (float)status.get("temperature").asDouble();
		power = (float)status.get("meters").get(0).get("power").asDouble(0);
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "longpush_time", "factory_reset_from_switch",
				"max_power", "supply_voltage", /*"power_correction",*/ "led_status_disable")));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}