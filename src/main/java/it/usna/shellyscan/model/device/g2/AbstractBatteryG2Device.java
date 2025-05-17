package it.usna.shellyscan.model.device.g2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.BatteryDeviceInterface;

public abstract class AbstractBatteryG2Device extends AbstractG2Device implements BatteryDeviceInterface {
	private final static Logger LOG = LoggerFactory.getLogger(AbstractBatteryG2Device.class);
	protected JsonNode shelly;
	protected JsonNode settings;
	protected JsonNode status;
	protected Map<String, JsonNode> others = new HashMap<>();
	protected int bat;

	protected AbstractBatteryG2Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	public void init(HttpClient httpClient, WebSocketClient wsClient, JsonNode devInfo) throws IOException {
		this.shelly = devInfo;
		this.httpClient = httpClient;
		this.wsClient = wsClient;
		init(devInfo);
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
		return new String[] {"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Webhook.List", "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents"};
	}
	
	@Override
	/**
	 * No scripts, No Schedule
	 */
	public boolean backup(final Path file) throws IOException {
		Map<String, String> providerProps = new HashMap<>();
        providerProps.put("create", "true");
		try(FileSystem fs = FileSystems.newFileSystem(file, providerProps)) {
			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", fs);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", fs);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", fs);
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			try {
				sectionToStream("/rpc/KVS.GetMany", "KVS.GetMany.json", fs);
			} catch(Exception e) {}
		} catch(InterruptedException e) {
			LOG.error("backup", e);
		} catch(Exception e) {
			if(getStatus() != Status.ON_LINE && getStoredJSON("/rpc/Shelly.GetDeviceInfo") != null && getStoredJSON("/rpc/Shelly.GetConfig") != null && getStoredJSON("/rpc/Webhook.List") != null && getStoredJSON("/rpc/KVS.GetMany") != null) {
				try(FileSystem fs = FileSystems.newFileSystem(file)) {
					try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath("Shelly.GetDeviceInfo.json"))) {
						jsonMapper.writer().writeValue(writer, getStoredJSON("/rpc/Shelly.GetDeviceInfo"));
					}
					try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath("Shelly.GetConfig.json"))) {
						jsonMapper.writer().writeValue(writer, getStoredJSON("/rpc/Shelly.GetConfig"));
					}
					try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath("Webhook.List.json"))) {
						jsonMapper.writer().writeValue(writer, getStoredJSON("/rpc/Webhook.List"));
					}
					try(BufferedWriter writer = Files.newBufferedWriter(fs.getPath("KVS.GetMany.json"))) {
						jsonMapper.writer().writeValue(writer, getStoredJSON("/rpc/KVS.GetMany"));
					}
				} catch(IOException ex) {
					LOG.error("backup script {script.getName()}", e);
				}
				return false;
			} else {
				throw e;
			}
		}
		return true;
	}

//	public void copyFrom(BatteryDeviceInterface dev) {
//		AbstractBatteryG2Device devG2 = (AbstractBatteryG2Device)dev;
//		if(shelly == null) {
//			shelly = devG2.shelly;
//		}
//		if(settings == null) {
//			settings = devG2.settings;
//		}
//		if(status == null) {
//			status = devG2.status;
//		}
//		devG2.others.keySet().forEach(k -> {
//			if(others.containsKey(k) == false) {
//				others.put(k, devG2.others.get(k));
//			}
//		});
//	}
}