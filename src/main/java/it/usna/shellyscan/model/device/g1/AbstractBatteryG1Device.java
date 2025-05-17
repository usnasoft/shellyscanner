package it.usna.shellyscan.model.device.g1;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.BatteryDeviceInterface;

public abstract class AbstractBatteryG1Device extends AbstractG1Device implements BatteryDeviceInterface {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractBatteryG1Device.class);
	protected JsonNode shelly;
	protected JsonNode settings;
	protected JsonNode status;
	protected JsonNode settingsActions;
	protected int bat;

	protected AbstractBatteryG1Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public final void init(HttpClient httpClient, JsonNode shelly) throws IOException {
		this.httpClient = httpClient;
		this.mac = shelly.get("mac").asText();
		this.shelly = shelly;
		init();
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
	public boolean backup(final Path file) throws IOException {
		try {
			return super.backup(file);
		} catch (/*java.net.SocketTimeout*/Exception e) {
			if(getStatus() != Status.ON_LINE && settings != null && settingsActions != null) {
				Map<String, String> providerProps = new HashMap<>();
		        providerProps.put("create", "true");
				try(FileSystem fs = FileSystems.newFileSystem(file, providerProps)) {
					try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath("settings.json"))) {
						jsonMapper.writer().writeValue(writer, settings);
					}
					try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath("Shelly.GetDeviceInfo.json"))) {
						jsonMapper.writer().writeValue(writer, settingsActions);
					}
				} catch(IOException ex) {
					LOG.error("backup script {script.getName()}", e);
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