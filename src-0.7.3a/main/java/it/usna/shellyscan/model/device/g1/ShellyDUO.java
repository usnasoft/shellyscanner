package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.WhiteCommander;

public class ShellyDUO extends AbstractG1Device implements WhiteCommander {
	public final static String ID = "SHBDUO-1";
	private LightWhite light = new LightWhite(this, "/light/", 0);

	public ShellyDUO(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}
	
	@Override
	public String getTypeName() {
		return "DUO";
	}
	
	@Override
	public int getWhiteCount() {
		return 1;
	}
	
	@Override
	public LightWhite getWhite(int index) {
		return light;
	}
	
	@Override
	public LightWhite[] getWhites() {
		return new LightWhite[] {light};
	}
	
//	@Override
//	public void statusRefresh() throws IOException {
//		light.refresh();
//	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		light.fillSettings(settings.get("lights").get(0));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		light.fillStatus(status.get("lights").get(0));
	}

//	@Override
//	public String[] getInfoRequests() {
//		return new String[] {"shelly", "settings", "settings/actions", "light/0", "status", "ota"};
//	}

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "transition")));
		JsonNode nightMode = settings.get("night_mode");
		if(nightMode.get("enabled").asBoolean()) {
			errors.add(sendCommand("/settings/night_mode?" + jsonNodeToURLPar(nightMode, "enabled", "start_time", "end_time", "brightness")));
		} else {
			errors.add(sendCommand("/settings/night_mode?enabled=false"));
		}
		errors.add(light.restore(settings.get("lights").get(0)));
	}

	@Override
	public String toString() {
		return super.toString() + " Load: " + light;
	}
}