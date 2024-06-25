package it.usna.shellyscan.model.device;

import java.io.File;
import java.net.InetAddress;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.WIFIManager.Network;
import it.usna.shellyscan.model.device.g2.LoginManagerG2;

public class GhostDevice extends ShellyAbstractDevice {
	private final static Logger LOG = LoggerFactory.getLogger(GhostDevice.class);
	private final String typeName;
	private final int gen;
	private final String typeID;
	private final boolean battery;
	private String note;
	private String keyNote;
	
	public GhostDevice(InetAddress address, int port, String hostname,
			String mac, String ssid, String typeName, String typeID, int gen, String name, long lastConnection, boolean battery,
			String note, String keyNote) {
		super(address, port, hostname);
		this.mac = mac;
		this.ssid = ssid;
		this.typeName = typeName;
		this.gen = gen;
		this.typeID = typeID;
		this.name = name;
		this.lastConnection = lastConnection;
		this.battery = battery;
		this.note = note;
		this.keyNote = keyNote;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}
	
	@Override
	public Status getStatus() {
		return Status.GHOST;
	}

	@Override
	public String getTypeID() {
		return typeID;
	}

	public int getGeneration() {
		return gen;
	}
	
	public boolean isBatteryOperated() {
		return battery;
	}
	
	public String getNote() {
		return note;
	}
	
	public void setNote(String note) {
		this.note = note;
	}
	
	public String getKeyNote() {
		return keyNote;
	}
	
	public void setKeyNote(String keyNote) {
		this.keyNote = keyNote;
	}
	
	@Override
	public JsonNode getJSON(final String command) throws DeviceOfflineException {
		throw new DeviceOfflineException("Status-GHOST");
	}

	@Override
	public String[] getInfoRequests() {
		return new String[] {"/shelly"};
	}

	@Override
	public void reboot() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String setCloudEnabled(boolean enable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setEcoMode(boolean eco) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void refreshSettings() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void refreshStatus() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FirmwareManager getFWManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WIFIManager getWIFIManager(Network net) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MQTTManager getMQTTManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LoginManager getLoginManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TimeAndLocationManager getTimeAndLocationManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean backup(File file) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Restore, String> restoreCheck(Map<String, JsonNode> backupJsons) {
		if(backupJsons.containsKey("settings.json")) {
			return restoreCheckG1(backupJsons);
		} else if(backupJsons.containsKey("Shelly.GetConfig.json")) {
			return restoreCheckG2(backupJsons);
		} else {
			return Collections.singletonMap(Restore.ERR_RESTORE_MODEL, null);
		}
	}
	
	private Map<Restore, String> restoreCheckG1(Map<String, JsonNode> backupJsons) {
		EnumMap<Restore, String> res = new EnumMap<>(Restore.class);
		try {
			JsonNode settings = backupJsons.get("settings.json");
			final String fileHostname = settings.get("device").get("hostname").asText("");
			final String fileType = settings.get("device").get("type").asText();
			if(/*fileType.length() > 0 &&*/ getTypeID().equals(fileType) == false) {
				res.put(Restore.ERR_RESTORE_MODEL, null);
			} else {
				boolean sameHost = fileHostname.equals(this.hostname);
				if(sameHost == false) {
					res.put(Restore.ERR_RESTORE_HOST, fileHostname);
				}
				if(settings.at("/login/enabled").asBoolean()) {
					res.put(Restore.RESTORE_LOGIN, settings.at("/login/username").asText());
				}
				// Can't restore network values
				if(settings.at("/mqtt/enable").asBoolean() && settings.at("/mqtt/user").asText("").length() > 0) {
					res.put(Restore.RESTORE_MQTT, settings.at("/mqtt/user").asText());
				}
			}
		} catch(RuntimeException e) {
			LOG.error("restoreCheck", e);
			res.put(Restore.ERR_RESTORE_MODEL, null);
		}
		return res;
	}

	private Map<Restore, String> restoreCheckG2(Map<String, JsonNode> backupJsons) {
		EnumMap<Restore, String> res = new EnumMap<>(Restore.class);
		try {
			JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
			final String fileHostname = devInfo.get("id").asText("");
			final String fileType = devInfo.get("app").asText();
			if(getTypeID().equals(fileType) == false) {
				res.put(Restore.ERR_RESTORE_MODEL, null);
			} else {
				boolean sameHost = fileHostname.equals(this.hostname);
				if(sameHost == false) {
					res.put(Restore.ERR_RESTORE_HOST, fileHostname);
				}
				if(devInfo.path("auth_en").asBoolean()) {
					res.put(Restore.RESTORE_LOGIN, LoginManagerG2.LOGIN_USER);
				}
				// Can't restore network values
				if(config.at("/mqtt/enable").asBoolean() && config.at("/mqtt/user").asText("").length() > 0) {
					res.put(Restore.RESTORE_MQTT, config.at("/mqtt/user").asText());
				}
				// device specific
//				restoreCheck(backupJsons, res); // TODO check compatibility with this call for any new device
			}
		} catch(RuntimeException e) {
			LOG.error("restoreCheck", e);
			res.put(Restore.ERR_RESTORE_MODEL, null);
		}
		return res;
	}
	
	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<Restore, String> data) {
		return List.of("GhostDevice");
	}
	
	@Override
	public String toString() {
		return "GHOST: " + super.toString();
	}
}