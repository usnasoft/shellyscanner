package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Shelly gateway G3 model
 * @author usna
 */
public class ShellyGatewayG3 extends AbstractG3Device {
//	private final static Logger LOG = LoggerFactory.getLogger(ShellyGatewayG3.class);
	public final static String ID = "BluGwG3";

	public ShellyGatewayG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();

		fillSettings(getJSON("/rpc/Shelly.GetConfig"));
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly BLU Gateway G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws IOException, InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		boolean ledOn = configuration.at("/blugw/sys_led_enable").booleanValue();
		postCommand("BluGw.SetConfig", "{\"config\":{\"sys_led_enable\":" + ledOn + "}}");
	}
}