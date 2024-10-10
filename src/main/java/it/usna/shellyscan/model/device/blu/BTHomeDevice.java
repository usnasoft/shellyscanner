package it.usna.shellyscan.model.device.blu;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class BTHomeDevice extends AbstractBluDevice {
	private String typeName;
	private final static Map<String, String> DevDictionary = Map.of(
			"SBBT-002C", "Blu Button",
			"SBMO-003Z", "Motion",
			"SBDW-002C", "Blu Door Window",
			"SBHT-003C", "Blu H&T",
			"SBBT-004CEU", "Blu Wall Switch 4",
			"SBBT-004CUS", "Blu RC Button 4"
			); 

	public BTHomeDevice(ShellyAbstractDevice parent, JsonNode info, String localName, String index) {
		super((AbstractG2Device)parent, info, index);
		this.hostname = localName + "-" + mac;
		this.localName = localName;
		typeName = Optional.ofNullable(DevDictionary.get(localName)).orElse("Generic BTHome");
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
}
