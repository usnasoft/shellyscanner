package it.usna.shellyscan.view.util;

import java.net.InetAddress;
import java.util.Comparator;

public class IPv4Comparator implements Comparator<InetAddress> {

	@Override
	public int compare(InetAddress o1, InetAddress o2) {
		byte[] i1 = o1.getAddress();
		byte[] i2 = o2.getAddress();
		if(i1[0] != i2[0]) return (i1[0] & 0xFF) - (i2[0] & 0xFF);
		if(i1[1] != i2[1]) return (i1[1] & 0xFF) - (i2[1] & 0xFF);
		if(i1[2] != i2[2]) return (i1[2] & 0xFF) - (i2[2] & 0xFF);
		return (i1[3] & 0xFF) - (i2[3] & 0xFF);
	}
}