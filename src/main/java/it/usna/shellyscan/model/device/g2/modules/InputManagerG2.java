package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.InpuManager;
import tools.jackson.databind.JsonNode;

/**
 * InputResetManager model implementation for G2+
 */
public class InputManagerG2 implements InpuManager {
	private final AbstractG2Device parent;
	private Status mode = Status.NOT_APPLICABLE;
	private ArrayList<Integer> ids = new ArrayList<>();
	
	public InputManagerG2(AbstractG2Device d) throws IOException {
		this(d, d.getJSON("/rpc/Shelly.GetConfig"));
	}
	
	/**
	 *  use this constructor if "settings" is known in order to decrease the device effort
	 * @param d the device
	 * @param settings JsonNode configuration node
	 */
	public InputManagerG2(AbstractG2Device d, JsonNode config) {
		this.parent = d;
		Status currentMode;
		for(Entry<String, JsonNode> node: config.properties()) {
			String name;
			int id;
			if((name = node.getKey()).startsWith("input:") && (id = Integer.parseInt(name.split(":")[1])) < 100) {
				JsonNode reset = node.getValue().path("factory_reset");
				if(reset.isMissingNode()) {
					currentMode = Status.NOT_APPLICABLE;
				} else if(reset.booleanValue(false)) {
					currentMode = Status.TRUE;
					ids.add(id);
				} else {
					currentMode = Status.FALSE;
					ids.add(id);
				}
				if(mode == Status.TRUE && currentMode == Status.FALSE || mode == Status.FALSE && currentMode == Status.TRUE) {
					mode = Status.MIX;
					break;
				} else if(currentMode != Status.NOT_APPLICABLE) {
					mode = currentMode;
				}
			}
		}
	}

	@Override
	public Status getVal() {
		return mode;
	}
	
	@Override
	public Boolean getValAsBoolean() {
		if(mode == Status.TRUE) {
			return true;
		} else if(mode == Status.FALSE) {
			return false;
		} else {
			return null;
		}
	}
	
	@Override
	public String enableReset(boolean enable) {
		if(mode == Status.NOT_APPLICABLE) {
			return "notApplicable";
		} else {
			String msg = null;
			for(int id: ids) {
				String localMsg = parent.postCommand("Input.SetConfig", "{\"id\":" + id + ",\"config\":{\"factory_reset\":" + enable + "}");
				if(localMsg != null) {
					msg = localMsg;
				}
			}
			return msg;
		}
	}
}