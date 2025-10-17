package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.DeviceOfflineException;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class Script {
	private final AbstractG2Device device;
	private String name;
	private final int id;
	private boolean enabled;
	private boolean running;

	private Script(AbstractG2Device device, JsonNode script) {
		this.device = device;
		name = script.get("name").asText();
		id = script.get("id").asInt();
		enabled = script.get("enable").asBoolean();
		running = script.path("running").asBoolean(false);
	}

	private Script(AbstractG2Device device, int id) throws IOException {
		this(device, device.getJSON("/rpc/Script.GetConfig?id=" + id));
	}
	
	public static List<Script> list(AbstractG2Device device) throws IOException {
		return list(device, device.getJSON("/rpc/Script.List"));
	}
	
	public static List<Script> list(AbstractG2Device device, JsonNode jsonList) {
		List<Script> ret = new ArrayList<>();
		for(JsonNode scr: jsonList.get("scripts")) {
			ret.add(new Script(device, scr));
		}
		return ret;
	}

	public static Script create(AbstractG2Device device, String name) throws IOException {
		JsonNode id;
		if(name != null) {
			JsonStringEncoder encoder = JsonStringEncoder.getInstance();
			id = device.getJSON("Script.Create", "{\"name\":\"" + (new String(encoder.quoteAsString(name))) + "\"}");
		} else {
			id = device.getJSON("Script.Create", "{}");
		}
		return new Script(device, id.get("id").asInt());
	}

	public String getName() {
		return name;
	}

	public String setName(String name) {
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		String ret = device.postCommand("Script.SetConfig", "{\"id\":" + id + ",\"config\":{\"name\":\"" + (new String(encoder.quoteAsString(name))) + "\"}}");
		if(ret == null) {
			this.name = name;
		}
		return ret;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String setEnabled(boolean enabled) {
		String ret = device.postCommand("Script.SetConfig", "{\"id\":" + id + ",\"config\":{\"enable\":" + enabled + "}}");
		if(ret == null) {
			this.enabled = enabled;
		}
		return ret;
	}
	
	public boolean isRunning() {
		return running;
	}

	public int getId() {
		return id;
	}

	public String getCode() throws IOException {
		try {
			return device.getJSON("/rpc/Script.GetCode?id=" + id).get("data").asText().replaceAll("\\r+\\n", "\n");
		} catch(IOException e) {
			if(e instanceof DeviceOfflineException) {
				throw e;
			}
			return "";
		}
	}
	
	/* warning: segmentation before "quoteAsString" or escapes could be divided */
	public String putCode(String code) {
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		for (int start = 0; start < code.length(); start += 1024) {
			String seg = code.substring(start, Math.min(start + 1024, code.length())).replaceAll("\\r+\\n", "\n");
			String append = (start > 0) ? ",\"append\":true" : "";
			String res = device.postCommand("Script.PutCode", "{\"id\":" + id + append + ",\"code\":\"" + new String(encoder.quoteAsString(seg)) + "\"}");
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			if(res != null) {
				return res;
			}
		}
		return null;
	}

	public void delete() throws IOException {
		device.getJSON("/rpc/Script.Delete?id=" + id);
	}

	// response example: {"was_running":true}
	public void run() throws IOException {
		try {
			device.getJSON("/rpc/Script.Start?id=" + id);
			running = true;
		} catch(IOException e) {
			running = false;
			throw e;
		}
	}

	public void stop() throws IOException {
		device.getJSON("/rpc/Script.Stop?id=" + id);
		running = false;
	}
	
	public static void restoreAll(AbstractG2Device device, Map<String, JsonNode> backupJsons, final long delay, boolean overrideScripts, boolean enableScriptsIfWasEnabled, List<String> errors) throws InterruptedException {
		try {
			JsonNode scriptsBackup = backupJsons.get("Script.List.json");
			if(scriptsBackup != null && scriptsBackup.path("scripts").size() > 0) {
				//check for existing scripts
				TimeUnit.MILLISECONDS.sleep(delay);
				JsonNode existingScripts = device.getJSON("/rpc/Script.List").get("scripts");
				HashMap<String, Integer> existingScriptsNamesIds = new HashMap<>();
				for(JsonNode existingScript: existingScripts) {
					existingScriptsNamesIds.put(existingScript.get("name").asText(), existingScript.get("id").asInt());
				}

				for(JsonNode jsonScript: scriptsBackup.get("scripts")) {
					String writeToScriptName = jsonScript.get("name").asText();
					if(existingScriptsNamesIds.containsKey(writeToScriptName) && overrideScripts == false) {
						writeToScriptName = writeToScriptName + "_restored";
						//if this also exists dynamically append numbers (counter of already existing with same name) to the name
						for(int i = 1; existingScriptsNamesIds.containsKey(writeToScriptName); i++) {
							writeToScriptName = jsonScript.get("name").asText() + "_restored" + i;
						}
					}
					String code = backupJsons.get(jsonScript.get("name").asText() + ".mjs.json").get("code").asText();
					if(code != null) {
						TimeUnit.MILLISECONDS.sleep(delay);
						Script script;
						if(existingScriptsNamesIds.containsKey(writeToScriptName)) {
							script = new Script(device, existingScriptsNamesIds.get(writeToScriptName));
							errors.add(script.putCode(code));
						} else {
							script = Script.create(device, writeToScriptName);
							errors.add(script.putCode(code));
						}
						TimeUnit.MILLISECONDS.sleep(delay);
						errors.add(script.setEnabled(enableScriptsIfWasEnabled && jsonScript.get("enable").asBoolean())); //edge case: might make problems if already 3 scripts are enabled and the user has 3 scripts enabled in the backup and wants to restore without override
					}
				}
			}
		} catch(IOException e) {
			errors.add(e.getMessage());
		}
	}

	@Override
	public String toString() {
		return name;
	}
}