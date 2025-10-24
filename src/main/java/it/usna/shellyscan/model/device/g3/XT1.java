package it.usna.shellyscan.model.device.g3;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

/**
 * XT1 PbS base model
 */
public class XT1 extends AbstractG3Device {
//	private static final Logger LOG = LoggerFactory.getLogger(AbstractG3Device.class);
	public static final String ID = "XT1";
	
	public XT1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "XT1";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		// @Override on subclasses if needed
	}
}

// model S3XT-0S (LinkedGo ST1820)
// Feature implemented as virtual components; e.g.:
// http://192.168.1.xxx/rpc/Number.GetConfig?id=202 - http://192.168.1.xxx/rpc/Number.GetStatus?id=202
// http://192.168.1.xxx/rpc/Shelly.GetComponents?keys=["boolean:202","number:200","number:201","number:202"]
// http://192.168.1.xxx/rpc/Number.Set?id=202&value=30