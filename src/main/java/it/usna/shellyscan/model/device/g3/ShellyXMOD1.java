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
//TODO gestire eventuali input in eccesso (o separare tutto) creando un array di LabelHolder (?) e appositi renderers/editors
public class ShellyXMOD1 extends AbstractG3Device implements RelayCommander {
	public final static String ID = "XMOD1";
//	TODO	private Meters[] meters;
	private int numInputs;
	private int numOutputs;
	private Relay[] inputOutput;

	public ShellyXMOD1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		final JsonNode xmod = getJSON("/rpc/XMOD.GetInfo").get("jwt").get("xmod1");
		numInputs = xmod.get("ni").intValue();
		numOutputs = xmod.get("no").intValue();

		inputOutput = new Relay[numOutputs];
		for(int i = 0; i < numOutputs; i++) {
			inputOutput[i] = new Relay(this, i);
		}

		fillSettings(getJSON("/rpc/Shelly.GetConfig"));
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly X MOD1";
	}
	
	@Override
	public Relay getRelay(int index) {
		return /*(Relay)*/inputOutput[index];
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public Relay[] getRelays() {
		return /*(Relay[])*/inputOutput;
	}
	
	@Override
	public int getRelayCount() {
		return numOutputs;
	}
	
//	@Override
//	public Meters[] getMeters() {
//		return meters;
//	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		for(int i = 0; i < numOutputs; i++) {
			if(i < numInputs) {
				(/*(Relay)*/inputOutput[i]).fillSettings(configuration.get("switch:" + i), configuration.get("input:" + i));
			} else {
				(/*(Relay)*/inputOutput[i]).fillSettings(configuration.get("switch:" + i));
			}
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		for(int i = 0; i < numOutputs; i++) {
			if(i < numInputs) {
				(/*(Relay)*/inputOutput[i]).fillStatus(status.get("switch:" + i), status.get("input:" + i));
			} else {
				(/*(Relay)*/inputOutput[i]).fillStatus(status.get("switch:" + i));
			}
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {
				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
				"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
				/*"/rpc/BTHome.GetConfig", "/rpc/BTHome.GetStatus",*/ "/rpc/XMOD.GetProductJWS", "/rpc/XMOD.GetInfo"};
	}
	
	//TODO warning on numStoredInputs != numInputs || numStoredOutputs != numOutputs
//	@Override
//	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<Restore, String> res) {
//	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		JsonNode xmodStored = backupJsons.get("XMOD.GetInfo.json");
		int numStoredInputs = xmodStored.at("/jwt/xmod1/ni").intValue();
		int numStoredOutputs = xmodStored.at("/jwt/xmod1/no").intValue();
		
		for(int i = 0; i < Math.min(numStoredInputs, numInputs); i++) {
			errors.add(Input.restore(this, configuration, i));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		}
		for(int i = 0; i < Math.min(numStoredOutputs, numOutputs); i++) {
			errors.add((/*(Relay)*/inputOutput[i]).restore(configuration));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		}
	}
	
	@Override
	public String toString() {
		return super.toString() + " Inputs: " + numInputs + "; Outputs: " + numOutputs;
	}
}