package it.usna.shellyscan.model.device.blu;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.usna.shellyscan.model.device.InetAddressAndPort;

public class BluInetAddressAndPort extends InetAddressAndPort {
	private final ArrayList<InetAddressAndPort> alternativeParents = new ArrayList<>();
	private final int index; // the index identifying the device among the parent components

	public BluInetAddressAndPort(InetAddressAndPort addressAndPort, int index) {
		super(addressAndPort);
		this.index = index;
	}
	
	public BluInetAddressAndPort(InetAddress address, int port, int index) {
		super(address, port);
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void addAlternativeParent(AbstractBTHomeDevice otherBlu) {	
		addAlternativeParent(otherBlu.parent.getAddressAndPort());
		((BluInetAddressAndPort)otherBlu.getAddressAndPort()).getAlternativeParents().forEach(this::addAlternativeParent);
	}
	
	public void addAlternativeParent(InetAddressAndPort parent) {
		if(alternativeParents.contains(parent) == false && this.equivalent(parent) == false) {
			alternativeParents.add(parent);
		}
	}

	public List<InetAddressAndPort> getAlternativeParents() {
		return alternativeParents;
	}
	
	@Override
	public int compareTo(InetAddressAndPort o2) {
//		byte[] i1 = address.getAddress();
//		byte[] i2 = o2.getAddress().getAddress();
//		int cmp;
//		if(i1[0] != i2[0]) return (i1[0] & 0xFF) - (i2[0] & 0xFF);
//		if(i1[1] != i2[1]) return (i1[1] & 0xFF) - (i2[1] & 0xFF);
//		if(i1[2] != i2[2]) return (i1[2] & 0xFF) - (i2[2] & 0xFF);
//		if((cmp = (i1[3] & 0xFF) - (i2[3] & 0xFF)) != 0) return cmp;
//		if((cmp = port - o2.getPort()) != 0) return cmp;
		
		if(sortValue != o2.getSortValue()) {
			return (sortValue > o2.getSortValue()) ? 1 : -1;
		}
		if(o2 instanceof BluInetAddressAndPort b) {
			return index - b.index;
		}
		return 1; // blue is greater than non blue
		
	}
	
	public String getParentsAsString() {
		if(alternativeParents.size() > 0) {
			return getRepresentation() + alternativeParents.stream().map(InetAddressAndPort::getRepresentation).collect(Collectors.joining(" / ", " / ", ""));
		} else {
			return getRepresentation() + " (1)";
		}
	}
	
	@Override
	public boolean equals(Object o2) {
		return o2 != null && BluInetAddressAndPort.class == o2.getClass() && sortValue == ((BluInetAddressAndPort)o2).sortValue && index == ((BluInetAddressAndPort)o2).index;
//		return o2 != null && getClass() == o2.getClass() && address.equals(((BluInetAddressAndPort)o2).address) && port == ((BluInetAddressAndPort)o2).port && index == ((BluInetAddressAndPort)o2).index;
	}
	
	@Override
	public String toString() {
		if(stringValue == null) {
			stringValue();
		}
		return stringValue + " (" + (alternativeParents.size() + 1) + ")";
	}
}