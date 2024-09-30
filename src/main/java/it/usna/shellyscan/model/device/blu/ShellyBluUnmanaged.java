package it.usna.shellyscan.model.device.blu;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class ShellyBluUnmanaged extends AbstractBluDevice {
	private String type;

	public ShellyBluUnmanaged(ShellyAbstractDevice parent, JsonNode info, String localName, String index) throws IOException {
		super(parent, info, index);
		this.type = localName;
	}

	@Override
	public String getTypeID() {
		return type;
	}
	
	public String getTypeName() {
		return "Generic BTHome";
	}
	
	@Override
	public String toString() {
		return "BTHome (unmanaged) " + type + ": " + super.toString();
	}
}