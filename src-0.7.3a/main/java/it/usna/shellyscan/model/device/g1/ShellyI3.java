package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.modules.Actions;
import it.usna.shellyscan.model.device.g1.modules.Actions.Input;
import it.usna.shellyscan.model.device.modules.ActionsCommander;

public class ShellyI3 extends AbstractG1Device implements ActionsCommander {
	public final static String ID = "SHIX3-1";
	private Actions actions = new Actions(this);
	
	public ShellyI3(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly I3";
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		actions.fillSettings(settings.get("inputs"));
	}
	
//	@Override
//	protected void fillStatus(JsonNode status) throws IOException {
//		super.fillStatus(status);
//	}
	
	// fillStatus - super

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable")));
		final int longpushtimemax = settings.get("longpush_duration_ms").get("max").asInt();
		final int longpushtimemin = settings.get("longpush_duration_ms").get("min").asInt();
		final int multipushtime = settings.get("multipush_time_between_pushes_ms").get("max").asInt();
		errors.add(sendCommand("/settings?multipush_time_between_pushes_ms_max=" + multipushtime + "&longpush_duration_ms_max=" + longpushtimemax + "&longpush_duration_ms_min=" + longpushtimemin));
		for(int i = 0; i < 3; i++) {
			JsonNode inp = settings.get("inputs").get(i);
			errors.add(sendCommand("/settings/input/" + i + "?" + jsonNodeToURLPar(inp, "name", "btn_type", "btn_reverse")));
		}
	}
	
	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public Actions.Input getActionsGroup(int index) {
		return actions.getInput(index);
	}
	
	@Override
	public Input[] getActionsGroups() {
		return new  Input[] {actions.getInput(0), actions.getInput(1), actions.getInput(2)};
	}
}