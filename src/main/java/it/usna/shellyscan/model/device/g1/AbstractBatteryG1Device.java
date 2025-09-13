package it.usna.shellyscan.model.device.g1;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.BatteryDeviceInterface;

public abstract class AbstractBatteryG1Device extends AbstractG1Device implements BatteryDeviceInterface {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractBatteryG1Device.class);
	protected JsonNode stShelly;
	protected JsonNode stSettings;
	protected JsonNode stStatus;
	protected JsonNode stSettingsActions;
	protected int bat;

	protected AbstractBatteryG1Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public final void init(HttpClient httpClient, JsonNode shelly) throws IOException {
		this.httpClient = httpClient;
		this.mac = shelly.get("mac").asText();
		this.stShelly = shelly;
		init();
	}
	
	@Override
	public int getBattery() {
		return bat;
	}
	
	@Override
	public JsonNode getStoredJSON(final String command) {
		if(command.equals("/shelly")) {
			return stShelly;
		} else if(command.equals("/settings")) {
			return stSettings;
		} else if(command.equals("/status")) {
			return stStatus;
		} else if(command.equals("/settings/actions")) {
			return stSettingsActions;
		}
		return null;
	}
	
	@Override
	public void setStoredJSON(final String command, JsonNode val) {
		if(command.equals("/shelly")) {
			this.stShelly = val;
		} else if(command.equals("/settings")) {
			this.stSettings = val;
		} else if(command.equals("/status")) {
			this.stStatus = val;
		} else if(command.equals("/settings/actions")) {
			this.stSettingsActions = val;
		}
		lastConnection = System.currentTimeMillis();
	}
	
	@Override
	public boolean backup(final Path file) throws IOException {
		try {
			return super.backup(file);
		} catch (/*java.net.SocketTimeout*/Exception e) {
			if(getStatus() != Status.ON_LINE && stSettings != null && stSettingsActions != null) {
				try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8)) {
					out.putNextEntry(new ZipEntry("settings.json"));
					jsonMapper.writer().writeValue(out, stSettings);
					out.closeEntry();
					out.putNextEntry(new ZipEntry("actions.json"));
					jsonMapper.writer().writeValue(out, stSettingsActions);
					out.closeEntry();
				} catch(IOException ex) {
					LOG.error("backup", e);
				}
				return false;
			} else {
				throw e;
			}
		}
	}

//	public void copyFrom(BatteryDeviceInterface dev) {
//		AbstractBatteryG1Device devG1 = (AbstractBatteryG1Device)dev;
//		if(shelly == null) {
//			shelly = devG1.shelly;
//		}
//		if(settings == null) {
//			settings = devG1.settings;
//		}
//		if(status == null) {
//			status = devG1.status;
//		}
//		if(settingsActions == null) {
//			settingsActions = devG1.settingsActions;
//		}
//	}

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