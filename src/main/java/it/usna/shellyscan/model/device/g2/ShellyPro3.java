package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyPro3 extends AbstractProDevice implements RelayCommander, InternalTmpHolder {
	public final static String ID = "Pro3";
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay relay2 = new Relay(this, 2);
	private float internalTmp;
	private RelayInterface[] relays = new RelayInterface[] {relay0, relay1, relay2};

	public ShellyPro3(InetAddress address, String hostname) {
		super(address, hostname);
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro 3";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public int getRelayCount() {
		return 3;
	}

	@Override
	public Relay getRelay(int index) {
		if(index == 0) return relay0;
		else if (index == 1) return relay1;
		else return relay2;
	}

	@Override
	public RelayInterface[] getRelays() {
		return relays;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"));
		relay1.fillSettings(configuration.get("switch:1"));
		relay2.fillSettings(configuration.get("switch:2"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);

		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0, status.get("input:0"));

		JsonNode switchStatus1 = status.get("switch:1");
		relay1.fillStatus(switchStatus1, status.get("input:1"));
		
		JsonNode switchStatus2 = status.get("switch:2");
		relay2.fillStatus(switchStatus2, status.get("input:2"));

		internalTmp = (float)switchStatus0.path("temperature").path("tC").asDouble();
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(Input.restore(this,configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, "1"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this,configuration, "2"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay0.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay1.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay2.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1;
	}
}

/*
{
  "name" : null,
  "id" : "shellypro2-xxx",
  "mac" : "xxx",
  "model" : "SPSW-202XE16EU",
  "gen" : 2,
  "fw_id" : "20221206-143405/0.12.0-gafc2404",
  "ver" : "0.12.0",
  "app" : "Pro2",
  "auth_en" : false,
  "auth_domain" : null
}
*/