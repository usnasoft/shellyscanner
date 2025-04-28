package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.InputResetManager;

public class InputResetManagerG2 implements InputResetManager {
	private final AbstractG2Device parent;
	private Status mode = Status.NOT_APPLICABLE;
	private ArrayList<Integer> ids = new ArrayList<>();
	
	public InputResetManagerG2(AbstractG2Device d) throws IOException {
		this(d, d.getJSON("/rpc/Shelly.GetConfig"));
	}
	
	public InputResetManagerG2(AbstractG2Device d, JsonNode config) {
		this.parent = d;
		Status currentMode;
		for(Entry<String, JsonNode> node: config.properties()) {
			String name;
			int id;
			if((name = node.getKey()).startsWith("input:") && (id = Integer.parseInt(name.split(":")[1])) < 100) {
				JsonNode reset = node.getValue().path("factory_reset");
				if(reset.isMissingNode()) {
					currentMode = Status.NOT_APPLICABLE;
				} else if(reset.booleanValue()) {
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