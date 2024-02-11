package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
			JsonStringEncoder encoder = JsonStringEncoder.getInstance();
			id = device.getJSON("Script.Create", "{\"name\":\"" + (new String(encoder.quoteAsString(name))) + "\"}");
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
			return device.getJSON("/rpc/Script.GetCode?id=" + id).get("data").asText();
		} catch(IOException e) {
			if(e instanceof DeviceOfflineException) {
				throw e;
			}
			return "";
		}
	}
	
	public String putCode(String code) {
		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
		char codeC[] = encoder.quoteAsString(code);
		for (int start = 0; start < codeC.length; start += 1024) {
			String seg = new String(codeC, start, Math.min(codeC.length - start, 1024));
			String append = (start > 0) ? ",\"append\":true" : "";
			String res = device.postCommand("Script.PutCode", "{\"id\":" + id + append + ",\"code\":\"" + seg + "\"}");
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			if(res != null) {
				return res;
			}
		}
		return null;
	}
	
//	public String putCode(Reader code) throws IOException {
//		JsonStringEncoder encoder = JsonStringEncoder.getInstance();
//		char[] buffer = new char[1024];
//		boolean subsequent = false;
//		int length;
//		while((length = code.read(buffer, 0, 1024)) >= 0) {
//			String append = subsequent ? ",\"append\":true" : "";
//			String res = device.postCommand("Script.PutCode", "{\"id\":" + id + append + ",\"code\":\"" + new String(encoder.quoteAsString(new String(buffer, 0, length))) + "\"}");
//			subsequent = true;
//			if(res != null) {
//				return res;
//			}
//		}
//		return null;	
//	}

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
	
//	public static void restoreCheckAll(AbstractG2Device device, Map<String, JsonNode> backupJsons, EnumMap<Restore, String> res) throws IOException {
//		JsonNode scripts = backupJsons.get("Script.List.json");
//		if(scripts != null && scripts.path("scripts").size() > 0) {
//			List<String> scriptsEnabledByDefault = new ArrayList<>();
//			List<String> scriptsWithSameName = new ArrayList<>();
//			JsonNode existingScripts = Script.list(device);
//			List<String> existingScriptsNames = new ArrayList<>();
//			for(JsonNode existingScript: existingScripts) {
//				existingScriptsNames.add(existingScript.get("name").asText());
//			}
//			for(JsonNode jsonScript: scripts.get("scripts")) {
//				if(existingScriptsNames.contains(jsonScript.get("name").asText()))
//					scriptsWithSameName.add(jsonScript.get("name").asText());
//				if(jsonScript.get("enable").asBoolean())
//					scriptsEnabledByDefault.add(jsonScript.get("name").asText());
//			}
//			if(scriptsWithSameName.isEmpty() == false) {
//				res.put(Restore.QUESTION_RESTORE_SCRIPTS_OVERRIDE, String.join(", ", scriptsWithSameName));
//			}
//			if(scriptsEnabledByDefault.isEmpty() == false) {
//				res.put(Restore.QUESTION_RESTORE_SCRIPTS_ENABLE_LIKE_BACKED_UP, String.join(", ", scriptsEnabledByDefault));
//			}
//		}	
//	}
	
	public static void restoreAll(AbstractG2Device device, Map<String, JsonNode> backupJsons, final long delay, boolean overrideScripts, boolean enableScriptsIfWasEnabled, List<String> errors) throws InterruptedException {
		try {
			JsonNode scriptsBackup = backupJsons.get("Script.List.json");
			if(scriptsBackup != null && scriptsBackup.path("scripts").size() > 0) {
				//check for existing scripts
				TimeUnit.MILLISECONDS.sleep(delay);
				JsonNode existingScripts = Script.list(device);
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