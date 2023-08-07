package it.usna.shellyscan.model.device;

import java.io.File;
import java.net.InetAddress;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.WIFIManager.Network;

public class GhostDevice extends ShellyAbstractDevice {
	private String typeName;
	private String typeID;
	
	public GhostDevice(InetAddress address, int port, String hostname,
			String mac, String ssid, String typeName, String typeID, String name, long lastConnection) {
		super(address, port, hostname);
		this.mac = mac;
		this.ssid = ssid;
		this.typeName = typeName;
		this.typeID = typeID;
		this.name = name;
		this.lastConnection = lastConnection;
	}

	private GhostDevice(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
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
	
	@Override
	public String[] getInfoRequests() {
		throw new UnsupportedOperationException();
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

	@Override
	public String restore(Map<String, JsonNode> backupJsons, Map<Restore, String> data) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return "GHOST: " + super.toString();
	}
}