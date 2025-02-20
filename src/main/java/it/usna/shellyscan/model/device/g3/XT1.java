package it.usna.shellyscan.model.device.g3;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * XT1 PbS base model
 */
public class XT1 extends AbstractG3Device {
	public XT1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	public final static String ID = "XT1";

	@Override
	public String getTypeName() {
		return "XT1";
	}

	@Override
	public String getTypeID() {
		return ID;
	}


	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) /*throws IOException, InterruptedException*/ {
		// TODO Auto-generated method stub
	}
}

// Feature implemented as virtual components; e.g.:
// http://192.168.1.xxx/rpc/Number.GetConfig?id=202
// {"id":202,"name":"Target temperature","min":15,"max":35,"meta":{"ui":{"view":"slider","unit":"°C","step":0.5},"cloud":["log"]},"persisted":false,"default_value":25,"owner":"service:0","access":"crw"}
// http://192.168.1.xxx/rpc/Number.GetStatus?id=202
// {"value":21,"source":"sys","last_update_ts":1738833610}