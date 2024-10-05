package it.usna.shellyscan.model.device.blu;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;

public class BTHomeDevice extends AbstractBluDevice {
	private String typeName;
	private final static Map<String, String> DevDictionary = Map.of(
			"SBHT-003C", "Blu H&T",
			"SBBT-002C", "Blu Button"
			); 
	
	public BTHomeDevice(ShellyAbstractDevice parent, JsonNode info, String localName, String index) {
		super(parent, info, index);
		this.hostname = localName + "-" + mac;
		this.localName = localName;
		typeName = Optional.ofNullable(DevDictionary.get(localName)).orElse("Generic BTHome");
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
}
