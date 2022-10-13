package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.modules.LightBulbRGB;
import it.usna.shellyscan.model.device.g1.modules.LightBulbRGBCommander;

public class ShellyBulb extends AbstractG1Device implements LightBulbRGBCommander {
	public final static String ID = "SHBLB-1";
	private LightBulbRGB light = new LightBulbRGB(this, 0);
	
	public ShellyBulb(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		JsonNode settings = getJSON("/settings");
		fillOnce(settings);
		fillSettings(settings);
		fillStatus(getJSON("/status"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Bulb";
	}
	
	@Override
	public int getLightCount() {
		return 1;
	}
	
	@Override
	public LightBulbRGB getLight(int index) {
		return light;
	}

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

	@Override
	protected void restore(JsonNode settings, ArrayList<String> errors) throws IOException {
		errors.add(sendCommand("/settings?" + jsonNodeToURLPar(settings, "mode")));
		errors.add(light.restore(settings.get("lights").get(0)));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Light " + light;
	}
}