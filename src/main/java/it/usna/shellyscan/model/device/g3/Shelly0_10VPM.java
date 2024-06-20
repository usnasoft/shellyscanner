package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.WhiteCommander;

public class Shelly0_10VPM extends AbstractG3Device implements WhiteCommander {
	public final static String ID = "Dimmer0110VPMG3";
	private LightWhite light = new LightWhite(this, 0);
	private LightWhite[] lightArray = new LightWhite[] {light};

	public Shelly0_10VPM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Dimmer 0/1-10 G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		light.fillSettings(configuration.get("light:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		light.fillStatus(status.get("light:0"), status.get("input:0"));
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(light.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "1"));
	}

	@Override
	public LightWhite getWhite(int index) {
		return light;
	}

	@Override
	public LightWhite[] getWhites() {
		return lightArray;
	}

	@Override
	public int getWhiteCount() {
		return 1;
	}

	@Override
	public String toString() {
		return super.toString() + " Light: " + light;
	}
}