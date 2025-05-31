package it.usna.shellyscan.model.device.g3;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.KVS;
import it.usna.shellyscan.model.device.g2.modules.LoginManagerG2;
import it.usna.shellyscan.model.device.g2.modules.WIFIManagerG2;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

/**
 * XT1 PbS base model
 */
public class XT1 extends AbstractG3Device {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractG3Device.class);
	public final static String ID = "XT1";
	
	public XT1(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "XT1";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	// No DynamicComponents & Script
	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> userPref) throws IOException {
		final ArrayList<String> errors = new ArrayList<>();
		try {
			final long delay = this instanceof BatteryDeviceInterface ? (Devices.MULTI_QUERY_DELAY / 2) : Devices.MULTI_QUERY_DELAY;

			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
			errors.add("->r_step:specific");
			restore(backupJsons, errors);
			if(status == Status.OFF_LINE) {
				return errors.size() > 0 ? errors : List.of(RestoreMsg.ERR_UNKNOWN.toString());
			}

//			errors.add("->r_step:DynamicComponents");
//			DynamicComponents.restore(this, backupJsons, errors); // only devices with same (existing) addr are restored; if a device is no more present, related  webooks will signal error(s)
			
			errors.add("->r_step:restoreCommonConfig");
			restoreCommonConfig(config, delay, userPref, errors);

			errors.add("->r_step:restoreSchedule");
			JsonNode schedule = backupJsons.get("Schedule.List.json");
			if(schedule != null) { // some devices do not have Schedule.List +H&T
				restoreSchedule(schedule, delay, errors);
			}

//			errors.add("->r_step:Script");
//			Script.restoreAll(this, backupJsons, delay, userPref.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_OVERRIDE), userPref.containsKey(RestoreMsg.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP), errors);

			errors.add("->r_step:KVS");
			JsonNode kvs = backupJsons.get("KVS.GetMany.json");
			if(kvs != null) {
				try {
					TimeUnit.MILLISECONDS.sleep(delay); // new KVS(,,,)
					KVS kvStore = new KVS(this);
					kvStore.restoreKVS(kvs, errors);
				} catch(Exception e) {}
			}
			
			errors.add("->r_step:Webhooks");
			Webhooks.restore(this, backupJsons.get("Webhook.List.json"), delay, errors);

			errors.add("->r_step:WIFIManagerG2");
			TimeUnit.MILLISECONDS.sleep(delay);
			Network currentConnection = WIFIManagerG2.currentConnection(this);
			if(currentConnection != Network.UNKNOWN) {
				JsonNode sta1Node = config.at("/wifi/sta1");
				if(sta1Node.isMissingNode() == false && (userPref.containsKey(RestoreMsg.RESTORE_WI_FI2) || sta1Node.path("is_open").asBoolean() || sta1Node.path("enable").asBoolean() == false) && currentConnection != Network.SECONDARY) {
					TimeUnit.MILLISECONDS.sleep(delay);
					WIFIManagerG2 wm = new WIFIManagerG2(this, Network.SECONDARY, true);
					errors.add(wm.restore(sta1Node, userPref.get(RestoreMsg.RESTORE_WI_FI2)));
				}
				if((userPref.containsKey(RestoreMsg.RESTORE_WI_FI1) || config.at("/wifi/sta/is_open").asBoolean() || config.at("/wifi/sta/enable").asBoolean() == false) && currentConnection != Network.PRIMARY) {
					TimeUnit.MILLISECONDS.sleep(delay);
					WIFIManagerG2 wm = new WIFIManagerG2(this, Network.PRIMARY, true);
					errors.add(wm.restore(config.at("/wifi/sta"), userPref.get(RestoreMsg.RESTORE_WI_FI1)));
				}
				
				TimeUnit.MILLISECONDS.sleep(delay);
				JsonNode apNode = config.at("/wifi/ap"); // e.g. wall display -> isMissingNode()
				if(currentConnection != Network.AP && apNode.isMissingNode() == false &&
						((userPref.containsKey(RestoreMsg.RESTORE_WI_FI_AP) || apNode.path("is_open").asBoolean() || apNode.path("enable").asBoolean() == false))) {
					errors.add(WIFIManagerG2.restoreAP_roam(this, config.get("wifi"), userPref.get(RestoreMsg.RESTORE_WI_FI_AP)));
				} else {
					errors.add(WIFIManagerG2.restoreRoam(this, config.get("wifi")));
				}
			}
			
			errors.add("->r_step:LoginManagerG2");
			TimeUnit.MILLISECONDS.sleep(delay);
			LoginManagerG2 lm = new LoginManagerG2(this, true);
			if(userPref.containsKey(RestoreMsg.RESTORE_LOGIN)) {
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(lm.set(null, userPref.get(RestoreMsg.RESTORE_LOGIN).toCharArray()));
			} else if(backupJsons.get("Shelly.GetDeviceInfo.json").path("auth_en").asBoolean() == false) {
				TimeUnit.MILLISECONDS.sleep(delay);
				errors.add(lm.disable());
			}
		} catch(RuntimeException | InterruptedException e) {
			LOG.error("restore - RuntimeException", e);
			errors.add(RestoreMsg.ERR_UNKNOWN.toString());
		}
		return errors;
	}
	
	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws IOException, InterruptedException {
		// @Override on subclasses if needed
	}
}

// model S3XT-0S (LinkedGo ST1820)
// Feature implemented as virtual components; e.g.:
// http://192.168.1.xxx/rpc/Number.GetConfig?id=202 - http://192.168.1.xxx/rpc/Number.GetStatus?id=202
// http://192.168.1.xxx/rpc/Shelly.GetComponents?keys=["boolean:202","number:200","number:201","number:202"]
// http://192.168.1.xxx/rpc/Number.Set?id=202&value=30