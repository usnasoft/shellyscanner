package it.usna.shellyscan.model.device.blu;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyUnmanagedDeviceInterface;

public class ShellyBluUnmanaged extends AbstractBluDevice implements ShellyUnmanagedDeviceInterface {
	private String type;
	private Throwable ex;

	public ShellyBluUnmanaged(ShellyAbstractDevice parent, JsonNode info, String localName, String componentIndex) {
		super(parent, info, componentIndex);
		this.type = localName;
		this.hostname = localName + "-" + mac;
	}
	
	protected ShellyBluUnmanaged(ShellyAbstractDevice parent, JsonNode info, String localName, String index, Throwable ex) {
		this(parent, info, localName, index);
		this.ex = ex;
		status = Status.ERROR;
	}

	@Override
	public String getTypeID() {
		return type;
	}
	
	public String getTypeName() {
		return "Generic BTHome";
	}
	
	@Override
	public Throwable getException() {
		return ex;
	}
	
	@Override
	public String toString() {
		if(ex == null) {
			return "BTHome (unmanaged): " + type + ": " + super.toString();
		} else {
			return "BTHome (unmanaged): " + super.toString() + " Error: " + ex.getMessage();
		}
	}
}