package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.meters.MetersWVI;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plug PM Gen3 model (partial)
 */
public class ShellyPlugPMG3 extends AbstractG3Device /*implements InternalTmpHolder*/ {
	public static final String ID = "PlugPMG3";
	public static final String MODEL = "S3PL-30116EU";
//	private float internalTmp;
	private MetersWVI meters = new MetersWVI();
	private Meters[] metersArray = new Meters[] {meters};

	public ShellyPlugPMG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String getTypeName() {
		return "Plug PM G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getModelID() {
		return MODEL;
	}
	
//	@Override
//	public float getInternalTmp() {
//		return internalTmp;
//	}
	
	@Override
	public Meters[] getMeters() {
		return metersArray;
	}
	
//	@Override
//	protected void fillSettings(JsonNode configuration) throws IOException {
//		super.fillSettings(configuration);
//		relay.fillSettings(configuration.get("switch:0"));
//	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		meters.fill(status.get("pm1:0"));
//		freq = pm1.get("freq").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		JsonNode ui = configuration.path("plugpm_ui");
		if(ui.isMissingNode() == false) {
			ObjectNode out = JsonNodeFactory.instance.objectNode();
			out.set("config", ui);
			errors.add(postCommand("PLUGPM_UI.SetConfig", out));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		}
	}
	
//	@Override
//	public String toString() {
//		return super.toString() + " Relay: " + relay;
//	}
}