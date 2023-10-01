package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.DeviceOfflineException;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class Script {
	private final AbstractG2Device device;
	private String name;
	private final int id;
	private boolean enabled;
	private boolean running;

	public Script(AbstractG2Device device, JsonNode script) {
		this.device = device;
		name = script.get("name").asText();
		id = script.get("id").asInt();
		enabled = script.get("enable").asBoolean();
		running = script.path("running").asBoolean(false);
	}

	public Script(AbstractG2Device device, int id) throws IOException {
		this(device, device.getJSON("/rpc/Script.GetConfig?id=" + id));
	}

	public static Script create(AbstractG2Device device, String name) throws IOException {
		JsonNode id;
		if(name != null) {
			JsonStringEncoder e = JsonStringEncoder.getInstance();
			id = device.getJSON("Script.Create", "{\"name\":\"" + (new String(e.quoteAsString(name))) + "\"}");
		} else {
			id = device.getJSON("Script.Create", "{}");
		}
		return new Script(device, id.get("id").asInt());
	}

	public static JsonNode list(AbstractG2Device device) throws IOException {
		JsonNode sl = device.getJSON("/rpc/Script.List");
		return sl.get("scripts");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		JsonStringEncoder e = JsonStringEncoder.getInstance();
		device.postCommand("Script.SetConfig", "{\"id\":" + id + ",\"config\":{\"name\":\"" + (new String(e.quoteAsString(name))) + "\"}}");
		this.name = name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		device.postCommand("Script.SetConfig", "{\"id\":" + id + ",\"config\":{\"enable\":" + enabled + "}}");
		this.enabled = enabled;
	}
	
	public boolean isRunning() {
		return running;
	}

	//		public int getId() {
	//			return id;
	//		}

	public String getCode() throws IOException {
		try {
			return device.getJSON("/rpc/Script.GetCode?id=" + id).get("data").asText();
		} catch(IOException e) {
			if(e instanceof DeviceOfflineException) {
				throw e;
			}
			return "";
		}
	}

	public String putCode(String code) {
		JsonStringEncoder e = JsonStringEncoder.getInstance();
		for (int start = 0; start < code.length(); start += 1024) {
			String seg = code.substring(start, Math.min(code.length(), start + 1024));
			String append = (start > 0) ? ",\"append\":true" : "";
			String res = device.postCommand("Script.PutCode", "{\"id\":" + id + append + ",\"code\":\"" + (new String(e.quoteAsString(seg))) + "\"}");
			if(res != null) {
				return res;
			}
		}
		return null;
	}

	public void delete() throws IOException {
		device.getJSON("/rpc/Script.Delete?id=" + id);
	}

	public void run() throws IOException {
		device.getJSON("/rpc/Script.Start?id=" + id);
		running = true;
	}

	public void stop() throws IOException {
		device.getJSON("/rpc/Script.Stop?id=" + id);
		running = false;
	}

	@Override
	public String toString() {
		return name;
	}
}