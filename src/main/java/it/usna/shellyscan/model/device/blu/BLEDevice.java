package it.usna.shellyscan.model.device.blu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;
import tools.jackson.databind.JsonNode;

/**
 * NOT USED
 */
public class BLEDevice extends ShellyAbstractDevice {
	public static final String GENERATION = "blu";
	private String typeName;
	private ArrayList<ShellyAbstractDevice> gateWays = new ArrayList<>();

	public BLEDevice(AbstractG2Device parent, String mac, String index) { // index -> on ListInfos
		super(new BluInetAddressAndPort(parent.getAddressAndPort(), Integer.parseInt(index)));
		this.typeName = "Generic BLE";
		this.hostname = "BLE-" + mac;
//		this.lastConnection 
		gateWays.add(parent);
	}
	
	@Override
	public String getGeneration() {
		return GENERATION;
	}
	
	/**
	 * @param gw
	 * @param relayInfo {"name" : null, "model" : 0, "sdata" : { "fcd2" : "QABrAWTwFwDxBAQBAQ==" }, "mdata" : { }, "last_seen" : 1776717181}
	 */
	public void addGateway(ShellyAbstractDevice gw, int lastSeen) {
		gateWays.add(gw);
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public String[] getInfoRequests() {
		return new String[] {"/rpc/BLE.CloudRelay.ListInfos"};
	}

	@Override
	public String getTypeID() {
		return "BLE";
	}

	@Override
	public void refreshSettings() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshStatus() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean setDebugMode(LogMode mode, boolean enable) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String setCloudEnabled(boolean enable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reboot() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String setEcoMode(boolean eco) {
		return null;
	}

	@Override
	public FirmwareManager getFWManager() {
		return null;
	}

	@Override
	public WIFIManager getWIFIManager(Network net) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MQTTManager getMQTTManager() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LoginManager getLoginManager() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TimeAndLocationManager getTimeAndLocationManager() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputResetManager getInputResetManager() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean backup(Path file) throws IOException {
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
}
