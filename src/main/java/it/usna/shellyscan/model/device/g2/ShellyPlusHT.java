package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyPlusHT extends AbstractBatteryG2Device {
	public final static String ID = "PlusHT";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.BAT};
	private float temp;
	private float humidity;
	private Meters[] meters;
	
	public ShellyPlusHT(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

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
	protected void restore(Map<String, JsonNode> backupJsons, ArrayList<String> errors) throws IOException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(postCommand("HT_UI.SetConfig", "{\"config\":" + jsonMapper.writeValueAsString(configuration.get("ht_ui")) + "}"));
		
		ObjectNode temperatureNode = (ObjectNode)configuration.get("temperature:0");
		temperatureNode.remove("id");
		errors.add(postCommand("Temperature.SetConfig", "{\"id\":0,\"config\":" + jsonMapper.writeValueAsString(temperatureNode) + "}"));

		ObjectNode humidityNode = (ObjectNode)configuration.get("humidity:0");
		humidityNode.remove("id");
		errors.add(postCommand("Humidity.SetConfig", "{\"id\":0,\"config\":" + jsonMapper.writeValueAsString(humidityNode) + "}"));
	}
}