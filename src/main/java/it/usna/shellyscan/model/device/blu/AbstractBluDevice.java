package it.usna.shellyscan.model.device.blu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
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
	private final static Logger LOG = LoggerFactory.getLogger(Devices.class);
	protected final ShellyAbstractDevice parent;
//	protected WebSocketClient wsClient;
	protected final String componentIndex;
//	private int battery;
	
	protected AbstractBluDevice(ShellyAbstractDevice parent, JsonNode info, String index) {
		super(new BluInetAddressAndPort(parent.getAddressAndPort()));
		this.parent = parent;
		this.componentIndex = index;
		final JsonNode config = info.path("config");
		this.mac = config.path("addr").asText();
		this.hostname = getTypeID() + "-" + mac;
		fillSettings(config);
		fillStatus(info.path("status"));
	}
	
	public void init(HttpClient httpClient/*, WebSocketClient wsClient*/) {
		this.httpClient = httpClient;
//		this.wsClient = wsClient;
	}
	
	public ShellyAbstractDevice getParent() {
		return parent;
	}
	
	public String getIndex() {
		return componentIndex;
	}
	
	@Override
	public Status getStatus() {
		return parent.getStatus();
	}

	@Override
	public void refreshSettings() throws IOException {
		fillSettings(getJSON("/rpc/BTHomeDevice.GetConfig?id=" + componentIndex));
	}
	
	@Override
	public void refreshStatus() throws IOException {
		fillStatus(getJSON("/rpc/BTHomeDevice.GetStatus?id=" + componentIndex));
	}
	
	protected void fillSettings(JsonNode config) {
		this.name = config.path("name").asText("");
	}
	
	protected void fillStatus(JsonNode status) {
		this.rssi = status.path("rssi").intValue();
//		this.battery = status.path("battery").intValue();
		this.lastConnection = status.path("last_updated_ts").intValue() * 1000L;
	}
	
//	public int getBattery() {
//		return battery;
//	}

	@Override
	public String[] getInfoRequests() {
		ArrayList<String> l = new ArrayList<String>(Arrays.asList(/*"/rpc/Shelly.GetComponents?dynamic_only=true",*/
				"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex));
		try {
			ArrayList<String> sensors = findSensorsID();
			sensors.stream().filter(s -> s.startsWith("bthomesensor:")).forEach(s -> {
				String index = s.substring(13);
				l.add("()/rpc/BTHomeSensor.GetConfig?id=" + index);
				l.add("()/rpc/BTHomeSensor.GetStatus?id=" + index);
			});
		} catch (IOException e) {
			LOG.error("BLU-getInfoRequests", e);
		}
		return l.toArray(String[]::new);
	}
	
	private ArrayList<String> findSensorsID() throws IOException {
		JsonNode objects = parent.getJSON("/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex).path("objects");
		final Iterator<JsonNode> compIt = objects.iterator();
		ArrayList<String> l = new ArrayList<>();
		while (compIt.hasNext()) {
			String comp = compIt.next().path("component").asText();
			if(comp != null) {
				l.add(comp);
			}
		}
		return l;
	}
	
	@Override
	public boolean backup(File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			ZipEntry entry = new ZipEntry("usna.json");
			out.putNextEntry(entry);
			out.write(("{\"index\": \"" + componentIndex + "\"}").getBytes());
			out.closeEntry();
			sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
		}
		return true;
	}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		return Collections.<RestoreMsg, Object>emptyMap();
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