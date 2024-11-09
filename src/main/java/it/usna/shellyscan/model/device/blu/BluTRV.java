package it.usna.shellyscan.model.device.blu;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class BluTRV extends AbstractBluDevice {
	public final static String DEVICE_KEY_PREFIX = "blutrv:"; // "bthomedevice:";
	
	public BluTRV(AbstractG2Device parent, JsonNode compInfo, String index) {
		super(parent, compInfo, index);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getTypeName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypeID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refreshSettings() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshStatus() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean backup(File file) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
