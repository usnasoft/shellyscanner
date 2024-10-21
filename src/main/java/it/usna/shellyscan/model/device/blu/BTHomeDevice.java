package it.usna.shellyscan.model.device.blu;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class BTHomeDevice extends AbstractBluDevice /*implements ModulesHolder*/ {
	private String typeName;
	private final static Map<String, String> DevDictionary = Map.of(
			"SBBT-002C", "Blu Button",
			"SBMO-003Z", "Motion",
			"SBDW-002C", "Blu Door Window",
			"SBHT-003C", "Blu H&T",
			"SBBT-004CEU", "Blu Wall Switch 4",
			"SBBT-004CUS", "Blu RC Button 4",
			"SBBT-USa8ac", "Blu RC Button 4" // not documented (test device?)
			);
//	private Input[] inputs;

	public BTHomeDevice(ShellyAbstractDevice parent, JsonNode info, String localName, String index) {
		super((AbstractG2Device)parent, info, index);
		this.hostname = localName + "/" + mac;
		this.localName = localName;
		typeName = Optional.ofNullable(DevDictionary.get(localName)).orElse("Generic BTHome");
//		
//		Sensor in[] = sensors.getInputSensors();
//		inputs = new Input[in.length];
//		for(int i = 0; i < in.length; i++) {
//			inputs[i] = new Input(this.parent, in[i].getId());
//		}
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
	
//	public int getModulesCount() {
//		return inputs.length;
//	}
//
//	@Override
//	public DeviceModule getModule(int index) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public DeviceModule[] getModules() {
//		// TODO Auto-generated method stub
//		return null;
//	}
	
//	public void refreshSettings() throws IOException {
//		super.refreshSettings();
//		// fillSettings(getJSON("/rpc/BTHomeDevice.GetConfig?id=" + componentIndex));
//	}
}
