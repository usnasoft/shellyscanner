package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

/**
 * Iterate over this kind of Json structure:<br>
 * <code>
 * {
 *  "items" : [ {"key" : "key", "etag" : "xxxyyy", "value" : "{}"} ],
 *  "offset" : 0, "total" : 1
 * }
 * </code><br>
 * calling "method" with the proper offset when needed
 */
public class PageIterator implements Iterator<JsonNode> {
	private final ShellyAbstractDevice device;
	private final String method;
	private final String arrayKey;
	private Iterator<JsonNode> current;
	private final int numNodes;
	private int nextIdx = 0;
	
	public PageIterator(ShellyAbstractDevice device, final String method, final String arrayKey) throws IOException {
		this.device = device;
		this.method = method;
		this.arrayKey = arrayKey;
		JsonNode resp = device.getJSON(method);
		this.numNodes = resp.path("total").intValue();
		this.current = resp.path(arrayKey).iterator();
	}

	@Override
	public boolean hasNext() {
		if(current.hasNext()) {
			return true;
		} else if(nextIdx < numNodes) {
			try {
				this.current = device.getJSON(method + ((method.contains("?")) ? "&offset=" : "?offset=") + nextIdx).path(arrayKey).iterator();
				return current.hasNext();
			} catch (IOException e) {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	/**
	 * this partially violates the Iterator contract: if hasNext is not called before
	 * next() do not try to load next page and a NoSuchElementException could be thrown.
	 * I'll never call next() without hasNext() so this is more efficient
	 */
	public JsonNode next() {
		nextIdx++;
		return current.next();
	}
}