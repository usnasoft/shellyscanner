package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.modules.Input;

public class ShellyPlusWallDimmer extends AbstractG2Device {
	public final static String ID = "PlusWallDimmer";

	public ShellyPlusWallDimmer(InetAddress address, String hostname) {
		super(address, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Wall Dimmer";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
//	@Override
//	public Relay getRelay(int index) {
//		return relay;
//	}
//	
//	@Override
//	public RelayInterface[] getRelays() {
//		return ralayes;
//	}
	
//	@Override
//	protected void fillSettings(JsonNode configuration) throws IOException {
//		super.fillSettings(configuration);
//		relay.fillSettings(configuration.get("switch:0")/*, configuration.get("input:0")*/);
////		System.out.println("fill");
//	}
	
//	@Override
//	protected void fillStatus(JsonNode status) throws IOException {
//		super.fillStatus(status);
////		JsonNode switchStatus = status.get("switch:0");
////		relay.fillStatus(switchStatus, status.get("input:0"));
//		internalTmp = (float)switchStatus.path("temperature").path("tC").asDouble();
//	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(Input.restore(this, configuration, "0"));
//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		errors.add(relay.restore(configuration));
	}
	
//	@Override
//	public String toString() {
//		return super.toString() + " Relay: " + relay;
//	}
}