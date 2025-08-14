package it.usna.shellyscan.model.device.g4;

import java.net.InetAddress;

import it.usna.shellyscan.model.device.g3.AbstractG3Device;

/**
 * Base class for any gen4 Shelly device
 * @author usna
 */
public abstract class AbstractG4Device extends AbstractG3Device {
//	private static final Logger LOG = LoggerFactory.getLogger(AbstractG4Device.class);
	protected boolean zigbee;

	protected AbstractG4Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients", "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents", "/rpc/BLE.CloudRelay.ListInfos", /*"/rpc/KNX.GetConfig",*/
				/*"/rpc/Matter.GetConfig", "/rpc/Matter.GetStatus",*/ "/rpc/Matter.GetSetupCode"};
	}
}