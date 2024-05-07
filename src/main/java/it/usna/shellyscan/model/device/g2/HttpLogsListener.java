package it.usna.shellyscan.model.device.g2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * NOT USED
 */
public interface HttpLogsListener {
	final static Logger LOG = LoggerFactory.getLogger(HttpLogsListener.class);

	default void accept(String txt) {
		LOG.trace("LogListener: {}", txt);
	}
	
	default void error(String txt) {
		LOG.error("LogListener error: {}", txt);
	}
	
	default void closed() {
		LOG.debug("LogListener closed");
	}
	
	public boolean requestNext();
}
