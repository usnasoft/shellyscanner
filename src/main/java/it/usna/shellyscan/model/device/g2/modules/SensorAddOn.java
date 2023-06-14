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
	
	public SensorAddOn(JsonNode peripherals) {
		ArrayList<Meters.Type> types = new ArrayList<>();
	
		Iterator<String> digIn = ((ObjectNode)peripherals.get("digital_in")).fieldNames();
		if(digIn.hasNext()) {
			extID = digIn.next();
			types.add(Meters.Type.EXS);
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
		} catch (RuntimeException e) {
			LOG.warn("Configuration changed?", e);
		}
	}
	
	public boolean isDigitalInputOn() {
		return extOn;
	}

	@Override
	public float getValue(Type t) {
		if(t == Meters.Type.EXS) {
			return extOn ? 1 : 0;
		} else {
			return 0; // todo
		}
	}
	
	public static String[] getInfoRequests(String [] cmd) {
		ArrayList<String> tmp = new ArrayList<>(Arrays.asList(cmd));
		tmp.add("/rpc/SensorAddon.GetPeripherals");
		return tmp.toArray(new String[tmp.size()]);
	}
}

// https://shelly-api-docs.shelly.cloud/gen2/Addons/ShellySensorAddon/
// https://kb.shelly.cloud/knowledge-base/shelly-plus-add-on