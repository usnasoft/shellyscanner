package it.usna.shellyscan.model.device.g2;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreMsg;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

public abstract class AbstractProDevice extends AbstractG2Device {

	protected AbstractProDevice(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients", "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents", "/rpc/BLE.CloudRelay.ListInfos", "/rpc/KNX.GetConfig"};
	}
	
	@Override
	protected void restoreCommonConfig(JsonNode config, final long delay, Map<RestoreMsg, String> data, List<String> errors) throws InterruptedException {
		super.restoreCommonConfig(config, delay, data, errors);
		errors.add(ethRestore(config.get("eth")));
	}
	
	private String ethRestore(JsonNode eth) throws JacksonException, InterruptedException {
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		return postCommand("Eth.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(eth) + "}");
	}
}

/*
eth" : {
"enable" : true,
"ipv4mode" : "dhcp",
"ip" : null,
"netmask" : null,
"gw" : null,
"nameserver" : null
},
*/