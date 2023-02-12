package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyPro1PM extends AbstractProDevice implements RelayCommander, InternalTmpHolder {
	public final static String ID = "Pro1PM";
	private final static Meters.Type[] SUPPORTED_MEASURES = new Meters.Type[] {Meters.Type.W, Meters.Type.PF, Meters.Type.V, Meters.Type.I};
	private Relay relay0= new Relay(this, 0);
	private float internalTmp;
	private float power0;
	private float voltage0;
	private float current0;
	private float pf0;
	private Meters[] meters;
	private RelayInterface[] relays = new RelayInterface[] {relay0};

	public ShellyPro1PM(InetAddress address, String hostname) {
		super(address, hostname);

		meters = new Meters[] {
				new Meters() {
					public Type[] getTypes() {
						return SUPPORTED_MEASURES;
					}

					@Override
					public float getValue(Type t) {
						if(t == Meters.Type.W) {
							return power0;
						} else if(t == Meters.Type.I) {
							return current0;
						} else if(t == Meters.Type.PF) {
							return pf0;
						} else {
							return voltage0;
						}
					}
				}
		};
	}

	@Override
	public String getTypeName() {
		return "Shelly Pro 4PM";
	}

	@Override
	public String getTypeID() {
		return ID;
	}

	@Override
	public int getRelayCount() {
		return 2;
	}

	@Override
	public Relay getRelay(int index) {
		return relay0;
	}

	@Override
	public RelayInterface[] getRelays() {
		return relays;
	}

	@Override
	public float getInternalTmp() {
		return internalTmp;
	}

	//	public float getPower() {
	//		return power0;
	//	}
	//
	//	public float getVoltage() {
	//		return voltage0;
	//	}
	//
	//	public float getCurrente() {
	//		return current0;
	//	}

	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay0.fillSettings(configuration.get("switch:0"));
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus0 = status.get("switch:0");
		relay0.fillStatus(switchStatus0, status.get("input:0"));
		power0 = (float)switchStatus0.get("apower").asDouble();
		voltage0 = (float)switchStatus0.get("voltage").asDouble();
		current0 = (float)switchStatus0.get("current").asDouble();
		pf0 = (float)switchStatus0.get("pf").asDouble();

		internalTmp = (float)switchStatus0.path("temperature").path("tC").asDouble();
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException, InterruptedException {
		errors.add(Input.restore(this,configuration, "0"));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(relay0.restore(configuration));
	}

	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay0;
	}
}