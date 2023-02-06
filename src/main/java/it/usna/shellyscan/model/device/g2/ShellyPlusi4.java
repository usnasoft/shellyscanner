package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.modules.InputCommander;
import it.usna.shellyscan.model.device.modules.InputInterface;

public class ShellyPlusi4 extends AbstractG2Device implements InputCommander {
	public final static String ID = "PlusI4";
	private Input[] inputs;
	private Webhooks webhooks;

	public ShellyPlusi4(InetAddress address, String hostname) {
		super(address, hostname);
		
		inputs = new Input[] {new Input(), new Input(), new Input(), new Input()};
		webhooks = new Webhooks(this);
	}
	
	@Override
	public String getTypeName() {
		return "Shelly +i4";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		inputs[0].fillSettings(configuration.get("input:0"));
		inputs[1].fillSettings(configuration.get("input:1"));
		inputs[2].fillSettings(configuration.get("input:2"));
		inputs[3].fillSettings(configuration.get("input:3"));
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		webhooks.fillSettings();
		inputs[0].associateWH(webhooks.getHooks(0));
		inputs[1].associateWH(webhooks.getHooks(1));
		inputs[2].associateWH(webhooks.getHooks(2));
		inputs[3].associateWH(webhooks.getHooks(3));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		inputs[0].fillStatus(status.get("input:0"));
		inputs[1].fillStatus(status.get("input:1"));
		inputs[2].fillStatus(status.get("input:2"));
		inputs[3].fillStatus(status.get("input:3"));
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(Input.restore(this, configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "1"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "2"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "3"));
	}
	
	@Override
	public InputInterface getActionsGroup(int index) {
		return inputs[index];
	}

	@Override
	public InputInterface[] getActionsGroups() {
		return inputs;
	}
	
//	@Override
//	public String toString() {
//		return super.toString();
//	}
}