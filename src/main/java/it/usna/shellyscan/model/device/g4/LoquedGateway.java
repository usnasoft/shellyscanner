package it.usna.shellyscan.model.device.g4;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;

/**
 * Loqued Gateway (only) model
 */
public class LoquedGateway extends AbstractG4Device {
	public static final String ID = "loqed-aca70440d160";
	public static final String MODEL = "S0GW-0ABT001";
	
	public LoquedGateway(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}

	@Override
	public String getTypeName() {
		return "Loqued Gateway";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getModelID() {
		return MODEL;
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
	}
}