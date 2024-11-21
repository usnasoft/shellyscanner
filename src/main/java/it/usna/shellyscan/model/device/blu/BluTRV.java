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

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.DeviceModule;
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
			tempChanged = false;
		} else {
			this.targetTemp = trv.get("target_C").floatValue();
		}
		this.pos = trv.get("pos").intValue();
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {"/rpc/BluTrv.GetRemoteDeviceInfo?id=" + componentIndex, "/rpc/BluTrv.GetConfig?id=" + componentIndex, "/rpc/BluTrv.GetRemoteConfig?id=" + componentIndex,
				"/rpc/BluTrv.GetStatus?id=" + componentIndex, "/rpc/BluTrv.GetRemoteStatus?id=" + componentIndex, "/rpc/BluTrv.CheckForUpdates?id=" + componentIndex};
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
	public boolean backup(File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			sectionToStream("/rpc/BluTrv.GetRemoteDeviceInfo?id=" + componentIndex, "Shelly.GetRemoteDeviceInfo.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/BluTrv.GetRemoteConfig?id=" + componentIndex, "Shelly.GetRemoteConfig.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/BluTrv.GetConfig?id=" + componentIndex, "Shelly.GetConfig.json", out);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
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
		errors.add("Currently unsupported");
		// TODO Auto-generated method stub
		return errors;
	}
	
	// --- ThermostatInterface ---
	
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

//http://192.168.1.29/rpc/BluTrv.SetConfig?id=200&config={"name":"mytrv"}