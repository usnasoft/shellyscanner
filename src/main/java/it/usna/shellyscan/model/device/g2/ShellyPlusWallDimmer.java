package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.WhiteCommander;

public class ShellyPlusWallDimmer extends AbstractG2Device implements WhiteCommander {
	public final static String ID = "PlusWallDimmer";
	private LightWhite light = new LightWhite(this, 0);
	private LightWhite[] lightArray = new LightWhite[] {light};

	public ShellyPlusWallDimmer(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Wall Dimmer";
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
		light.fillStatus(status.get("light:0"));
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(light.restore(configuration));
		errors.add(postCommand("WD_UI.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("wd_ui")) + "}"));
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
	
//	@Override
//	public String toString() {
//		return super.toString() + " Relay: " + relay;
//	}
}