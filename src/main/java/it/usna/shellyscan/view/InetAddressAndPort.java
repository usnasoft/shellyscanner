package it.usna.shellyscan.view;

import java.net.InetAddress;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class InetAddressAndPort implements Comparable<InetAddressAndPort> {
	private InetAddress address;
	private int port;

//	public InetAddressAndPort(InetAddress address, int port) {
//		this.address = address;
//		this.port = port;
//	}
	
	public InetAddressAndPort(ShellyAbstractDevice d) {
		this.address = d.getAddress();
		this.port = d.getPort();
	}

	@Override
	public int compareTo(InetAddressAndPort o2) {
		byte[] i1 = address.getAddress();
		byte[] i2 = o2.address.getAddress();
		int cmp;
		if(i1[0] != i2[0]) return (i1[0] & 0xFF) - (i2[0] & 0xFF);
		if(i1[1] != i2[1]) return (i1[1] & 0xFF) - (i2[1] & 0xFF);
		if(i1[2] != i2[2]) return (i1[2] & 0xFF) - (i2[2] & 0xFF);
		if((cmp = (i1[3] & 0xFF) - (i2[3] & 0xFF)) != 0) return cmp;
		return port - o2.port;
	}
	
	public static String toString(ShellyAbstractDevice d) {
		if(d.getPort() == 80) {
			return d.getAddress().getHostAddress();
		} else {
			return d.getAddress().getHostAddress() + ":" + d.getPort();
		}
	}
	
	@Override
	public String toString() {
		if(port == 80) {
			return address.getHostAddress();
		} else {
			return address.getHostAddress() + ":" + port;
		}
	}
}