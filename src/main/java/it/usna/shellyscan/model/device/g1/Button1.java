package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g1.modules.Actions;
import it.usna.shellyscan.model.device.modules.InputCommander;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class Button1 extends AbstractBatteryG1Device implements InputCommander {
	public final static String ID = "SHBTN-2";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT};
	private Actions actions = new Actions(this);
	private Meters[] meters;
	
	public Button1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						return bat;
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Button 1";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		this.settings = settings;
		this.settingsActions = actions.fillSettings(settings.get("inputs"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.status = status;
		bat = status.get("bat").get("value").intValue();
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException, InterruptedException {
		final int longpushtime = settings.get("longpush_duration_ms").get("max").asInt();
		final int multipushtime = settings.get("multipush_time_between_pushes_ms").get("max").asInt();
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "remain_awake", "led_status_disable") +
				"&multipush_time_between_pushes_ms_max=" + multipushtime + "&longpush_duration_ms_max=" + longpushtime));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(sendCommand("/settings/input/0?name=" + settings.get("inputs").get(0).get("name").asText()));
	}
	
	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public InputInterface getActionsGroup(int index) {
		return actions.getInput(0);
	}

	@Override
	public InputInterface[] getActionsGroups() {
		return new  InputInterface[] {actions.getInput(0)};
	}
}