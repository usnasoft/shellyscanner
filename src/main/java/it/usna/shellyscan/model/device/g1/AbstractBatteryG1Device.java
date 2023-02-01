package it.usna.shellyscan.model.device.g1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.BatteryDeviceInterface;

public abstract class AbstractBatteryG1Device extends AbstractG1Device implements BatteryDeviceInterface {
	protected JsonNode shelly;
	protected JsonNode settings;
	protected JsonNode status;
	protected JsonNode settingsActions;
	protected int bat;

	protected AbstractBatteryG1Device(InetAddress address, CredentialsProvider credentialsProv) {
		super(address, credentialsProv);
	}
	
	@Override
	public int getBattery() {
		return bat;
	}
	
	@Override
	public JsonNode getStoredJSON(final String command) {
		if(command.equals("/shelly")) {
			return shelly;
		} else if(command.equals("/settings")) {
			return settings;
		} else if(command.equals("/status")) {
			return status;
		} else if(command.equals("/settings/actions")) {
			return settingsActions;
		}
		return null;
	}
	
	@Override
	public void setStoredJSON(final String command, JsonNode val) {
		if(command.equals("/shelly")) {
			this.shelly = val;
		} else if(command.equals("/settings")) {
			this.settings = val;
		} else if(command.equals("/status")) {
			this.status = val;
		} else if(command.equals("/settings/actions")) {
			this.settingsActions = val;
		}
		lastConnection = System.currentTimeMillis();
	}
	
	@Override
	public boolean backup(final File file) throws IOException {
		try {
			return super.backup(file);
		} catch (java.net.ConnectException e) {
			if(settings != null && settingsActions != null) {
				try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
					out.putNextEntry(new ZipEntry("settings.json"));
					out.write(settings.toString().getBytes());
					out.closeEntry();
					out.putNextEntry(new ZipEntry("actions.json"));
					out.write(settingsActions.toString().getBytes());
					out.closeEntry();
				}
				return false;
			} else {
				throw e;
			}
		}
	}

//	protected class BatteryMeter extends Meters {
//		private final  Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.BAT};
//
//		@Override
//		public Type[] getTypes() {
//			return SUPPORTED_MEASURES;
//		}
//
//		@Override
//		public float getValue(Type t) {
//			return bat;
//		}
//	}
}
