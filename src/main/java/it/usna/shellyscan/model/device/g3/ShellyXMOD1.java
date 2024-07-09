package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;

/**
 * Shelly X MOD1 model
 * @author usna
 */
public class ShellyXMOD1 extends AbstractG3Device implements RelayCommander {
	public final static String ID = "XMOD1";
	private Relay relay0 = new Relay(this, 0);
	private Relay relay1 = new Relay(this, 1);
	private Relay relay2 = new Relay(this, 2);
	private Relay relay3 = new Relay(this, 3);
//	private float power;
//	private float voltage;
//	private float current;
	private Relay[] relays = new Relay[] {relay0, relay1, relay2, relay3};
//	private Meters[] meters;

	public ShellyXMOD1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
//	@Override
//	protected void init(JsonNode devInfo) throws IOException {
//		this.hostname = devInfo.get("id").asText("");
//		this.mac = devInfo.get("mac").asText();
//		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
//	
////		Meters m0 = new MetersWVI() {
////			@Override
////			public float getValue(Type t) {
////				if(t == Meters.Type.W) {
////					return power;
////				} else if(t == Meters.Type.I) {
////					return current;
////				} else {
////					return voltage;
////				}
////			}
////		};
//
////		meters = (addOn == null || addOn.getTypes().length == 0) ? new Meters[] {m0} : new Meters[] {m0, addOn};
//		
//		fillSettings(config);
//		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
//	}
	
	@Override
	public String getTypeName() {
		return "Shelly X MOD1";
	}
	
	@Override
	public Relay getRelay(int index) {
		return relays[index];
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public Relay[] getRelays() {
		return relays;
	}
	
	@Override
	public int getRelayCount() {
		return 4;
	}
	
//	public float getPower() {
//		return power;
//	}
//	
//	public float getVoltage() {
//		return voltage;
//	}
//	
//	public float getCurrent() {
//		return current;
//	}
	
//	@Override
//	public Meters[] getMeters() {
//		return meters;
//	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"), configuration.get("input:0"));
		relay1.fillSettings(configuration.get("switch:1"), configuration.get("input:1"));
		relay2.fillSettings(configuration.get("switch:2"), configuration.get("input:2"));
		relay3.fillSettings(configuration.get("switch:3"), configuration.get("input:3"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		relay0.fillStatus(status.get("switch:0"), status.get("input:0"));
		relay1.fillStatus(status.get("switch:1"), status.get("input:1"));
		relay2.fillStatus(status.get("switch:2"), status.get("input:2"));
		relay3.fillStatus(status.get("switch:3"), status.get("input:3"));
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
				"/rpc/BTHome.GetConfig", "/rpc/BTHome.GetStatus", "/rpc/XMOD.GetProductJWS", "/rpc/XMOD.GetInfo"};
	}
	
//	@Override
//	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<Restore, String> res) {
//		if(SensorAddOn.restoreCheck(this, backupJsons, res) == false) {
//			res.put(Restore.WARN_RESTORE_MSG, SensorAddOn.MSG_RESTORE_ERROR);
//		}
//	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "1"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "2"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, "3"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		
		errors.add(relay0.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay1.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay2.restore(configuration));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay3.restore(configuration));
		
//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		SensorAddOn.restore(this, backupJsons, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay0: " + relay0 + "; Relay1: " + relay1 + "; Relay2: " + relay2 + "; Relay3: " + relay3;
	}
}