package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.meters.MetersWVI;
import it.usna.shellyscan.model.device.g2.modules.Relay;

public class ShellyPlugSG3 extends AbstractG3Device implements ModulesHolder, InternalTmpHolder {
	public final static String ID = "PlugSG3";
	private Relay relay = new Relay(this, 0);
	private float internalTmp;
	private MetersWVI meters = new MetersWVI();
	private Meters[] metersArray = new Meters[] {meters};

	public ShellyPlugSG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Plug S G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public Relay[] getModules() {
		return new Relay[] {relay};
	}
	
	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	@Override
	public Meters[] getMeters() {
		return metersArray;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay.fillSettings(configuration.get("switch:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus);
		internalTmp = switchStatus.path("temperature").path("tC").floatValue();
		meters.fill(switchStatus);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		JsonNode ui = configuration.get("plugs_ui").deepCopy();
		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.set("config", ui);
		errors.add(postCommand("PLUGS_UI.SetConfig", out));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		
		errors.add(relay.restore(configuration));
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}