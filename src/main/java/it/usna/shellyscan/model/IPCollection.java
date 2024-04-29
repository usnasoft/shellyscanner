package it.usna.shellyscan.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class IPCollection implements Iterable<InetAddress> {
	private ArrayList<IPRange> collection = new ArrayList<>();
	
	public void add(final byte[] ip, int first, final int last) {
		collection.add(new IPRange(ip, first, last));
	}
	
	public void add(final String ip, int first, final int last) {
		String ipS[] = ip.split("\\.");
		byte [] baseIP = new byte[] {(byte)Integer.parseInt(ipS[0]), (byte)Integer.parseInt(ipS[1]), (byte)Integer.parseInt(ipS[2]), 0};
		collection.add(new IPRange(baseIP, first, last));
	}

	@Override
	public Iterator<InetAddress> iterator() {
		return new Iterator<InetAddress>() {
			private int rangeIndex = 0;
			private int currentOctet = -1;
			
			@Override
			public boolean hasNext() {
				return rangeIndex < collection.size() - 1 || (rangeIndex < collection.size() && currentOctet <= collection.get(rangeIndex).last);
			}

			@Override
			public InetAddress next() {
				try {
					IPRange ipr = collection.get(rangeIndex);
					if(currentOctet < 0) {
						currentOctet = ipr.first;
					} else if(currentOctet > ipr.last) {
						ipr = collection.get(++rangeIndex);
						currentOctet = ipr.first;
					}
					byte[] baseScanIP = ipr.ip;
					baseScanIP[3] = (byte)currentOctet++;

					return InetAddress.getByAddress(baseScanIP);
				} catch (UnknownHostException | RuntimeException e) {
					throw new NoSuchElementException(e);
				}
			}
		};
	}
	
	@Override
	public String toString() {
		return collection.stream().map(IPRange::toString).collect(Collectors.joining(", "));
	}
	
	private record IPRange(byte[] ip, int first, int last) {
		@Override
		public String toString() {
			return (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "/" + first + "-" + last;
		}
	}
	
	public static void main(String ...strings) {
		IPCollection c = new IPCollection();
		c.add(new byte[] {(byte)192, (byte)168, (byte)1, (byte)0}, 10, 20);
		c.add(new byte[] {(byte)192, (byte)168, (byte)2, (byte)0}, 15, 18);
		c.add("192.168.3", 30, 31);
		System.out.println(c);
		for(InetAddress a: c) {
			System.out.println(a);
		}
	}
}