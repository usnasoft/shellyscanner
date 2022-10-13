package it.usna.shellyscan.model.device.g1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.hc.client5.http.auth.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class AbstractBatteryDevice extends AbstractG1Device {
	protected JsonNode shelly;
	protected JsonNode settings;
	protected JsonNode status;
	protected JsonNode settingsActions;
	protected int bat;

	protected AbstractBatteryDevice(InetAddress address, CredentialsProvider credentialsProv) {
		super(address, credentialsProv);
	}
	
	public int getBattery() {
		return bat;
	}
	
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
	}
	
	@Override
	public boolean backup(final File file) throws IOException {
		try {
			return super.backup(file);
		} catch (java.net.ConnectException e) {
			if(settings != null && settingsActions != null) {
				try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
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
