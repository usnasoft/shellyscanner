package it.usna.shellyscan.model.device.g2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.BatteryDeviceInterface;

public abstract class AbstractBatteryG2Device extends AbstractG2Device implements BatteryDeviceInterface {
	protected JsonNode shelly;
	protected JsonNode settings;
	protected JsonNode status;
	protected Map<String, JsonNode> others = new HashMap<>();
	protected int bat;

	protected AbstractBatteryG2Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
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
	
	@Override
	/**
	 * No scripts, No Schedule
	 */
	public String[] getInfoRequests() {
		return new String[] {"/rpc/Shelly.GetDeviceInfo", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Webhook.List"};
	}
	
	@Override
	/**
	 * No scripts, No Schedule
	 */
	public boolean backup(final File file) throws IOException {
		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", out);
			sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", out);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
		} catch(Exception e) {
			if(getStatus() != Status.ON_LINE && getStoredJSON("/rpc/Shelly.GetDeviceInfo") != null && getStoredJSON("/rpc/Shelly.GetConfig") != null && getStoredJSON("/rpc/Webhook.List") != null) {
				try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
					out.putNextEntry(new ZipEntry("Shelly.GetDeviceInfo.json"));
					out.write(getStoredJSON("/rpc/Shelly.GetDeviceInfo").toString().getBytes());
					out.closeEntry();
					out.putNextEntry(new ZipEntry("Shelly.GetConfig.json"));
					out.write(getStoredJSON("/rpc/Shelly.GetConfig").toString().getBytes());
					out.closeEntry();
					out.putNextEntry(new ZipEntry("Webhook.List.json"));
					out.write(getStoredJSON("/rpc/Webhook.List").toString().getBytes());
					out.closeEntry();
				}
				return false;
			} else {
				throw e;
			}
		}
		return true;
	}
}
