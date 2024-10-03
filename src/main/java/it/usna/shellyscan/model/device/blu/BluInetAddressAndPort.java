package it.usna.shellyscan.model.device.blu;

import java.util.ArrayList;
import java.util.List;

import it.usna.shellyscan.model.device.InetAddressAndPort;

public class BluInetAddressAndPort extends InetAddressAndPort {
	private ArrayList<InetAddressAndPort> alternativeParents = new ArrayList<>();

	public BluInetAddressAndPort(InetAddressAndPort address) {
		super(address.getAddress(), address.getPort());
	}
	
	public void addAlternativeParent(AbstractBluDevice otherBlu) {
		alternativeParents.add(otherBlu.parent.getAddressAndPort());
		alternativeParents.addAll(((BluInetAddressAndPort)otherBlu.getAddressAndPort()).getAlternativeParents());
	}
	
	public void addAlternativeParent(InetAddressAndPort parent) {
		alternativeParents.add(parent);
	}

	public List<InetAddressAndPort> getAlternativeParents() {
		return alternativeParents;
	}
	
	@Override
	public String toString() {
		if(port == 80) {
			return address.getHostAddress() + " (" + (alternativeParents.size() + 1) + ")";
		} else {
			return address.getHostAddress() + ":" + port + " (" + (alternativeParents.size() + 1) + ")";
		}
	}
}
