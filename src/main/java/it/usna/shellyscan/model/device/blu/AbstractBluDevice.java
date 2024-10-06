package it.usna.shellyscan.model.device.blu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.blu.modules.Sensor;
import it.usna.shellyscan.model.device.blu.modules.SensorsCollection;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

public abstract class AbstractBluDevice extends ShellyAbstractDevice {
	public final static String GENERATION = "blu";
	private final static Logger LOG = LoggerFactory.getLogger(Devices.class);
	protected final ShellyAbstractDevice parent;
//	protected WebSocketClient wsClient;
	protected final String componentIndex;
	protected String localName;
	protected SensorsCollection sensors;
	private Meters[] meters;
	
	private final static String DEVICE_PREFIX = "bthomedevice:";
	private final static String SENSOR_PREFIX = "bthomesensor:";
	
	protected AbstractBluDevice(ShellyAbstractDevice parent, JsonNode info, String index) {
		super(new BluInetAddressAndPort(parent.getAddressAndPort()));
		this.parent = parent;
		this.componentIndex = index;
		final JsonNode config = info.path("config");
		this.mac = config.path("addr").asText();
		fillSettings(config);
		fillStatus(info.path("status"));
	}
	
	public void init(HttpClient httpClient/*, WebSocketClient wsClient*/) throws IOException {
		this.httpClient = httpClient;
//		this.wsClient = wsClient;
		sensors = new SensorsCollection(this);
		meters = new Meters[] {sensors};
	}
	
	public ShellyAbstractDevice getParent() {
		return parent;
	}
	
	public String getIndex() {
		return componentIndex;
	}
	
	@Override
	public String getTypeID() {
		return localName;
	}
	
	@Override
	public Status getStatus() {
		return parent.getStatus();
	}

	@Override
	// refreshStatus also refreshes settings so this method does nothing
	public void refreshSettings() throws IOException {
		// fillSettings(getJSON("/rpc/BTHomeDevice.GetConfig?id=" + componentIndex));
	}
	
	@Override
	public void refreshStatus() throws IOException {
		JsonNode components = getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components");
		String k;
		for(JsonNode comp: components) {
			if(comp.path("key").textValue().equals(DEVICE_PREFIX + componentIndex)) {
				fillSettings(comp.path("config"));
				fillStatus(comp.path("status"));
			} else if((k = comp.path("key").textValue()).startsWith(SENSOR_PREFIX)) {
				int id = Integer.parseInt(k.substring(13));
				Sensor s = sensors.getSensor(id);
				if(s != null) {
					s.fillSConfig(comp.path("config"));
					s.fillStatus(comp.path("status"));
				}
			}
		}
	}
	
	protected void fillSettings(JsonNode config) {
		this.name = config.path("name").asText("");
	}
	
	protected void fillStatus(JsonNode status) {
		this.rssi = status.path("rssi").intValue();
//		this.battery = status.path("battery").intValue();
		this.lastConnection = status.path("last_updated_ts").intValue() * 1000L;
	}

	@Override
	public String[] getInfoRequests() {
		ArrayList<String> l = new ArrayList<String>(Arrays.asList(/*"/rpc/Shelly.GetComponents?dynamic_only=true",*/
				"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex));
		for(Sensor s: sensors.getSensors()) {
			l.add("()/rpc/BTHomeSensor.GetConfig?id=" + s.getId());
			l.add("()/rpc/BTHomeSensor.GetStatus?id=" + s.getId());
		}
		return l.toArray(String[]::new);
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public boolean backup(File file) throws IOException {
		JsonFactory jsonFactory = new JsonFactory();
		jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		ObjectMapper mapper = new ObjectMapper(jsonFactory);
		
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			// ShellyScanner.json
			ZipEntry entry = new ZipEntry("ShellyScannerBLU.json");
			out.putNextEntry(entry);
			ObjectNode usnaData = JsonNodeFactory.instance.objectNode();
			usnaData.put("index", componentIndex);
			usnaData.put("type", localName);
			mapper.writeValue(out, usnaData);
			out.closeEntry();
			
			sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
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
		JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
		String fileLocalName;
		if(usnaInfo == null || (fileLocalName = usnaInfo.path("type").asText("")).equals(localName) == false) {
			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
			return res;
		}
		final String fileComponentIndex = usnaInfo.path("index").asText();
		JsonNode components = backupJsons.get("Shelly.GetComponents.json").path("components");
		for(JsonNode comp: components) {
			if(comp.path("key").textValue().equals(DEVICE_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
				String fileMac = comp.path("config").path("addr").textValue();
				if(fileMac.equals(mac) == false) {
					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, fileLocalName + "-" + fileMac);
				}
				break;
			}
		}
		return res;
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) throws IOException {
		// TODO define
		// TODO remove from parent
		final ArrayList<String> errors = new ArrayList<>();
		return errors;
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