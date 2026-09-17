package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g3.modules.LightRGBCCT;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.RGBCCTInterface;
import tools.jackson.databind.JsonNode;

/**
 * Shelly dimmer G3 model
 * @author usna
 */
public class ShellyBulbRGBG3 extends AbstractG3Device implements ModulesHolder {
	public static final String ID = "RGBCCTBulbG3";
	public static final String MODEL = "S3BL-C010007AEU";
	private Meters[] meters;
	private LightRGBCCT rgbcct = new LightRGBCCT(this);
	private RGBCCTInterface[] lightArray = new RGBCCTInterface[] {rgbcct};
	private float power;

	public ShellyBulbRGBG3(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W};
					
					@Override
					public float getValue(Type t) {
						return power;
					}

					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly RGB bulb G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public String getModelID() {
		return MODEL;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	public DeviceModule[] getModules() {
		return lightArray;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		rgbcct.fillSettings(configuration.get("rgbcct:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode lightStatus = status.get("rgbcct:0");
		rgbcct.fillStatus(lightStatus);
		power = lightStatus.get("apower").floatValue();
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(rgbcct.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " RGBCCT: " + rgbcct;
	}
}