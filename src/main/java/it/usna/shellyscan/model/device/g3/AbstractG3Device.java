package it.usna.shellyscan.model.device.g3;

import java.net.InetAddress;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

/**
 * Base class for any gen3 Shelly device
 * usna
 */
public abstract class AbstractG3Device extends AbstractG2Device {

	protected AbstractG3Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	// todo
}