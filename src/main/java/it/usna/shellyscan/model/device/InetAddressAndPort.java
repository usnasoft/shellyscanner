package it.usna.shellyscan.model.device;

import java.net.InetAddress;

/**
 * Address and port representation with Comparable<> implementation.<br>
 * This fits well on TableModel. Port 80 is not shown.<br>
 * InetAddress is assumed to be Inet4Address
 */
public class InetAddressAndPort implements Comparable<InetAddressAndPort> {
	private final InetAddress address;
	private final int port;

	public InetAddressAndPort(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
//	public InetAddressAndPort(ShellyAbstractDevice d) {
//		this.address = d.getAddress();
//		this.port = d.getPort();
//	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getIpAsText() {
		return address.getHostAddress();
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
	
	@Override
	public boolean equals(Object o2) {
		return o2 != null && getClass() == o2.getClass() && address.equals(((InetAddressAndPort)o2).address) && port == ((InetAddressAndPort)o2).port;
	}
	
//	public static String toString(ShellyAbstractDevice d) {
//		if(d.getPort() == 80) {
//			return d.getAddress().getHostAddress();
//		} else {
//			return d.getAddress().getHostAddress() + ":" + d.getPort();
//		}
//	}
	
	public String getRepresentation() {
		if(port == 80) {
			return address.getHostAddress();
		} else {
			return address.getHostAddress() + ":" + port;
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

//Note: InetAddress is not efficient (see java.net.Inet4Address implementation); an initial getAddress() and byte storage could improve performance