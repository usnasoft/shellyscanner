package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Arrays;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import tools.jackson.databind.JsonNode;

/**
 
import tools.jackson.databind.JsonNode;* Circuit Breaker model
 */
// todo specific renderer (and, if later useful, interface) with "isLocked" evidence; removal of "isInputOn"
public class CBreaker implements /*DeviceModule*/RelayInterface {
	private final AbstractG2Device parent;
	private String name;
	private boolean isOn;
	private boolean isLocked;
	private String source;
	
	public CBreaker(AbstractG2Device parent) {
		this.parent = parent;
	}
	
	public void fillSettings(JsonNode cbConfiguration) {
		name = cbConfiguration.get("name").asString("");
	}
	
	public void fillStatus(JsonNode cbStatus) {
		isOn = cbStatus.get("output").booleanValue(false);
		source = cbStatus.get("source").asString("-");
		isLocked = cbStatus.get("safety").booleanValue(false);
	}

	@Override
	public String getLabel() {
		return (name == null || name.isEmpty()) ? parent.getName() : name;
	}

	public String getName() {
		return name;
	}

	// output: only accepts false, otherwise an error is returned. The breaker lever can not be engaged remotely!
	public boolean toggle() throws IOException {
		change(! isOn);
		return isOn;
	}

	// output: only accepts false, otherwise an error is returned. The breaker lever can not be engaged remotely!
	public void change(boolean on) throws IOException {
		if(parent.postCommand("CB.Set", "{\"id\":0,\"output\":" + on + "}") == null) {
			isOn = on;
			source = Devices.SCANNER_AGENT;
		}
	}

	public boolean isOn() {
		return isOn;
	}
	
	@Override
	public String getLastSource() {
		return source;
	}

	public boolean isLocked() {
		return isLocked;
	}
	
	// to be removed on RelayInterface removal (?)
	@Override
	public boolean isInputOn() {
		return false;
	}
	
	public static String[] getInfoRequests(String [] cmd) {
		String[] newArray = Arrays.copyOf(cmd, cmd.length + 1);
		newArray[cmd.length] = "/rpc/CB.GetLog?id=0";
		return newArray;
	}
	
	public String restore(JsonNode config) {
		//todo test
		return parent.postCommand("CB.SetConfig", RestoreUtil.createIndexedRestoreNode(config, "cb", 0));
	}
	
	@Override
	public String toString() {
		return getLabel() + "-" + (isOn ? "ON" : "OFF");
	}
}