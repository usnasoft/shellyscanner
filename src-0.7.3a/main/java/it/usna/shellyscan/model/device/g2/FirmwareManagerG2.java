package it.usna.shellyscan.model.device.g2;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.FirmwareManager;

//https://shelly-api-docs.shelly.cloud/gen2/Overview/CommonServices/Shelly#shellyupdate
public class FirmwareManagerG2 implements FirmwareManager {

	private final AbstractG2Device d;
	private String current;
	private String stable;
	private String beta;
	private boolean updating;
	
	public FirmwareManagerG2(AbstractG2Device d) throws IOException {
		this.d = d;
		init();
	}

	private void init() throws IOException {
		JsonNode node = d.getJSON("/rpc/Shelly.GetDeviceInfo");
		current = node.get("fw_id").asText();
		node = d.getJSON("/rpc/Shelly.CheckForUpdate");
		stable = node.at("/stable/build_id").asText(null);
		beta = node.at("/beta/build_id").asText(null);
//		updating = STATUS_UPDATING.equals(node.get("status").asText());
	}

	public void chech() throws IOException {
		updating = false;
		init();
	}
	
	public String current() {
		return current;
	}
	
	public String newBeta() {
		return beta;
	}
	
	public String newStable() {
		return stable;
	}
	
	public String update(boolean stable) {
		updating = true;
		return d.postCommand("Shelly.Update", stable ? "{}" : "{\"stage\":\"beta\"}");
//		return d.sendCommand(stable ? "/rpc/Shelly.Update?stage=stable" : "/rpc/Shelly.Update?stage=beta");
	}
	
	public boolean upadating() {
		return updating;
	}
}