package it.usna.shellyscan.model.device.g3;

import java.net.InetAddress;

public class PbSXT1St1820 extends XT1 {
	public final static String MODEL = "S3XT-0S";

	public PbSXT1St1820(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "ST1820";
	}
}
