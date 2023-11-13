package it.usna.shellyscan.model.device;

import java.io.File;
import java.net.InetAddress;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.WIFIManager.Network;

public class GhostDevice extends ShellyAbstractDevice {
//	private final static Logger LOG = LoggerFactory.getLogger(GhostDevice.class);
	private final String typeName;
	private final String typeID;
	private final boolean battery;
	private String note;
	
	public GhostDevice(InetAddress address, int port, String hostname,
			String mac, String ssid, String typeName, String typeID, String name, long lastConnection, boolean battery,
			String note) {
		super(address, port, hostname);
		this.mac = mac;
		this.ssid = ssid;
		this.typeName = typeName;
		this.typeID = typeID;
		this.name = name;
		this.lastConnection = lastConnection;
		this.battery = battery;
		this.note = note;
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
	
	public boolean isBattery() {
		return battery;
	}
	
	public String getNote() {
		return note;
	}
	
	public void setNote(String note) {
		this.note = note;
	}
	
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
	public void setEcoMode(boolean eco) {
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
	public boolean backup(File file) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Restore, String> restoreCheck(Map<String, JsonNode> backupJsons) {
		throw new UnsupportedOperationException();
	}
	
//	public Map<Restore, String> restoreCheckG1(Map<String, JsonNode> backupJsons) throws IOException {
//		EnumMap<Restore, String> res = new EnumMap<>(Restore.class);
//		try {
//			JsonNode settings = backupJsons.get("settings.json");
//			final String fileHostname = settings.get("device").get("hostname").asText("");
//			final String fileType = settings.get("device").get("type").asText();
//			if(fileType.length() > 0 && fileType.equals(this.getTypeID()) == false) {
//				res.put(Restore.ERR_RESTORE_MODEL, null);
//			} else {
//				boolean sameHost = fileHostname.equals(this.hostname);
//				if(sameHost == false) {
//					res.put(Restore.ERR_RESTORE_HOST, fileHostname);
//				}
//				if(settings.at("/login/enabled").asBoolean()) {
//					res.put(Restore.RESTORE_LOGIN, settings.at("/login/username").asText());
//				}
////				Network currentConnection = WIFIManagerG1.currentConnection(this);
////				if(settings.at("/wifi_sta/enabled").asBoolean() && (sameHost || settings.at("/wifi_sta/ipv4_method").asText().equals("dhcp")) && currentConnection != Network.PRIMARY) {
////					res.put(Restore.RESTORE_WI_FI1, settings.at("/wifi_sta/ssid").asText());
////				}
////				if(settings.at("/wifi_sta1/enabled").asBoolean() && (sameHost || settings.at("/wifi_sta1/ipv4_method").asText().equals("dhcp")) && currentConnection != Network.SECONDARY) {
////					res.put(Restore.RESTORE_WI_FI2, settings.at("/wifi_sta1/ssid").asText());
////				}
//				if(settings.at("/mqtt/enable").asBoolean() && settings.at("/mqtt/user").asText("").length() > 0) {
//					res.put(Restore.RESTORE_MQTT, settings.at("/mqtt/user").asText());
//				}
//			}
//		} catch(RuntimeException e) {
//			LOG.error("restoreCheck", e);
//			res.put(Restore.ERR_RESTORE_MODEL, null);
//		}
//		return res;
//	}
//	
//	public Map<Restore, String> restoreCheckG2(Map<String, JsonNode> backupJsons) throws IOException {
//		EnumMap<Restore, String> res = new EnumMap<>(Restore.class);
//		try {
//			JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
//			JsonNode config = backupJsons.get("Shelly.GetConfig.json");
//			final String fileHostname = devInfo.get("id").asText("");
//			final String fileType = devInfo.get("app").asText();
//			if(this.getTypeID().equals(fileType) == false) {
//				res.put(Restore.ERR_RESTORE_MODEL, null);
//			} else {
//				boolean sameHost = fileHostname.equals(this.hostname);
//				if(sameHost == false) {
//					res.put(Restore.ERR_RESTORE_HOST, fileHostname);
//				}
//				if(devInfo.path("auth_en").asBoolean()) {
//					res.put(Restore.RESTORE_LOGIN, LoginManagerG2.LOGIN_USER);
//				}
////				Network currentConnection = WIFIManagerG2.currentConnection(this);
////				JsonNode wifi = config.at("/wifi/sta");
////				if(wifi.path("enable").asBoolean() && (sameHost || wifi.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.PRIMARY) {
////					if(wifi.path("is_open").asBoolean() == false) {
////						res.put(Restore.RESTORE_WI_FI1, wifi.path("ssid").asText());
////					}
////				}
////				JsonNode wifi2 = config.at("/wifi/sta1");
////				if(wifi2.path("enable").asBoolean() && (sameHost || wifi2.path("ipv4mode").asText().equals("dhcp")) && currentConnection != Network.SECONDARY) {
////					if(wifi2.path("is_open").asBoolean() == false) {
////						res.put(Restore.RESTORE_WI_FI2, wifi2.path("ssid").asText());
////					}
////				}
////				JsonNode wifiAP = config.at("/wifi/ap");
////				if(wifiAP.path("enable").asBoolean() && currentConnection != Network.AP) {
////					if(wifiAP.path("is_open").asBoolean() == false) {
////						res.put(Restore.RESTORE_WI_FI_AP, wifiAP.path("ssid").asText());
////					}
////				}
//				
//				if(config.at("/mqtt/enable").asBoolean() && config.at("/mqtt/user").asText("").length() > 0) {
//					res.put(Restore.RESTORE_MQTT, config.at("/mqtt/user").asText());
//				}
//				// device specific
//				restoreCheck(backupJsons, res);
//			}
//		} catch(RuntimeException e) {
//			LOG.error("restoreCheck", e);
//			res.put(Restore.ERR_RESTORE_MODEL, null);
//		}
//		return res;
//	}

	@Override
	public String restore(Map<String, JsonNode> backupJsons, Map<Restore, String> data) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return "GHOST: " + super.toString();
	}
}