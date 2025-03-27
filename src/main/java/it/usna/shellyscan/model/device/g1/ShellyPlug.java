package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g1.meters.MetersPower;
import it.usna.shellyscan.model.device.g1.modules.Relay;

public class ShellyPlug extends AbstractG1Device implements ModulesHolder {
	public final static String ID = "SHPLG-1";
	private Relay relay = new Relay(this, 0);
	private Relay[] relayArray = new Relay[] {relay};
	private float power;
	private Meters[] meters;

	public ShellyPlug(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
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
	public Relay[] getModules() {
		return relayArray;
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
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "wifirecovery_reboot_enabled")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(settings.get("relays").get(0)));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}