package it.usna.shellyscan.model.device.g3;

import java.net.InetAddress;

import it.usna.shellyscan.model.device.g2.AbstractBatteryG2Device;

public abstract class AbstractBatteryG3Device extends AbstractBatteryG2Device {

	protected AbstractBatteryG3Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	// todo: should extend AbstractG3Device
}