package it.usna.shellyscan.model.device.blu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.blu.modules.FirmwareManagerTRV;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;

public class BluTRV extends AbstractBluDevice implements ThermostatInterface, ModulesHolder {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractBluDevice.class);
	public final static String DEVICE_KEY_PREFIX = "blutrv:";
	public final static String ID = "BluTRV";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.BAT};
	private int battery;
	private float externalTemp;
	private float targetTemp;
	private int pos;
	private boolean enabled;
	private Meters[] meters;
	private ThermostatInterface[] thermostats = new ThermostatInterface[] {this};
	private boolean tempChanged = false;
	
	public BluTRV(AbstractG2Device parent, JsonNode compInfo, String index) {
		super(parent, compInfo, index);
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.BAT) {
							return battery;
						} else {
							return externalTemp;
						}
					}
				}
		};
	}
	
	@Override
	public void init(HttpClient httpClient/*, WebSocketClient wsClient*/) throws IOException {
		super.init(httpClient);
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		this.hostname = getJSON("/rpc/BluTrv.GetRemoteDeviceInfo?id=" + componentIndex).get("device_info").get("id").asText();
	}

	@Override
	public String getTypeName() {
		return "Blu TRV";
	}

	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	public void refreshSettings() throws IOException {
		JsonNode settings = getJSON("/rpc/BluTrv.GetConfig?id=" + componentIndex);
		this.name = settings.get("name").asText("");
		
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) { }
		JsonNode remoteConfig = getJSON("/rpc/BluTrv.GetRemoteConfig?id=" + componentIndex).get("config");
		this.enabled = remoteConfig.get("trv:0").get("enable").asBoolean();
	}

	@Override
	public void refreshStatus() throws IOException {
		JsonNode status = getJSON("/rpc/BluTrv.GetStatus?id=" + componentIndex);
		this.rssi = status.path("rssi").intValue();
		this.lastConnection = status.path("last_updated_ts").intValue() * 1000L;
		this.battery = status.path("battery").intValue();
		
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) { }
		JsonNode remoteStatus = getJSON("/rpc/BluTrv.GetRemoteStatus?id=" + componentIndex).get("status");
		this.uptime = remoteStatus.get("sys").get("uptime").asInt();
		JsonNode trv = remoteStatus.get("trv:0");
		this.externalTemp = trv.get("current_C").floatValue();
		if(tempChanged) {
			tempChanged = false; // do not overwrite after a GUI command
		} else {
			this.targetTemp = trv.get("target_C").floatValue();
		}
		this.pos = trv.get("pos").intValue();
		//http://192.168.1.29/rpc/BluTrv.Call?id=200&method="TRV.ListScheduleRules"&params={"id":0}
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {"/rpc/BluTrv.GetRemoteDeviceInfo?id=" + componentIndex, "/rpc/BluTrv.GetConfig?id=" + componentIndex, "/rpc/BluTrv.GetRemoteConfig?id=" + componentIndex,
				"/rpc/BluTrv.GetStatus?id=" + componentIndex, "/rpc/BluTrv.GetRemoteStatus?id=" + componentIndex, "/rpc/BluTrv.CheckForUpdates?id=" + componentIndex,
				"(TRV.ListScheduleRules)/rpc/BluTrv.Call?id=" + componentIndex + "&method=%22TRV.ListScheduleRules%22&params=%7B%22id%22:0%7D"};
	}
	
	@Override
	public void reboot() throws IOException {
		getJSON("/rpc/BluTrv.call?id=" + componentIndex + "&method=Shelly.Reboot");
	}
	
	@Override
	public DeviceModule getModule(int index) {
		return this;
	}

	@Override
	public DeviceModule[] getModules() {
		return thermostats;
	}
	
	@Override
	public FirmwareManager getFWManager() {
		return new FirmwareManagerTRV(this);
	}

	@Override
	public boolean backup(File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			sectionToStream("/rpc/BluTrv.GetRemoteDeviceInfo?id=" + componentIndex, "Shelly.GetRemoteDeviceInfo.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/BluTrv.GetRemoteConfig?id=" + componentIndex, "Shelly.GetRemoteConfig.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			byte[] config = sectionToStream("/rpc/BluTrv.GetConfig?id=" + componentIndex, "Shelly.GetConfig.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/BluTrv.Call?id=" + componentIndex + "&method=%22TRV.ListScheduleRules%22&params=%7B%22id%22:0%7D", "TRV.ListScheduleRules.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
			String bthome = jsonMapper.readTree(config).path("trv").asText();
			String bhtIndex = bthome.substring(bthome.indexOf(':') + 1);
			sectionToStream("/rpc/BTHomeDevice.GetKnownObjects?id=" + bhtIndex, "BTHomeDevice.GetKnownObjects.json", out);
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		}
		return true;
	}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
		try {
			JsonNode remoteDevInfo = backupJsons.get("Shelly.GetRemoteDeviceInfo.json");
			if(remoteDevInfo == null) {
				res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
				return res;
			}
			JsonNode devInfo = remoteDevInfo.get("device_info");
			if(devInfo == null || getTypeID().equals(devInfo.get("app").asText()) == false) {
				res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			} else {
				final String fileHostname = devInfo.get("id").asText("");
				boolean sameHost = fileHostname.equals(this.hostname);
				if(sameHost == false) {
					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, fileHostname);
				}
			}
		} catch(RuntimeException e) {
			LOG.error("restoreCheck", e);
			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
		}
		return res;
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) throws IOException {
		final ArrayList<String> errors = new ArrayList<>();
		try {
			// BluTrv.SetConfig
			JsonNode storedConfig = backupJsons.get("Shelly.GetConfig.json");
			ObjectNode out = JsonNodeFactory.instance.objectNode();
			out.put("id", Integer.parseInt(componentIndex));
			ObjectNode outConfig = JsonNodeFactory.instance.objectNode();
			outConfig.set("name", storedConfig.get("name"));
			out.set("config", outConfig);
			errors.add(postCommand("BluTrv.SetConfig", out)); // http://192.168.1.29/rpc/BluTrv.SetConfig?id=200&config={%22name%22:%22xxx%22}
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

			JsonNode storedRemoteConfig = backupJsons.get("Shelly.GetRemoteConfig.json").get("config");
			out = JsonNodeFactory.instance.objectNode();
			out.put("id", Integer.parseInt(componentIndex));

			// BluTrv.Call - Sys.SetConfig
			ObjectNode sysParams = JsonNodeFactory.instance.objectNode();
			sysParams.put("id", 0);
			ObjectNode sys = JsonNodeFactory.instance.objectNode();
			sys.set("ui", storedRemoteConfig.get("sys").get("ui"));
			sysParams.set("config", sys);
			out.put("method", "Sys.SetConfig");
			out.set("params", sysParams);
			errors.add(postCommand("BluTrv.Call", out));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

			// BluTrv.Call - Temperature.SetConfig
			ObjectNode tempParams = JsonNodeFactory.instance.objectNode();
			tempParams.put("id", 0);
			ObjectNode temp0 = ((ObjectNode)storedRemoteConfig.get("temperature:0"));
			temp0.remove("id");
			tempParams.set("config", temp0);
			out.set("params", tempParams);
			out.put("method", "Temperature.SetConfig");
			errors.add(postCommand("BluTrv.Call", out));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

			// BluTrv.Call - TRV.SetConfig
			ObjectNode trvParams = JsonNodeFactory.instance.objectNode();
			trvParams.put("id", 0);
			ObjectNode trv = ((ObjectNode)storedRemoteConfig.get("trv:0"));
			trv.remove("id");
			trvParams.set("config", trv);
			out.set("params", trvParams);
			out.put("method", "TRV.SetConfig");
			errors.add(postCommand("BluTrv.Call", out));
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);

			// BluTrv.Call - TRV.RemoveScheduleRule
			JsonNode existingRules = getJSON("/rpc/BluTrv.Call?id=" + componentIndex + "&method=%22TRV.ListScheduleRules%22&params=%7B%22id%22:0%7D").get("rules");
			ObjectNode scheduleParams = JsonNodeFactory.instance.objectNode();
			scheduleParams.put("id", 0);
			out.put("method", "TRV.RemoveScheduleRule");
			for(JsonNode rule: existingRules) {
				scheduleParams.set("rule_id", rule.get("rule_id"));
				out.set("params", scheduleParams);
				errors.add(postCommand("BluTrv.Call", out));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}

			// BluTrv.Call - TRV.AddScheduleRule
			JsonNode storedSchedule = backupJsons.get("TRV.ListScheduleRules.json").get("rules");
			/*ObjectNode*/ scheduleParams = JsonNodeFactory.instance.objectNode();
			scheduleParams.put("id", 0);
			out.put("method", "TRV.AddScheduleRule");
			for(JsonNode rule: storedSchedule) {
				((ObjectNode)rule).remove("rule_id");
				scheduleParams.set("rule", rule);
				out.set("params", scheduleParams);
				errors.add(postCommand("BluTrv.Call", out));
				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			}

			final int storedId = storedConfig.get("id").intValue();
			final int currentId = Integer.parseInt(componentIndex);
			JsonNode storedWebHooks = backupJsons.get("Webhook.List.json");
			Webhooks.delete(parent,"blutrv", currentId);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			Webhooks.restore(parent, "blutrv", storedId, Integer.parseInt(componentIndex), Devices.MULTI_QUERY_DELAY, storedWebHooks, errors);
			
			// todo bthomesensor actions restore
			JsonNode storedBTHSensors = backupJsons.get("BTHomeDevice.GetKnownObjects.json").get("objects");
			final String storedBtHome = storedConfig.get("trv").asText();
			String bhtIndex = storedBtHome.substring(storedBtHome.indexOf(':') + 1);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			JsonNode existingBTHSensors = getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + bhtIndex).get("objects");
			
			for(JsonNode sensorConf: storedBTHSensors) {
				String comp = sensorConf.path("component").asText();
				if(comp != null && comp.startsWith(SENSOR_KEY_PREFIX)) {
//					confronto tra storedBTHSensors e existingBTHSensors
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//					Webhooks.delete
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//					Webhooks.restore
				}
			}
//			Webhooks.restore(parent, storedId, /*per cid sensore*/, Devices.MULTI_QUERY_DELAY, storedWebHooks, errors);

		} catch(RuntimeException | InterruptedException e) {
			LOG.error("restore - RuntimeException", e);
			errors.add(RestoreMsg.ERR_UNKNOWN.toString());
		}
		return errors;
	}
	
	// --- ThermostatInterface implementation ---
	
	@Override
	public String getLabel() {
		return null;
	}
	
	@Override
	public float getMaxTargetTemp() {
		return 30f;
	}
	
	@Override
	public float getMinTargetTemp() {
		return 4f;
	}
	
	@Override
	public int getUnitDivision() {
		return 10;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isRunning() {
		return pos > 0;
	}

	@Override
	public void setEnabled(boolean enable) throws IOException {
		String res = postCommand("BluTrv.Call", "{\"id\":" + componentIndex + ",\"method\":\"TRV.SetConfig\",\"params\":{\"id\":0,\"config\":{\"enable\":" + enable + "}}}");
		if(res == null) {
			this.enabled = enable;
		} else {
			throw new IOException(res);
		}
	}

	@Override
	public float getTargetTemp() {
		return targetTemp;
	}

	@Override
	public void setTargetTemp(float temp) throws IOException {
		String res = postCommand("BluTrv.Call", "{\"id\":" + componentIndex + ",\"method\":\"TRV.SetTarget\",\"params\":{\"id\":0,\"target_C\":" + temp + "}}");
		if(res == null) {
			targetTemp = temp;
			tempChanged = true;
		} else {
			throw new IOException(res);
		}
	}
}