package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.MetersPower;
import it.usna.shellyscan.model.device.g1.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;

public class ShellyPlugS extends AbstractG1Device implements RelayCommander, InternalTmpHolder {
	public final static String ID = "SHPLG-S";
	private Relay relay = new Relay(this, 0);
	private Relay[] relayArray = new Relay[] {relay};
	private float internalTmp;
	private float power;
	private Meters[] meters;

	public ShellyPlugS(InetAddress address, int port, String hostname) {
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
		return "PlugS";
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
	public Relay getRelay(int index) {
		return relay;
	}
	
	@Override
	public Relay[] getRelays() {
		return relayArray;
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
		internalTmp = status.get("temperature").floatValue();
		power = status.get("meters").get(0).get("power").floatValue();
	}
	
	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable", "led_power_disable", "wifirecovery_reboot_enabled")));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(settings.get("relays").get(0)));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}