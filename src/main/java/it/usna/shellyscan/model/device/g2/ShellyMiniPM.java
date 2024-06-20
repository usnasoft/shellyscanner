package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.meters.MetersWVI;

/**
 * Shelly Shelly Plus mini PM model
 * @author usna
 */
public class ShellyMiniPM extends AbstractG2Device {
	public final static String ID = "PlusPMMini";
	private float power;
	private float voltage;
	private float current;
	private Meters[] meters;

	public ShellyMiniPM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters = new MetersWVI[] {
				new MetersWVI() {
					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power;
						} else if(t == Meters.Type.I) {
							return current;
						} else {
							return voltage;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Mini PM";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

//	@Override
//	protected void fillSettings(JsonNode configuration) throws IOException {
//		super.fillSettings(configuration);
//	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode pm1 = status.get("pm1:0");
		power = pm1.get("apower").floatValue();
		voltage = pm1.get("voltage").floatValue();
		current = pm1.get("current").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
//		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
//		ObjectNode pm1 = (ObjectNode)config.path("pm1:0").deepCopy();
//		pm1.remove("id");
//		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//		errors.add(postCommand("PM1.SetConfig", pm1));
	}
}