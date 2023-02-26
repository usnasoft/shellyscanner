package it.usna.shellyscan.model.device.g2;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.BatteryDeviceInterface;

public abstract class AbstractBatteryG2Device extends AbstractG2Device implements BatteryDeviceInterface {
	protected JsonNode shelly;
	protected JsonNode settings;
	protected JsonNode status;
	protected Map<String, JsonNode> others = new HashMap<>();
	protected int bat;

	protected AbstractBatteryG2Device(InetAddress address, String hostname) {
		super(address, hostname);
	}
	
	@Override
	public int getBattery() {
		return bat;
	}
	
	@Override
	public JsonNode getStoredJSON(final String command) {
		if(command.equals("/shelly") || command.equals("/rpc/Shelly.GetDeviceInfo")) {
			return shelly;
		} else if(command.equals("/rpc/Shelly.GetConfig")) {
			return settings;
		} else if(command.equals("/rpc/Shelly.GetStatus")) {
			return status;
		} else {
			return others.get(command);
		}
	}
	
	@Override
	public void setStoredJSON(final String command, JsonNode val) {
		if(command.equals("/shelly") || command.equals("/rpc/Shelly.GetDeviceInfo")) {
			this.shelly = val;
		} else if(command.equals("/rpc/Shelly.GetConfig")) {
			this.settings = val;
		} else if(command.equals("/rpc/Shelly.GetStatus")) {
			this.status = val;
		} else {
			others.put(command, val);
		}
		lastConnection = System.currentTimeMillis();
	}
	
//	@Override
//	public boolean backup(final File file) throws IOException {
//		try {
//			return super.backup(file);
//		} catch (java.net.ConnectException e) {
//			if(settings != null && settingsActions != null) {
//				try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
//					out.putNextEntry(new ZipEntry("settings.json"));
//					out.write(settings.toString().getBytes());
//					out.closeEntry();
//					out.putNextEntry(new ZipEntry("actions.json"));
//					out.write(settingsActions.toString().getBytes());
//					out.closeEntry();
//				}
//				return false;
//			} else {
//				throw e;
//			}
//		}
//	}
}
