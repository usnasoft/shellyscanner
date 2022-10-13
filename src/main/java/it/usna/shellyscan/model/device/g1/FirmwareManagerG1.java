package it.usna.shellyscan.model.device.g1;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.FirmwareManager;

public class FirmwareManagerG1 implements FirmwareManager {
	private final static String STATUS_UPDATING = "updating";

	private final AbstractG1Device d;
	private String current;
	private String stable;
	private String beta;
	private boolean updating;
	
	public FirmwareManagerG1(AbstractG1Device d) throws IOException {
		this.d = d;
		init();
	}
	
	private void init() throws IOException {
		JsonNode node = d.getJSON("/ota");
		updating = STATUS_UPDATING.equals(node.get("status").asText());
		current = node.get("old_version").asText();
		stable = node.get("has_update").asBoolean() ? node.get("new_version").asText() : null;
		boolean hasBeta = node.has("beta_version") && node.get("beta_version").asText().equals(current) == false;
		beta = hasBeta ? node.get("beta_version").asText() : null;
	}

	public void chech() throws IOException {
		d.sendCommand("/ota/check");
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
		return d.sendCommand(stable ? "/ota?update=true" : "/ota?beta=true");
	}
	
	public boolean upadating() {
		return updating;
	}
}