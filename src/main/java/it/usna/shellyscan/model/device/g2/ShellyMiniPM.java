package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;

/**
 * Shelly Shelly Plus mini PM model
 * @author usna
 */
public class ShellyMiniPM extends AbstractG2Device {
	public static final String ID = "PlusPMMini";
	public static final String MODEL = "SNPM-001PCEU16";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.V, Meters.Type.I, Meters.Type.FREQ};
	private float power;
	private float voltage;
	private float current;
	private float freq;
	private Meters[] meters;

	public ShellyMiniPM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);

		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power;
						} else if(t == Meters.Type.I) {
							return current;
						} else if(t == Meters.Type.V)  {
							return voltage;
						} else { // Meters.Type.FREQ
							return freq;
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
		freq = pm1.get("freq").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(postCommand("PM1.SetConfig", RestoreUtil.createIndexedRestoreNode(config, "pm1", 0)));
	}
}