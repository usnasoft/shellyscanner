package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.InputResetManager;

public class InputResetManagerG2 implements InputResetManager {
	private final AbstractG2Device parent;
	private Status mode = Status.NOT_APPLICABLE;
	
	public InputResetManagerG2(AbstractG2Device d) throws IOException {
		this(d, d.getJSON("/rpc/Shelly.GetConfig"));
	}
	
	public InputResetManagerG2(AbstractG2Device d, JsonNode config) {
		this.parent = d;
		Status currentMode;
		Iterator<Entry<String, JsonNode>> iter = ((ObjectNode)config).fields();
		while(iter.hasNext()) {
			Entry<String, JsonNode> node = iter.next();
			String name;
			if((name = node.getKey()).startsWith("input:") && Integer.parseInt(name.split(":")[1]) < 100) {
				JsonNode reset = node.getValue().path("factory_reset");
				if(reset.isMissingNode()) {
					currentMode = Status.NOT_APPLICABLE;
				} else if(reset.booleanValue()) {
					currentMode = Status.TRUE;
				} else {
					currentMode = Status.FALSE;
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
			try {
				JsonNode config = parent.getJSON("/rpc/Shelly.GetConfig");
				Iterator<Entry<String, JsonNode>> iter = ((ObjectNode)config).fields();
				String msg = null;
				while(iter.hasNext()) {
					Entry<String, JsonNode> node = iter.next();
					String name = node.getKey(); // input:0
					String nameId[] = name.split(":");
					if(nameId.length == 2 && nameId[0].equals("input") && Integer.parseInt(nameId[1]) < 100) {
						JsonNode reset = node.getValue().path("factory_reset");
						if(reset.isMissingNode() == false && reset.booleanValue() != enable) {
							String localMsg = parent.postCommand("Input.SetConfig", "{\"id\":" + nameId[1] + ",\"config\":{\"factory_reset\":" + enable + "}");
							if(localMsg != null) {
								msg = localMsg;
							}
						}
					}
				}
				return msg;
			} catch (IOException e) {
				return e.getMessage();
			}
		}
	}
}