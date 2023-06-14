package it.usna.shellyscan.model.device.g2.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.Meters;

public class SensorAddOn extends Meters {
	private final static Logger LOG = LoggerFactory.getLogger(Meters.class);
	
	public final static String ADDON_TYPE = "sensor";
	private Meters.Type[] supported;
	
	private String extID = null;
	private boolean extOn;
	
	private String analogID = null;
	private float analog;
	
	private String voltmeterID = null;
	private float volt;
	
	public SensorAddOn(JsonNode peripherals) {
		ArrayList<Meters.Type> types = new ArrayList<>();

		Iterator<String> digIn = ((ObjectNode)peripherals.get("digital_in")).fieldNames();
		if(digIn.hasNext()) {
			extID = digIn.next();
			types.add(Meters.Type.EXS);
		}
		Iterator<String> analogIn = ((ObjectNode)peripherals.get("analog_in")).fieldNames();
		if(analogIn.hasNext()) {
			analogID = digIn.next();
			types.add(Meters.Type.PERC);
		}
		Iterator<String> voltIn = ((ObjectNode)peripherals.get("voltmeter")).fieldNames();
		if(voltIn.hasNext()) {
			voltmeterID = digIn.next();
			types.add(Meters.Type.V);
		}
		supported = types.toArray(new Meters.Type[types.size()]);
	}
	
	@Override
	public Type[] getTypes() {
		return supported;
	}
	
	public void fillStatus(JsonNode status) {
		try {
			if(extID != null) {
				extOn = status.path(extID).get("state").asBoolean();
			}
			if(analogID != null) {
				analog = status.path(analogID).get("percent").floatValue();
			}
			if(voltmeterID != null) {
				volt = status.path(voltmeterID).get("voltage").floatValue();
			}
		} catch (RuntimeException e) {
			LOG.warn("Add-on configuration changed?", e);
		}
	}
	
	public boolean isDigitalInputOn() {
		return extOn;
	}
	
	public float getAnalog() {
		return analog;
	}
	
	public float getVoltage() {
		return volt;
	}

	@Override
	public float getValue(Type t) {
		if(t == Meters.Type.EXS) {
			return extOn ? 1f : 0f;
		} else if(t == Meters.Type.PERC) {
			return analog;
		} else if(t == Meters.Type.V) {
			return volt;
		} else {
			return 0; // todo
		}
	}
	
	public static String[] getInfoRequests(String [] cmd) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + 1);
		newArray[cmd.length] = "/rpc/SensorAddon.GetPeripherals";
		return newArray;
	}
}

// https://shelly-api-docs.shelly.cloud/gen2/Addons/ShellySensorAddon/
// https://kb.shelly.cloud/knowledge-base/shelly-plus-add-on