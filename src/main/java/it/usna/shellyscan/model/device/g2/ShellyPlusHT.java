package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.Meters;

public class ShellyPlusHT extends AbstractBatteryG2Device {
	public static final String ID = "PlusHT";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.T, Meters.Type.H, Meters.Type.BAT};
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
						if(t == Type.BAT) {
							return bat;
						} else if(t == Type.H) {
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
		this.settingsJ = settings;
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		this.statusJ = status;
		temp = status.path("temperature:0").path("tC").floatValue();
		humidity = status.path("humidity:0").path("rh").floatValue();
		bat = status.path("devicepower:0").path("battery").path("percent").asInt();

//		System.out.println(getJSON("/rpc/Shelly.CheckForUpdate")); TEST - no way to obtain data for this device
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
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws JsonProcessingException {
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