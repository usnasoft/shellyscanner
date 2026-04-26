package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.meters.Meters.Type;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Pro Sensor add-on model
 * @author usna
 */
public class SensorAddOnPro {
	private static final Logger LOG = LoggerFactory.getLogger(SensorAddOnPro.class);
	public static final String BACKUP_SECTION = "SensorAddon.GetPeripherals.json";
	public static final String ADDON_TYPE = "sensor";
	private Meters.Type[][] supported = new Meters.Type[2][];

	private String[] extT0ID = new String[2];
	private String[] extT1ID = new String[2];
	private String[] extT2ID = new String[2];
	private String[] extT3ID = new String[2];
	private String[] extT4ID = new String[2];
	
	private float[] extT0 = new float[2];
	private float[] extT1 = new float[2];
	private float[] extT2 = new float[2];
	private float[] extT3 = new float[2];
	private float[] extT4 = new float[2];
	
	private String[] extT0Name = new String[2];
	private String[] extT1Name = new String[2];
	private String[] extT2Name = new String[2];
	private String[] extT3Name = new String[2];
	private String[] extT4Name = new String[2];

	private String[] humidityID = new String[2];
	private float[] humidity = new float[2];
	private String[]  humidityName = new String[2];

	private String[] digitalInputID = new String[2];
	private boolean[] digitalInputOn = new boolean[2];
	private String[] digitalInputName = new String[2];

	private String analogID[] = new String[2];
	private float[] analog = new float[2];
	private String[] analogName = new String[2];

	private String[] voltmeterID = new String[2];
	private float[] volt = new float[2];
	private String[] voltmeterName = new String[2];
	private boolean xVoltSupported[] = {false, false}; // custom expression
	private float[] xVolt = new float[2];
	private String[] xVoltUnit = new String[2];
	
	private Meters[] meters;
	
	private Relay digitalOut;

	public SensorAddOnPro(AbstractG2Device d) throws IOException {
		try {
			JsonNode peripherals = d.getJSON("/rpc/SensorAddon.GetPeripherals");
			List<List<Meters.Type>> suppertedList = List.of(new ArrayList<Meters.Type>(), new ArrayList<Meters.Type>());

			if(peripherals.get("dht22") instanceof ObjectNode dht22Node && dht22Node.size() > 0) {
				for(Entry<String, JsonNode> p: dht22Node.properties()) {
					String peripheralId = p.getKey();
					if(peripheralId.startsWith("temperature")) {
						int group = p.getValue().get("io").intValue();
						extT0ID[group] = peripheralId;
						suppertedList.get(group).add(Meters.Type.T);
					} else if(peripheralId.startsWith("humidity")) {
						int group = p.getValue().get("io").intValue();
						humidityID[group] = peripheralId;
						suppertedList.get(group).add(Meters.Type.HD);
					}
					
				}
			}
			if(peripherals.get("ds18b20") instanceof ObjectNode ds18b20Node && ds18b20Node.size() > 0) {
				int[] tInd = new int[] {0, 0};
				for(Entry<String, JsonNode> p: ds18b20Node.properties()) {
					int group = p.getValue().get("io").intValue();
					if(tInd[group] == 0) {
						extT0ID[group] = p.getKey();
						suppertedList.get(group).add(Meters.Type.T);
					} else if(tInd[group] == 1) {
						extT1ID[group] = p.getKey();
						suppertedList.get(group).add(Meters.Type.T1);
					} else if(tInd[group] == 2) {
						extT2ID[group] = p.getKey();
						suppertedList.get(group).add(Meters.Type.T2);
					} else if(tInd[group] == 3) {
						extT3ID[group] = p.getKey();
						suppertedList.get(group).add(Meters.Type.T3);
					} else if(tInd[group] == 4) {
						extT4ID[group] = p.getKey();
						suppertedList.get(group).add(Meters.Type.T4);
					}
					tInd[group]++;
				}
			}
			if(peripherals.get("digital_in") instanceof ObjectNode digIn) {
				for(Entry<String, JsonNode> p: digIn.properties()) {
					int group = p.getValue().get("io").intValue();
					digitalInputID[group] = p.getKey();
					suppertedList.get(group).add(Meters.Type.EX);
				}
			}
			if(peripherals.get("analog_in") instanceof ObjectNode analogIn) {
				for(Entry<String, JsonNode> p: analogIn.properties()) {
					int group = p.getValue().get("io").intValue();
					analogID[group] = p.getKey();
					suppertedList.get(group).add(Meters.Type.PERC);
				}
			}
			if(peripherals.get("voltmeter") instanceof ObjectNode voltIn) {
				for(Entry<String, JsonNode> p: voltIn.properties()) {
					int group = p.getValue().get("io").intValue();
					voltmeterID[group] = p.getKey();
					suppertedList.get(group).add(Meters.Type.VL);
				}
			}
			if(peripherals.get("digital_out") instanceof ObjectNode sw) {
				for(Entry<String, JsonNode> p: sw.properties()) {
					if(p.getKey().startsWith("switch:")) {
						digitalOut = new Relay(d, Integer.parseInt(p.getKey().substring(7)));
						break;
					}
				}
			}
			
			supported[0] = suppertedList.get(0).toArray(Meters.Type[]::new);
			supported[1] = suppertedList.get(1).toArray(Meters.Type[]::new);
		} catch (RuntimeException e) {
			supported[0] = supported[1] = new Meters.Type[0];
			LOG.error("Add-on init error", e);
		}
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return supported[0];
					}

					@Override
					public float getValue(Type t) {
						return switch(t) {
						case EX -> digitalInputOn[0] ? 1f : 0f;
						case PERC -> analog[0];
						case V -> volt[0];
						case VX-> xVolt[0];
						case T -> extT0[0];
						case T1 -> extT1[0];
						case T2 -> extT2[0];
						case T3 -> extT3[0];
						case T4 -> extT4[0];
						case HD -> humidity[0];
						default -> 0;
						};
					}

					@Override
					public String getName(Type t) {
						return switch(t) {
						case EX -> digitalInputName[0];
						case PERC -> analogName[0];
						case V -> voltmeterName[0];
						case VX -> voltmeterName[0] + "[" + xVoltUnit[0] + "]";
						case T -> extT0Name[0];
						case T1 -> extT1Name[0];
						case T2 -> extT2Name[0];
						case T3 -> extT3Name[0];
						case T4 -> extT4Name[0];
						case HD -> humidityName[0];
						default -> "";
						};
					}
				},
				new Meters() {
					@Override
					public Type[] getTypes() {
						return supported[1];
					}

					@Override
					public float getValue(Type t) {
						return switch(t) {
						case EX -> digitalInputOn[1] ? 1f : 0f;
						case PERC -> analog[1];
						case VL -> volt[1];
						case VX-> xVolt[1];
						case T -> extT0[1];
						case T1 -> extT1[1];
						case T2 -> extT2[1];
						case T3 -> extT3[1];
						case T4 -> extT4[1];
						case HD -> humidity[1];
						default -> 0;
						};
					}

					@Override
					public String getName(Type t) {
						return switch(t) {
						case EX -> digitalInputName[1];
						case PERC -> analogName[1];
						case VL -> voltmeterName[1];
						case VX -> voltmeterName[1] + "[" + xVoltUnit[1] + "]";
						case T -> extT0Name[1];
						case T1 -> extT1Name[1];
						case T2 -> extT2Name[1];
						case T3 -> extT3Name[1];
						case T4 -> extT4Name[1];
						case HD -> humidityName[1];
						default -> "";
						};
					}
				}
		};
	}

	public Meters[] getMetersArray() {
		return meters;
	}
	
	public Relay getDigitalOut() {
		return digitalOut;
	}
	
	public void fillSettings(JsonNode configuration) throws IOException {
		try {
			JsonNode cnf;
			for(int i = 0; i < 2; i++) {
				if(digitalInputID[i] != null && (cnf = configuration.get(digitalInputID[i])) != null) {
					digitalInputName[i] = cnf.path("name").asString("");
				}
				if(analogID[i] != null && (cnf = configuration.get(analogID[i])) != null) {
					analogName[i] = cnf.path("name").asString("");
				}
				if(voltmeterID[i] != null && (cnf = configuration.get(voltmeterID[i])) != null) {
					voltmeterName[i] = cnf.path("name").asString("");
					xVoltUnit[i] = cnf.path("xvoltage").path("unit").asString(null);
				}
				if(extT0ID[i] != null && (cnf = configuration.get(extT0ID[i])) != null) {
					extT0Name[i] = cnf.path("name").asString("");
				}
				if(extT1ID[i] != null && (cnf = configuration.get(extT1ID[i])) != null) {
					extT1Name[i] = cnf.path("name").asString("");
				}
				if(extT2ID[i] != null && (cnf = configuration.get(extT2ID[i])) != null) {
					extT2Name[i] = cnf.path("name").asString("");
				}
				if(extT3ID[i] != null && (cnf = configuration.get(extT3ID[i])) != null) {
					extT3Name[i] = cnf.path("name").asString("");
				}
				if(extT4ID[i] != null && (cnf = configuration.get(extT4ID[i])) != null) {
					extT4Name[i] = cnf.path("name").asString("");
				}
				if(humidityID[i] != null && (cnf = configuration.get(humidityID[i])) != null) {
					humidityName[i] = cnf.path("name").asString("");
				}
				if(digitalOut != null) {
					digitalOut.fillSettings(configuration.get("switch:" + digitalOut.getIndex()));	
				}
			}
		} catch (RuntimeException e) {
			LOG.warn("Settings Add-on configuration changed?", e);
		}	
	}

	public void fillStatus(JsonNode status) {
		try {
			for(int i = 0; i < 2; i++) {
				if(digitalInputID != null) {
					digitalInputOn[i] = status.path(digitalInputID[i]).path("state").asBoolean(false);
				}
				if(analogID[i] != null) {
					analog[i] = status.path(analogID[i]).path("percent").floatValue(0);
				}
				if(voltmeterID[i] != null) {
					var voltNode = status.path(voltmeterID[i]);
					volt[i] = voltNode.path("voltage").floatValue(0f);
					var xVoltNode = voltNode.path("xvoltage");
					if(xVoltNode.isMissingNode()) {
						xVolt[i] = 0f;
						if(xVoltSupported[i]) {
							xVoltSupported[i] = false;
							var tempList = new ArrayList<Meters.Type>(List.of(supported[i]));
							tempList.remove(Meters.Type.VX);
							supported[i] = tempList.toArray(Type[]::new);
						}
					} else {
						xVolt[i] = xVoltNode.floatValue(0f);
						if(xVoltSupported[i] == false) {
							xVoltSupported[i] = true;
							var tempList = new ArrayList<Meters.Type>(List.of(supported[i]));
							tempList.add(Meters.Type.VX);
							supported[i] = tempList.toArray(Type[]::new);
						}
					}
				}
				if(extT0ID[i] != null) {
					extT0[i] = status.path(extT0ID[i]).path("tC").floatValue(0);
				}
				if(extT1ID[i] != null) {
					extT1[i] = status.path(extT1ID[i]).path("tC").floatValue(0);
				}
				if(extT2ID[i] != null) {
					extT2[i] = status.path(extT2ID[i]).path("tC").floatValue(0);
				}
				if(extT3ID != null) {
					extT3[i] = status.path(extT3ID[i]).path("tC").floatValue(0);
				}
				if(extT4ID[i] != null) {
					extT4[i] = status.path(extT4ID[i]).path("tC").floatValue(0);
				}
				if(humidityID[i] != null) {
					humidity[i] = status.path(humidityID[i]).path("rh").floatValue(0);
				}
				if(digitalOut != null) {
					digitalOut.fillStatus(status.get("switch:" + digitalOut.getIndex()));	
				}
			}
		} catch (RuntimeException e) {
			LOG.warn("Status Add-on configuration changed?", e);
		}
	}

	public static String[] getInfoRequests(String [] cmd) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + 1);
		newArray[cmd.length] = "/rpc/SensorAddon.GetPeripherals";
		return newArray;
	}
	
	public Meters[] addMetersArray(Meters ... baseMeters) {
		ArrayList<Meters> metersList = new ArrayList<Meters>(4);
		for(Meters m: baseMeters) {
			metersList.add(m);
		}
		for(Meters met: meters) {
			if(met.getTypes().length > 0) {
				metersList.add(met);
			}
		}
		return metersList.toArray(Meters[]::new);
	}

	private static String enable(AbstractG2Device d, boolean enable) {
		return d.postCommand("Sys.SetConfig", "{\"config\":{\"device\":{\"addon_type\":" + (enable ? "\"sensor\"" : "null") + "}}}");
	}

	private static String addSensor(AbstractG2Device d, String type, String id, JsonNode attrs) {
		if(attrs.has("addr")) {
			return d.postCommand("SensorAddon.AddPeripheral", "{\"type\":\"" + type + "\",\"attrs\":{\"cid\":" + id + ",\"io\":" + attrs.get("io").intValue() + ",\"addr\":\"" + attrs.get("addr").asString() + "\"}}");
		} else {
			return d.postCommand("SensorAddon.AddPeripheral", "{\"type\":\"" + type + "\",\"attrs\":{\"cid\":" + id + ",\"io\":" + attrs.get("io").intValue() + "}}");
		}
	}

	public static void restoreCheck(AbstractG2Device d, SensorAddOnPro addOn, Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) {
		JsonNode backupAddOn = backupJsons.get(BACKUP_SECTION);
		if(backupAddOn != null) {
			int backupNumSensors = 0;
			for(Map.Entry<String, JsonNode> entry: backupAddOn.properties()) {
				if(entry.getValue() != null && entry.getValue().isEmpty() == false) {
					backupNumSensors++;
				}
			}
			if(addOn == null && backupNumSensors > 0) { // NO addon on the device but addon on backup -> enable (must reboot and later install sensors)
				res.put(RestoreMsg.WARN_RESTORE_ADDON_ENABLE, null);// msg: Please reboot the device at the end of the restore process and restore again to install sensors
			} else if(addOn != null && (addOn.supported[0].length + addOn.supported[1].length) > 0 && backupNumSensors > 0) { // will restore configuration (if possible)
				try {
					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
					JsonNode devicePeripherals = d.getJSON("/rpc/SensorAddon.GetPeripherals");
					if(devicePeripherals.equals(backupAddOn) == false) {
						res.put(RestoreMsg.WARN_RESTORE_ADDON_CANT_INSTALL, null); // Sensors installation list will not be restored since one or more sensors are already configured on the destination device.
					}
				} catch (IOException | InterruptedException e) {
					LOG.error("SensorAddOn.restoreCheck", e);
				}
			} else if(addOn != null && (addOn.supported[0].length + addOn.supported[1].length) == 0 && backupNumSensors > 0) { // will install sensors
				res.put(RestoreMsg.WARN_RESTORE_ADDON_INSTALL, null); // msg: Please reboot the device at the end of restore process and restore again to restore full sensors configuration
			}
		}
	}

	public static void restore(AbstractG2Device d, SensorAddOnPro addOn, Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode backupAddOn = backupJsons.get(BACKUP_SECTION);
		if(backupAddOn == null && addOn != null) { // there is addon on the device but not on backup -> disable (must reboot)
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(enable(d, false));
		} else if(backupAddOn != null && addOn == null) { // NO addon on the device but addon on backup -> enable (must reboot)
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			errors.add(enable(d, true));
		} else if(backupAddOn != null && (addOn.supported[0].length + addOn.supported[1].length) == 0) { // addon exists on backup and on device but no sensor installed on the device -> install backup sensors (must reboot)
			for(Map.Entry<String, JsonNode> entry: backupAddOn.properties()) {
				if(entry.getValue() != null && entry.getValue().isEmpty() == false) {
					String sensor = entry.getKey();
					String prevIndex = "";
					for(Map.Entry<String, JsonNode> input: entry.getValue().properties()) {
						String inputKey = input.getKey();
						JsonNode attrs = input.getValue();
						TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
						String index = inputKey.substring(inputKey.indexOf(':') + 1);
						if(index.equals(prevIndex) == false) { // dht22 have 2 entries but must be added once
							prevIndex = index;
							errors.add(addSensor(d, sensor, index, attrs));
						}
					}
				}
			}
		} else if(backupAddOn != null) { // backupAddOn != null && addOn.getTypes().length > 0 -> restore sensor config
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			restoreAddoOnConfig(d, backupAddOn,  backupJsons.get("Shelly.GetConfig.json"), errors); // device must reboot before configuration can be restored
		}
	}
	
	private static void restoreAddoOnConfig(AbstractG2Device d, JsonNode backupAddOn, JsonNode backConfig, List<String> errors) throws InterruptedException {
		try {
			JsonNode config = d.getJSON("/rpc/Shelly.GetConfig");
			for(Map.Entry<String, JsonNode> entry: backupAddOn.properties()) {
				if(entry.getValue() != null && entry.getValue().isEmpty() == false) {
					for(Map.Entry<String, JsonNode> input: entry.getValue().properties()) {
						String inputKey = input.getKey();
						if(config.has(inputKey)) {
							TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
							String typeIdx[] = inputKey.split(":");
							if(typeIdx[0].equals("temperature")) {
								errors.add(d.postCommand("Temperature.SetConfig", RestoreUtil.createIndexedRestoreNode(backConfig, "temperature", Integer.parseInt(typeIdx[1]))));
							} else if(typeIdx[0].equals("humidity")) {
								errors.add(d.postCommand("Humidity.SetConfig", RestoreUtil.createIndexedRestoreNode(backConfig, "humidity", Integer.parseInt(typeIdx[1]))));
							} else if(typeIdx[0].equals("input")) {
								errors.add(d.postCommand("Input.SetConfig", RestoreUtil.createIndexedRestoreNode(backConfig, "input", Integer.parseInt(typeIdx[1]))));
							} else if(typeIdx[0].equals("voltmeter")) {
								errors.add(d.postCommand("Voltmeter.SetConfig", RestoreUtil.createIndexedRestoreNode(backConfig, "voltmeter", Integer.parseInt(typeIdx[1]))));
							} else if(typeIdx[0].equals("switch")) {
								errors.add(d.postCommand("Switch.SetConfig", RestoreUtil.createIndexedRestoreNode(backConfig, "switch", Integer.parseInt(typeIdx[1]))));
							}
						}
					}
				}
			}
		} catch(IOException e) {
			LOG.error("SensorAddOn.restoreConfig", e);
		}
	}
}