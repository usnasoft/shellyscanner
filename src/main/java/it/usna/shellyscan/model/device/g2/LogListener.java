package it.usna.shellyscan.model.device.g2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogListener {
	private final static Logger LOG = LoggerFactory.getLogger(LogListener.class);

	public void accept(String txt) {
		LOG.trace("LogListener: {}", txt);
	}
	
	public boolean requestNext() {
		return true;
	}
}
