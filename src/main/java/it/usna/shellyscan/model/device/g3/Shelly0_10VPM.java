package it.usna.shellyscan.model.device.g3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.LightWhite;
import it.usna.shellyscan.model.device.meters.MetersWVI;
import it.usna.shellyscan.model.device.modules.WhiteCommander;

/**
 * Shelly dimmer 0/1-10 G3 model
 * @author usna
 */
public class Shelly0_10VPM extends AbstractG3Device implements InternalTmpHolder, WhiteCommander {
	public final static String ID = "Dimmer0110VPMG3";
	private float internalTmp;
	private float power;
	private float voltage;
	private float current;
	private Meters[] meters;
	private LightWhite light = new LightWhite(this, 0);
	private LightWhite[] lightArray = new LightWhite[] {light};

	public Shelly0_10VPM(InetAddress address, int port, String hostname) {
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
		return "Shelly Dimmer 0/1-10 G3";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	public LightWhite getWhite(int index) {
		return light;
	}

	@Override
	public LightWhite[] getWhites() {
		return lightArray;
	}

	@Override
	public int getWhitesCount() {
		return 1;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		light.fillSettings(configuration.get("light:0"));
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode lightStatus = status.get("light:0");
		internalTmp = lightStatus.get("temperature").get("tC").floatValue();
		power = lightStatus.get("apower").floatValue();
		voltage = lightStatus.get("voltage").floatValue();
		current = lightStatus.get("current").floatValue();
		light.fillStatus(lightStatus, status.get("input:0"));
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(light.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Light: " + light;
	}
}