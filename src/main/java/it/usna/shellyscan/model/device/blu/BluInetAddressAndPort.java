package it.usna.shellyscan.model.device.blu;

import java.util.ArrayList;
import java.util.List;

import it.usna.shellyscan.model.device.InetAddressAndPort;

public class BluInetAddressAndPort extends InetAddressAndPort {
	private final ArrayList<InetAddressAndPort> alternativeParents = new ArrayList<>();
	private final int index; // the index identifying the device among the parent components

	public BluInetAddressAndPort(InetAddressAndPort address, int index) {
		super(address.getAddress(), address.getPort());
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void addAlternativeParent(AbstractBluDevice otherBlu) {	
		addAlternativeParent(otherBlu.parent.getAddressAndPort());
		((BluInetAddressAndPort)otherBlu.getAddressAndPort()).getAlternativeParents().forEach(a -> addAlternativeParent(a));
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
		byte[] i1 = address.getAddress();
		byte[] i2 = o2.getAddress().getAddress();
		int cmp;
		if(i1[0] != i2[0]) return (i1[0] & 0xFF) - (i2[0] & 0xFF);
		if(i1[1] != i2[1]) return (i1[1] & 0xFF) - (i2[1] & 0xFF);
		if(i1[2] != i2[2]) return (i1[2] & 0xFF) - (i2[2] & 0xFF);
		if((cmp = (i1[3] & 0xFF) - (i2[3] & 0xFF)) != 0) return cmp;
		if((cmp = port - o2.getPort()) != 0) return cmp;
		if(o2 instanceof BluInetAddressAndPort b) return index - b.index;
		return 1; // blue is greater than non blue
	}
	
	@Override
	public boolean equals(Object o2) {
		return o2 != null && getClass() == o2.getClass() && address.equals(((BluInetAddressAndPort)o2).address) && port == ((BluInetAddressAndPort)o2).port && index == ((BluInetAddressAndPort)o2).index;
	}
	
	@Override
	public String toString() {
		if(stringValue == null) {
			stringValue();
		}
		return stringValue + " (" + (alternativeParents.size() + 1) + ")";
//		if(port == 80) {
//			return address.getHostAddress() + " (" + (alternativeParents.size() + 1) + ")";
//		} else {
//			return address.getHostAddress() + ":" + port + " (" + (alternativeParents.size() + 1) + ")";
//		}
	}
}