package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyPro1 extends AbstractG2Device implements RelayCommander, InternalTmpHolder {
	public final static String ID = "Pro1";
//	private final static JsonPointer SW_TEMP_P = JsonPointer.valueOf("/temperature/tC");
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private RelayInterface[] ralayes = new RelayInterface[] {relay};

	public ShellyPro1(InetAddress address, String hostname) {
		super(address, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Pro 1";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public Relay getRelay(int index) {
		return relay;
	}
	
	@Override
	public RelayInterface[] getRelays() {
		return ralayes;
	}
	
	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay.fillSettings(configuration.get("switch:0")/*, configuration.get("input:0")*/);
//		System.out.println("fill");
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus, status.get("input:0"));
		internalTmp = (float)switchStatus.path("temperature").path("tC").asDouble();
//		System.out.println("status");
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(Input.restore(this, configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay.restore(configuration));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}