package it.usna.shellyscan.model.device.blu;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.blu.modules.Sensor;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.modules.DeviceModule;

/**
 * Generic BTHome device with measures and/or buttons
 */
public class BTHomeDevice extends AbstractBluDevice implements ModulesHolder {
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
	private Webhooks webhooks = new Webhooks(parent);
	private Input[] inputs;

	public BTHomeDevice(ShellyAbstractDevice parent, JsonNode compInfo, String localName, String index) {
		super((AbstractG2Device)parent, compInfo, index);
		this.hostname = localName + "/" + mac;
		this.localName = localName;
		this.typeName = Optional.ofNullable(DevDictionary.get(localName)).orElse("Generic BTHome");
		this.webhooks = new Webhooks(this.parent);
	}
	
	@Override
	public void init(HttpClient httpClient) throws IOException {
		this.httpClient = httpClient;
		createSensors();
		Sensor in[] = sensors.getInputSensors();
		this.inputs = new Input[in.length];
		for(int i = 0; i < in.length; i++) {
			inputs[i] = new Input(/*this.parent, in[i].getId()*/);
			inputs[i].setEnabled(true);
		}
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		refreshStatus();
		refreshSettings();
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
	
	@Override
	public int getModulesCount() {
		return inputs.length;
	}

	@Override
	public DeviceModule getModule(int index) {
		return inputs[index];
	}

	@Override
	public DeviceModule[] getModules() {
		return inputs;
	}
	
	@Override
	public void refreshSettings() throws IOException {
//		super.refreshSettings();
		webhooks.fillBTHomesensorSettings();
		
		Sensor in[] = sensors.getInputSensors();
		for(int i = 0; i < in.length; i++) {
			inputs[i].setLabel(in[i].getName());
			inputs[i].associateWH(webhooks.getHooks(in[i].getId()));
		}
	}
}