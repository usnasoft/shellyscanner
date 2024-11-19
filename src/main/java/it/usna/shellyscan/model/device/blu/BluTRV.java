package it.usna.shellyscan.model.device.blu;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.ThermostatInterface;

public class BluTRV extends AbstractBluDevice implements ThermostatInterface, ModulesHolder {
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
	
	public BluTRV(AbstractG2Device parent, JsonNode compInfo, String index) {
		super(parent, compInfo, index);
		this.hostname = ID + "-" + mac;
		
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
		this.targetTemp = trv.get("target_C").floatValue();
		this.pos = trv.get("pos").intValue();
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {"/rpc/BluTrv.GetRemoteDeviceInfo?id=" + componentIndex, "/rpc/BluTrv.GetConfig?id=" + componentIndex, "/rpc/BluTrv.GetRemoteConfig?id=" + componentIndex,
				"/rpc/BluTrv.GetStatus?id=" + componentIndex, "/rpc/BluTrv.GetRemoteStatus?id=" + componentIndex, "/rpc/BluTrv.CheckForUpdates?id=" + componentIndex};
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) throws IOException {
		// TODO Auto-generated method stub
		return null;
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
	public void setEnabled(boolean enabled) throws IOException {
		String res = postCommand("BluTrv.Call", "{\"id\":" + componentIndex + ",\"method\":\"TRV.SetConfig\",\"params\":{\"id\":0,\"enable\":" + enabled + "}}");
		if(res == null) {
			this.enabled = enabled;
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
		} else {
			throw new IOException(res);
		}
	}
}
//http://192.168.1.29/rpc/BluTrv.GetRemoteDeviceInfo?id=200
//http://192.168.1.29/rpc/BluTrv.GetConfig?id=200 ("trv": "bthomedevice:200")
//http://192.168.1.29/rpc/BluTrv.GetStatus?id=200
//http://192.168.1.29/rpc/BluTrv.CheckForUpdates?id=200
//http://192.168.1.29/rpc/BluTrv.GetRemoteConfig?id=200
//http://192.168.1.29/rpc/BluTrv.GetRemoteStatus?id=200
//http://192.168.1.29/rpc/BluTrv.Call?id=200&method="TRV.SetTarget"&params={"id":0,"target_C":24}
//http://192.168.1.29/rpc/BluTrv.SetConfig?id=200&config={"name":"mytrv"}