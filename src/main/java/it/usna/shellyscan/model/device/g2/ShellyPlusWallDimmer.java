package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.modules.WhiteCommander;

public class ShellyPlusWallDimmer extends AbstractG2Device implements WhiteCommander {
	public final static String ID = "PlusWallDimmer";
	private LightWhite light = new LightWhite(this, 0);

	public ShellyPlusWallDimmer(InetAddress address, String hostname) {
		super(address, hostname);
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
		errors.add(Input.restore(this, configuration, "0"));
//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		errors.add(relay.restore(configuration));
	}

	@Override
	public LightWhite getWhite(int index) {
		return light;
	}

	@Override
	public LightWhite[] getWhites() {
		return new LightWhite[] {light};
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