package it.usna.shellyscan.model.device.g1.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g1.AbstractG1Device;
import it.usna.shellyscan.model.device.modules.InputResetManager;

public class InputResetManagerG1 implements InputResetManager {
	private final AbstractG1Device parent;
	private Status mode;
	
	public InputResetManagerG1(AbstractG1Device d) throws IOException {
		this(d, d.getJSON("/settings"));
	}
	
	public InputResetManagerG1(AbstractG1Device d, JsonNode config) {
		this.parent = d;
		JsonNode reset = config.path("factory_reset_from_switch");
		if(reset.isMissingNode()) {
			mode = Status.NOT_APPLICABLE;
		} else if(reset.booleanValue()) {
			mode = Status.TRUE;
		} else {
			mode = Status.FALSE;
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
			return parent.sendCommand("/settings?factory_reset_from_switch=" + enable);
		}
	}
}
