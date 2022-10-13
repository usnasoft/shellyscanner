package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.modules.Actions;
import it.usna.shellyscan.model.device.g1.modules.Actions.Input;
import it.usna.shellyscan.model.device.modules.ActionsCommander;

public class Button1 extends AbstractG1Device implements ActionsCommander {
	public final static String ID = "SHBTN-2";
	private Actions actions = new Actions(this);
	private int bat;
	
	public Button1(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		JsonNode status = getJSON("/status");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(status);
	}
	
	@Override
	public String getTypeName() {
		return "Button 1";
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		actions.fillSettings(settings.get("inputs"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		bat = status.get("bat").get("value").asInt();
	}
	
	public int getBattery() {
		return bat;
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "remain_awake", "led_status_disable")));
		final int longpushtime = settings.get("longpush_duration_ms").get("max").asInt();
		final int multipushtime = settings.get("multipush_time_between_pushes_ms").get("max").asInt();
		errors.add(sendCommand("/settings?multipush_time_between_pushes_ms_max=" + multipushtime + "&longpush_duration_ms_max=" + longpushtime + "&longpush_duration_ms_max=" + longpushtime));
		errors.add(sendCommand("/settings/input/0?name=" + settings.get("inputs").get(0).get("name").asText()));
	}
	
	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public Actions.Input getActionsGroup(int index) {
		return actions.getInput(0);
	}

	@Override
	public Input[] getActionsGroups() {
		return new  Input[] {actions.getInput(0)};
	}
}
