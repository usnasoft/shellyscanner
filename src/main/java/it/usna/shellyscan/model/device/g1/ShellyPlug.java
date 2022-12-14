package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.MetersPower;
import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyPlug extends AbstractG1Device implements RelayCommander {
	public final static String ID = "SHPLG-1";
	private Relay relay = new Relay(this, 0);
	private float power;
	private Meters[] meters;

	public ShellyPlug(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
		
		meters = new Meters[] {
				new MetersPower() {
					@Override
					public float getValue(Type t) {
						return power;
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Plug";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	public float getPower() {
		return power;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
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
		power = (float)status.get("meters").get(0).get("power").asDouble(0);
	}
	
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "wifirecovery_reboot_enabled")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(settings.get("relays").get(0)));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}