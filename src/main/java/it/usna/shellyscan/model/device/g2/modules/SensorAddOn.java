package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class SensorAddOn extends Meters {
	private final static Logger LOG = LoggerFactory.getLogger(Meters.class);
	
	public final static String ADDON_TYPE = "sensor";
	private Type[] supported;
	
	private String extT0ID, extT1ID, extT2ID, extT3ID, extT4ID;
	private float extT0, extT1, extT2, extT3, extT4;
	
	private String humidityID;
	private int humidity;
	
	private String switchID;
	private boolean switchOn;
	
	private String analogID;
	private float analog;
	
	private String voltmeterID;
	private float volt;
	
	public SensorAddOn(AbstractG2Device d) throws IOException {
		try {
			JsonNode peripherals = d.getJSON("/rpc/SensorAddon.GetPeripherals");
			ArrayList<Meters.Type> types = new ArrayList<>();
			
			ObjectNode dht22Node = ((ObjectNode)peripherals.get("dht22"));
			if(dht22Node.size() > 0) {
				Iterator<String> dht22 = dht22Node.fieldNames();
				while(dht22.hasNext()) {
					String par = dht22.next();
					if(par.startsWith("temperature")) {
						extT0ID = par;
					} else if(par.startsWith("humidity")) {
						humidityID = par;
					}
				}
				types.add(Type.T);
				types.add(Type.H);
			}
			ObjectNode ds18b20Node = ((ObjectNode)peripherals.get("ds18b20"));
			if(ds18b20Node.size() > 0) {
				Iterator<String> temp = ds18b20Node.fieldNames();
				for(int i = 0; temp.hasNext(); i++) {
					if(i == 0) extT0ID = temp.next();
					else if(i == 1) extT1ID = temp.next();
					else if(i == 2) extT2ID = temp.next();
					else if(i == 3) extT3ID = temp.next();
					else if(i == 4) extT4ID = temp.next();
				}
			}
			
			Iterator<String> digIn = ((ObjectNode)peripherals.get("digital_in")).fieldNames();
			if(digIn.hasNext()) {
				switchID = digIn.next();
				types.add(Type.EXS);
			}
			Iterator<String> analogIn = ((ObjectNode)peripherals.get("analog_in")).fieldNames();
			if(analogIn.hasNext()) {
				analogID = analogIn.next();
				types.add(Type.PERC);
			}
			Iterator<String> voltIn = ((ObjectNode)peripherals.get("voltmeter")).fieldNames();
			if(voltIn.hasNext()) {
				voltmeterID = voltIn.next();
				types.add(Type.V);
			}
			supported = types.toArray(new Type[types.size()]);
		} catch (RuntimeException e) {
			supported = new Type[0];
			LOG.error("Add-on init error", e);
		}
	}
	
	@Override
	public Type[] getTypes() {
		return supported;
	}
	
	public void fillStatus(JsonNode status) {
		try {
			if(switchID != null) {
				switchOn = status.path(switchID).get("state").asBoolean();
			}
			if(analogID != null) {
				analog = status.path(analogID).get("percent").floatValue();
			}
			if(voltmeterID != null) {
				volt = status.path(voltmeterID).get("voltage").floatValue();
			}
			if(extT0ID != null) {
				volt = status.path(extT0ID).get("tC").floatValue();
			}
			if(extT1ID != null) {
				volt = status.path(extT1ID).get("tC").floatValue();
			}
			if(extT2ID != null) {
				volt = status.path(extT2ID).get("tC").floatValue();
			}
			if(extT3ID != null) {
				volt = status.path(extT3ID).get("tC").floatValue();
			}
			if(extT4ID != null) {
				volt = status.path(extT4ID).get("tC").floatValue();
			}
			if(humidityID != null) {
				volt = status.path(humidityID).get("rh").floatValue();
			}
		} catch (RuntimeException e) {
			LOG.warn("Add-on configuration changed?", e);
		}
	}
	
	public boolean isDigitalInputOn() {
		return switchOn;
	}
	
	public float getAnalog() {
		return analog;
	}
	
	public float getVoltage() {
		return volt;
	}
	
	public float getTemp0() {
		return extT0;
	}
	
	public float getTemp1() {
		return extT1;
	}
	
	public float getTemp2() {
		return extT2;
	}
	
	public float getTemp3() {
		return extT3;
	}
	
	public float getTemp4() {
		return extT4;
	}
	
	public float gethumidity() {
		return humidity;
	}

	@Override
	public float getValue(Type t) {
		switch(t) {
		case EXS: return switchOn ? 1f : 0f;
		case PERC: return analog;
		case V: return volt;
		case T: return extT0;
		case TX1: return extT1;
		case TX2: return extT2;
		case TX3: return extT3;
		case TX4: return extT4;
		case H: return humidity;
		default: return 0;
		}
	}
	
	public static String[] getInfoRequests(String [] cmd) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + 1);
		newArray[cmd.length] = "/rpc/SensorAddon.GetPeripherals";
		return newArray;
	}
	
	public static void enable(AbstractG2Device d, boolean enable) {
		d.postCommand("Sys.SetConfig", "{\"config\":{\"device\":{\"addon_type\":" + (enable ? "\"sensor\"" : "null") + "}}}");
	}
	
//	public static String restore(AbstractG2Device d, JsonNode config) {
//		return null;
//	}
}

// https://shelly-api-docs.shelly.cloud/gen2/Addons/ShellySensorAddon/
// https://kb.shelly.cloud/knowledge-base/shelly-plus-add-on