package it.usna.shellyscan.model.device.g2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyPlusHT extends AbstractBatteryG2Device {
	public final static String ID = "PlusHT";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.BAT};
	private float temp;
	private float humidity;
	private Meters[] meters;
	
	public ShellyPlusHT(InetAddress address, JsonNode shelly, String hostname) {
		this(address, hostname);
		this.shelly = shelly;
	}
	
	public ShellyPlusHT(InetAddress address, String hostname) {
		super(address, hostname);

		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.BAT) {
							return bat;
						} else if(t == Meters.Type.H) {
							return humidity;
						} else {
							return temp;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly +H&T";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	/**
	 * No scripts, No Schedule
	 */
	public String[] getInfoRequests() {
		return new String[] {"/rpc/Shelly.GetDeviceInfo", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Webhook.List"};
	}
	
	@Override
	protected void fillSettings(JsonNode settings) throws IOException {
		super.fillSettings(settings);
		this.settings = settings;
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.status = status;
		temp = (float)status.path("temperature:0").path("tC").asDouble();
		humidity = (float)status.path("humidity:0").path("rh").asDouble();
		bat = status.path("devicepower:0").path("battery").path("percent").asInt();
	}

	public float getTemp() {
		return temp;
	}
	
	public float getHumidity() {
		return humidity;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
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
	
	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException {
		errors.add(postCommand("HT_UI.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("ht_ui")) + "}"));
		errors.add(postCommand("Temperature.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("temperature:0")) + "}"));
		errors.add(postCommand("Humidity.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("humidity:0")) + "}"));
	}
}