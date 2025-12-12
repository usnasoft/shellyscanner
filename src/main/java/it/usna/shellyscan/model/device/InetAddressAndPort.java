package it.usna.shellyscan.model.device;

import java.net.InetAddress;

/**
 * Address and port representation with Comparable<> implementation.<br>
 * This fits well on TableModel. Port 80 is not shown.<br>
 * InetAddress is assumed to be Inet4Address
 */
public class InetAddressAndPort implements Comparable<InetAddressAndPort> {
	protected final InetAddress address;
	protected final int port;
	protected String stringValue = null;
	protected final long sortValue;
	
	public InetAddressAndPort(InetAddress address, int port) {
		this.address = address;
		this.port = port;
		byte[] b = address.getAddress();
		sortValue = (long)(b[0] & 0xFF)<<40 | (long)(b[1] & 0xFF)<<32 | (long)(b[2] & 0xFF)<<24 | (long)(b[3] & 0xFF)<<16 | port;
	}
	
	public InetAddressAndPort(InetAddressAndPort addressAndPort) {
		this.address = addressAndPort.getAddress();
		this.port = addressAndPort.getPort();
		sortValue = addressAndPort.sortValue;
		stringValue = addressAndPort.stringValue;
	}
	
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
//		byte[] i1 = address.getAddress();
//		byte[] i2 = o2.address.getAddress();
//		int cmp;
//		if(i1[0] != i2[0]) return (i1[0] & 0xFF) - (i2[0] & 0xFF);
//		if(i1[1] != i2[1]) return (i1[1] & 0xFF) - (i2[1] & 0xFF);
//		if(i1[2] != i2[2]) return (i1[2] & 0xFF) - (i2[2] & 0xFF);
//		if((cmp = (i1[3] & 0xFF) - (i2[3] & 0xFF)) != 0) return cmp;
//		if((cmp = port - o2.getPort()) != 0) return cmp;
		if(sortValue != o2.sortValue) {
			return (sortValue > o2.sortValue) ? 1 : -1;
		}
		return (o2.getClass() == InetAddressAndPort.class) ? 0 : -1; // InetAddressAndPort before derived classes
	}
	
	public long getSortValue() {
		return sortValue;
	}
	
	@Override
	public boolean equals(Object o2) {
		return o2 != null && InetAddressAndPort.class == o2.getClass() && sortValue == ((InetAddressAndPort)o2).sortValue;
		// address.equals(((InetAddressAndPort)o2).address) && port == ((InetAddressAndPort)o2).port
	}
	
	// valid for subclasses
	public boolean equivalent(InetAddressAndPort o2) {
		return address.equals(o2.address) && port == o2.port;
	}
	
	//see java.net.Inet4Address.getHostAddress() implementation to undestand ...
	protected void stringValue() {
		if(port == 80) {
			stringValue = address.getHostAddress();
		} else {
			stringValue = address.getHostAddress() + ":" + port;
		}
	}
	
	public String getRepresentation() {
		if(stringValue == null) {
			stringValue();
		}
		return stringValue;
	}
	
	@Override
	public String toString() {
		if(stringValue == null) {
			stringValue();
		}
		return stringValue;
	}
}