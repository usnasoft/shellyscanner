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

public class ShellyPlugS extends AbstractG1Device implements RelayCommander, InternalTmpHolder {
	public final static String ID = "SHPLG-S";
	private Relay relay = new Relay(this, 0);
	private float internalTmp;

	public ShellyPlugS(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}

	@Override
	public String getTypeName() {
		return "PlugS";
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
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		relay.fillSettings(settings.get("relays").get(0));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay.fillStatus(status.get("relays").get(0));
//		internalTmp = (float)status.get("tmp").get("tC").asDouble();
		internalTmp = (float)status.get("temperature").asDouble();
	}
	
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "led_power_disable")));
		errors.add(relay.restore(settings.get("relays").get(0)));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}