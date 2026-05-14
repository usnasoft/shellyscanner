package it.usna.shellyscan.model.device.blu;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public record BLEGateway(AbstractG2Device gw, long lastSeen) implements Comparable<BLEGateway> {
	@Override
	public int compareTo(BLEGateway o) {
		if(this.gw.equals(o.gw)) {
			return 0;
		} else if(this.lastSeen == o.lastSeen) {
			return this.gw.getAddressAndPort().compareTo(o.gw.getAddressAndPort());
		} else {
			return (this.lastSeen > o.lastSeen) ? 1 : -1;
		}
	}

	@Override
	public boolean equals(Object obj) {
		return gw.equals(((BLEGateway)obj).gw);
	}

	@Override
	public int hashCode() {
		return gw.hashCode();
	}
	
	public String toString() {
		return gw.getAddressAndPort().getRepresentation();
	}
}
