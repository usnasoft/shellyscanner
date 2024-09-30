package it.usna.shellyscan.model.device.blu;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

public abstract class AbstractBluDevice extends ShellyAbstractDevice {
	protected final ShellyAbstractDevice parent;
	private final String componentIndex;
	private int battery;
	
	protected AbstractBluDevice(ShellyAbstractDevice parent, JsonNode info, String index) throws IOException {
		super(parent.getAddressAndPort());
		this.parent = parent;
		this.componentIndex = index;
		this.hostname = getTypeID() + "-" + mac;
		final JsonNode config = info.path("config");
		this.mac = config.path("addr").asText();
		fillSettings(config);
		fillStatus(info.path("status"));
	}
	
	@Override
	public Status getStatus() {
		return Status.BLU;
	}

	public void fillSettings() throws IOException {
		fillSettings(parent.getJSON("/rpc/BTHomeDevice.GetConfig?id=" + componentIndex));
	}
	
	public void fillStatus() throws IOException {
		fillSettings(parent.getJSON("/rpc/BTHomeDevice.GetStatus?id=" + componentIndex));
	}
	
	protected void fillSettings(JsonNode config) {
		this.name = config.path("name").asText();
	}
	
	protected void fillStatus(JsonNode status) {
		this.rssi = status.path("rssi").intValue();
		this.battery = status.path("battery").intValue();
		this.lastConnection = status.path("last_updated_ts").intValue();
	}
	
	public int getBattery() {
		return battery;
	}
	
	@Override
	public JsonNode getJSON(final String command) throws IOException {
		return parent.getJSON(command);
	}

	@Override
	public String[] getInfoRequests() {
		// TODO define 
		return new String[] {"/rpc/Shelly.GetComponents"};
	}
	
	@Override
	public boolean backup(File file) throws IOException {
		// TODO define
		return false;
	}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		// TODO define - remove from parent
		return null;
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) throws IOException {
		// TODO define - remove from parent
		return null;
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
	public InputResetManager getInputResetManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return getTypeName() + "-" + name + ":" + mac;
	}
}

/*
{
    "key" : "bthomedevice:200",
    "status" : {
      "id" : 200,
      "rssi" : -63,
      "battery" : 100,
      "packet_id" : 209,
      "last_updated_ts" : 1726738130
    },
    "config" : {
      "id" : 200,
      "addr" : "b0:c7:xx:xx:xx:xx",
      "name" : "h&t",
      "key" : null,
      "meta" : {
        "ui" : {
          "view" : "regular",
          "local_name" : "SBHT-003C",
          "icon" : null
        }
      }
    }
  }
  
  "local_name" : "SBHT-003C" -> h&t
  "local_name" : "SBBT-002C" -> button
*/