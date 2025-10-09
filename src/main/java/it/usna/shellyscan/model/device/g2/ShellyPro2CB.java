package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.g2.modules.CBreaker;
import it.usna.shellyscan.model.device.meters.Meters;
import it.usna.shellyscan.model.device.modules.DeviceModule;

public class ShellyPro2CB extends AbstractProDevice implements ModulesHolder, InternalTmpHolder {
	public static final String ID = "ProCB";
	public static final String MODEL = "SPCB-02VENEU";
	private static final Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.V};
	private CBreaker breaker = new CBreaker(this);
	private CBreaker[] breakers = new CBreaker[] {breaker};
	private float voltage;
	private String voltmeterName;
	private float internalTmp;
	private Meters[] meters;
	
	public ShellyPro2CB(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		
		meters = new Meters[] {
				new Meters() {
					@Override
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						return voltage;
					}
					
					@Override
					public String getName(Type t) {
						return voltmeterName;
					}
				}
		};
	}
	
	@Override
	public String getTypeName() {
		return "Shelly Pro 2CB";
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
	public DeviceModule[] getModules() {
		return breakers;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		breaker.fillSettings(configuration.get("cb:0"));
		voltmeterName = configuration.get("voltmeter:0").get("name").textValue();
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode cb0 = status.get("cb:0");
		breaker.fillStatus(cb0);

		internalTmp = cb0.get("temperature").get("tC").floatValue();
		voltage = status.get("voltmeter:0").get("voltage").floatValue();
	}
	
	@Override
	public String[] getInfoRequests() {
		return CBreaker.getInfoRequests(super.getInfoRequests());
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(breaker.restore(config));
		
		errors.add(postCommand("CB.SetConfig", AbstractG2Device.createIndexedRestoreNode(config, "voltmeter", 0)));
	}
}