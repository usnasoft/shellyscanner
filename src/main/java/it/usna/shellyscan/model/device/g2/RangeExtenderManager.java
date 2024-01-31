package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public class RangeExtenderManager {
//	private final AbstractG2Device d;
	private List<Integer> ports = new ArrayList<>();

	public RangeExtenderManager(AbstractG2Device d) throws IOException {
//		this.d = d;
		Status s = d.getStatus(); // get status since next call could loose NOT_LOOGGED (no login needed)
		JsonNode clients = d.getJSON("/rpc/WiFi.ListAPClients");
		if(clients.isNull() == false) {
			clients.get("ap_clients").forEach(c -> ports.add(c.get("mport").asInt()));
		}
		if(s == Status.NOT_LOOGGED) { // /rpc/WiFi.ListAPClients does not require login so status became ON_LINE
			d.setStatus(Status.NOT_LOOGGED);
		}
	}
	
	public int numExConnected() {
		return ports.size();
	}
	
	public List<Integer> getPorts() {
		return ports;
	}
	
	public static String enable(AbstractG2Device d, boolean enable) {
		if(enable) {
			return d.postCommand("WiFi.SetConfig", "{\"config\":{\"ap\":{\"enable\":true,\"range_extender\":{\"enable\":true}}}}");
		} else {
			return d.postCommand("WiFi.SetConfig", "{\"config\":{\"ap\":{\"range_extender\":{\"enable\":false}}}}");	
		}
	}
}