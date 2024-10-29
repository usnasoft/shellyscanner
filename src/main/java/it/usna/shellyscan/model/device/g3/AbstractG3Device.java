package it.usna.shellyscan.model.device.g3;

import java.net.InetAddress;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

/**
 * Base class for any gen3 Shelly device
 * @author usna
 */
public abstract class AbstractG3Device extends AbstractG2Device {
//	private final static Logger LOG = LoggerFactory.getLogger(AbstractG3Device.class);

	protected AbstractG3Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents", "/rpc/KNX.GetConfig"};
	}
}