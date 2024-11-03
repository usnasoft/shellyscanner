package it.usna.shellyscan.model.device.blu;

import java.io.IOException;

import org.eclipse.jetty.client.HttpClient;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

public abstract class AbstractBluDevice extends ShellyAbstractDevice {
	public final static String GENERATION = "blu";
//	private final static Logger LOG = LoggerFactory.getLogger(AbstractBluDevice.class);
	protected final AbstractG2Device parent;
//	protected WebSocketClient wsClient;
	protected final String componentIndex;
//	protected String localName;
//	protected SensorsCollection sensors;
//	private Meters[] meters;
	
	public final static String DEVICE_KEY_PREFIX = DynamicComponents.BTHOME_DEVICE + ":"; // "bthomedevice:";
	public final static String SENSOR_KEY_PREFIX = DynamicComponents.BTHOME_SENSOR + ":"; // "bthomesensor:";
	public final static String GROUP_KEY_PREFIX = DynamicComponents.GROUP_TYPE + ":"; // "group:";
	
	/**
	 * AbstractBluDevice constructor
	 * @param parent
	 * @param info
	 * @param index
	 */
	protected AbstractBluDevice(AbstractG2Device parent, JsonNode compInfo, String index) {
		super(new BluInetAddressAndPort(parent.getAddressAndPort(), Integer.parseInt(index)));
		this.parent = parent;
		this.componentIndex = index;
		final JsonNode config = compInfo.path("config");
		this.mac = config.path("addr").asText();
	}
	
	public void init(HttpClient httpClient/*, WebSocketClient wsClient*/) throws IOException {
		this.httpClient = httpClient;
//		this.wsClient = wsClient;
		refreshStatus();
		refreshSettings();
	}

//	@Override
//	public BluInetAddressAndPort getAddressAndPort() {
//		return (BluInetAddressAndPort)addressAndPort;
//	}
	
	public ShellyAbstractDevice getParent() {
		return parent;
	}
	
	public String getIndex() {
		return componentIndex;
	}
	
	@Override
	public Status getStatus() {
		if(rssi < 0) {
			return parent.getStatus();
		} else if(parent.getStatus() == Status.NOT_LOOGGED) {
			return Status.NOT_LOOGGED;
		} else {
			return Status.OFF_LINE;
		}
	}
	
	public String postCommand(final String method, String payload) {
		return parent.postCommand(method, payload);
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex};
	}

//	@Override
//	public boolean backup(File file) throws IOException {
//		JsonFactory jsonFactory = new JsonFactory();
//		jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
//		ObjectMapper mapper = new ObjectMapper(jsonFactory);
//		
//		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
//			// ShellyScanner.json
//			ZipEntry entry = new ZipEntry("ShellyScannerBLU.json");
//			out.putNextEntry(entry);
//			ObjectNode usnaData = JsonNodeFactory.instance.objectNode();
//			usnaData.put("index", componentIndex);
//			usnaData.put("type", localName);
//			mapper.writeValue(out, usnaData);
//			out.closeEntry();
//			
//			sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
//		} catch(InterruptedException e) {
//			LOG.error("backup", e);
//		}
//		return true;
//	}

//	@Override
//	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
//		EnumMap<RestoreMsg, Object> res = new EnumMap<>(RestoreMsg.class);
//		JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
//		String fileLocalName;
//		if(usnaInfo == null || (fileLocalName = usnaInfo.path("type").asText("")).equals(localName) == false) {
//			res.put(RestoreMsg.ERR_RESTORE_MODEL, null);
//			return res;
//		}
//		final String fileComponentIndex = usnaInfo.get("index").asText();
//		JsonNode fileComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
//		for(JsonNode fileComp: fileComponents) {
//			if(fileComp.path("key").textValue().equals(DEVICE_KEY_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
//				String fileMac = fileComp.path("config").path("addr").textValue();
//				if(fileMac.equals(mac) == false) {
//					res.put(RestoreMsg.PRE_QUESTION_RESTORE_HOST, fileLocalName + "-" + fileMac);
//				}
//				break;
//			}
//		}
//		return res;
//	}
//
//	@Override
//	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) {
//		final ArrayList<String> errors = new ArrayList<>();
//		try {
//			createSensors(); // in case they have been altered (from the web GUI)
//			
//			// Store groups components into HashMap<String, ArrayNode> groups (they will be removed deleting a sensor)
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			JsonNode currentComponents = parent.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true");
//			HashMap<String, ArrayNode> groups = new HashMap<>();
//			for (JsonNode storedComp: currentComponents.path("components")) {
//				String key = storedComp.get("key").asText();
//				if(key.startsWith(GROUP_KEY_PREFIX)) {
//					groups.put(key, (ArrayNode)storedComp.path("status").get("value"));
//				}
//			}
//
//			JsonNode usnaInfo = backupJsons.get("ShellyScannerBLU.json");
//			String fileComponentIndex = usnaInfo.get("index").textValue();
//			JsonNode fileComponents = backupJsons.get("Shelly.GetComponents.json").path("components");
//			String fileAddr = null;
//			// BLU configuration: Device
//			for(JsonNode fileComp: fileComponents) {
//				if(fileComp.path("key").textValue().equals(DEVICE_KEY_PREFIX + fileComponentIndex)) { // find the component by fileComponentIndex
//					ObjectNode out = JsonNodeFactory.instance.objectNode();
//					out.put("id", Integer.parseInt(componentIndex)); // could be different
//					ObjectNode config = (ObjectNode)fileComp.path("config").deepCopy();
//					config.remove("id");
//					config.remove("addr"); // new (registered on host) addr could not be the stored addr
//					out.set("config", config);
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//					errors.add(parent.postCommand("BTHomeDevice.SetConfig", out));
//					fileAddr = fileComp.at("/config/addr").textValue();
//					break;
//				}
//			}
//
//			// Sensors
//			HashMap<String, String> sensorsDictionary = new HashMap<>(); // old-new key ("bthomesensor:200"-"bthomesensor:201")
//			JsonNode storedWebHooks = backupJsons.get("Webhook.List.json");
//			errors.add(sensors.deleteAll()); // deleting a sensor all related webhooks are also deleted
//			for(JsonNode fileComp: fileComponents) {
//				final String fileKey = fileComp.path("key").textValue();
//				if(fileKey.startsWith(SENSOR_KEY_PREFIX) && fileComp.at("/config/addr").textValue().equals(fileAddr)) {
//					ObjectNode out = JsonNodeFactory.instance.objectNode();
//					ObjectNode config = (ObjectNode)fileComp.path("config");
//					config.put("addr", this.mac);
//					out.set("config", config);
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//					String newKey = parent.getJSON("BTHome.AddSensor", out).get("added").textValue(); // BTHome.AddSensor -> {"added":"bthomesensor:200"}
//					Webhooks.restore(parent, fileKey, newKey, Devices.MULTI_QUERY_DELAY, storedWebHooks, errors); // Webhook.Create
//					
//					sensorsDictionary.put(fileKey, newKey);
//				}
//			}
//			
//			// restore groups
//			for(Map.Entry<String, ArrayNode> group: groups.entrySet()) {
//				ArrayNode conponents = group.getValue();
//				boolean change = false;
//				for(int i = 0; i < conponents.size(); i++) {
//					String newKey = sensorsDictionary.get(conponents.get(i).textValue());
//					if(newKey != null) {
//						change = true;
//						conponents.set(i, newKey);
//					}
//				}
//				if(change) {
//					ObjectNode grValue = JsonNodeFactory.instance.objectNode();
//					grValue.put("id", Integer.parseInt(group.getKey().substring(6)));
//					grValue.set("value", conponents);
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//					errors.add(parent.postCommand("Group.Set", grValue));
//				}
//			}
//			
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			createSensors();
//		} catch(IOException | RuntimeException | InterruptedException e) {
//			LOG.error("restore - RuntimeException", e);
//			errors.add(RestoreMsg.ERR_UNKNOWN.toString());
//		}
//		return errors;
//	}

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