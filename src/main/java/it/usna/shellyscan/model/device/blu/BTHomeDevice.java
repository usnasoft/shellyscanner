package it.usna.shellyscan.model.device.blu;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class BTHomeDevice extends AbstractBluDevice {
	private final String localName;
	private String typeName;
	
	public BTHomeDevice(ShellyAbstractDevice parent, JsonNode info, String localName, String index) throws IOException {
		super(parent, info, index);

		this.localName = localName;
		try {
			typeName = BTHomeIDs.valueOf(localName.replace("-", "_")).getLabel();
		} catch(IllegalArgumentException e) {
			typeName = "Generic BTHome";
		}
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public String getTypeID() {
		return localName;
	}
}
