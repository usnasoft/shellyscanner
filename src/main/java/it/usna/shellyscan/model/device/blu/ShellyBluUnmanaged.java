package it.usna.shellyscan.model.device.blu;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class ShellyBluUnmanaged extends AbstractBluDevice {
	private String type;

	public ShellyBluUnmanaged(ShellyAbstractDevice parent, JsonNode info, String id) throws IOException {
		super(parent, info, id);
		this.type = info.path("config").path("meta").path("ui").path("local_name").asText();
	}

	@Override
	public String getTypeID() {
		return type;
	}
	
	public String getTypeName() {
		return "Generic BLU";
	}
	
	@Override
	public String toString() {
		return "Shelly BLU (unmanaged) " + type + ": " + super.toString();
	}
}