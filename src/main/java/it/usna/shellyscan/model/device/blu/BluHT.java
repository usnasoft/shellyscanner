package it.usna.shellyscan.model.device.blu;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class BluHT extends AbstractBluDevice {
	public BluHT(ShellyAbstractDevice parent, JsonNode info, String index) throws IOException {
		super(parent, info, index);
	}

	public final static String ID = "SBHT-003C";

	@Override
	public String getTypeName() {
		return "Blu H&T";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
}
