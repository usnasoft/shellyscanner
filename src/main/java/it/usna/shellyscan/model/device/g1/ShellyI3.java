package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g1.modules.Actions;
import it.usna.shellyscan.model.device.modules.InputCommander;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class ShellyI3 extends AbstractG1Device implements InputCommander {
	public final static String ID = "SHIX3-1";
	private Actions actions = new Actions(this);
	
	public ShellyI3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly I3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		actions.fillSettings(settings.get("inputs"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		actions.fillStatus(status.get("inputs"));
	}

	@Override
	protected void restore(JsonNode settings, List<String> errors) throws IOException, InterruptedException {
		final int longpushtimemax = settings.get("longpush_duration_ms").get("max").asInt();
		final int longpushtimemin = settings.get("longpush_duration_ms").get("min").asInt();
		final int multipushtime = settings.get("multipush_time_between_pushes_ms").get("max").asInt();
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "led_status_disable") +
				"&multipush_time_between_pushes_ms_max=" + multipushtime + "&longpush_duration_ms_max=" + longpushtimemax + "&longpush_duration_ms_min=" + longpushtimemin));
		for(int i = 0; i < 3; i++) {
			JsonNode inp = settings.get("inputs").get(i);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(sendCommand("/settings/input/" + i + "?" + jsonNodeToURLPar(inp, "name", "btn_type", "btn_reverse")));
		}
	}

	@Override
	public InputInterface getActionsGroup(int index) {
		return actions.getInput(index);
	}
	
	@Override
	public InputInterface[] getActionsGroups() {
		return new InputInterface[] {actions.getInput(0), actions.getInput(1), actions.getInput(2)};
	}

	@Override
	public String toString() {
		return super.toString();
	}
}