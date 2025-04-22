package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ModulesHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.g2.modules.Input;
import it.usna.shellyscan.model.device.g2.modules.SensorAddOn;
import it.usna.shellyscan.model.device.g2.modules.Webhooks;
import it.usna.shellyscan.model.device.modules.DeviceModule;

public class ShellyPlusi4 extends AbstractG2Device implements ModulesHolder {
	private final static Logger LOG = LoggerFactory.getLogger(ShellyPlusi4.class);
	public final static String ID = "PlusI4";
	private Input[] inputs;
	private Webhooks webhooks;
	private Meters[] meters;
	private SensorAddOn addOn;

	public ShellyPlusi4(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
		inputs = new Input[] {new Input(), new Input(), new Input(), new Input()};
		webhooks = new Webhooks(this);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		this.hostname = devInfo.get("id").asText("");
		this.mac = devInfo.get("mac").asText();
		
		final JsonNode config = configure();
		
		fillSettings(config);
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	private JsonNode configure() throws IOException {
		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
		if(SensorAddOn.ADDON_TYPE.equals(config.get("sys").get("device").path("addon_type").asText())) {
			addOn = new SensorAddOn(this);
			meters = (addOn.getTypes().length > 0) ? new Meters[] {addOn} : null;
		} else {
			addOn = null;
			meters = null;
		}
		return config;
	}
	
	@Override
	public String getTypeName() {
		return "Shelly +i4";
	}
	
	@Override
	public String getTypeID() {
		return ID;
	}
	
	@Override
	public Meters[] getMeters() {
		return meters;
	}

	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		inputs[0].fillSettings(configuration.get("input:0"));
		inputs[1].fillSettings(configuration.get("input:1"));
		inputs[2].fillSettings(configuration.get("input:2"));
		inputs[3].fillSettings(configuration.get("input:3"));
		try { TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY); } catch (InterruptedException e) {}
		webhooks.fillSettings();
		inputs[0].associateWH(webhooks.getHooks(0));
		inputs[1].associateWH(webhooks.getHooks(1));
		inputs[2].associateWH(webhooks.getHooks(2));
		inputs[3].associateWH(webhooks.getHooks(3));
		if(addOn != null) {
			addOn.fillSettings(configuration);
		}
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		inputs[0].fillStatus(status.get("input:0"));
		inputs[1].fillStatus(status.get("input:1"));
		inputs[2].fillStatus(status.get("input:2"));
		inputs[3].fillStatus(status.get("input:3"));
		if(addOn != null) {
			addOn.fillStatus(status);
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		final String[] cmd = super.getInfoRequests();
		return (addOn != null) ? SensorAddOn.getInfoRequests(cmd) : cmd;
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		try {
			configure(); // maybe useless in case of mDNS use since you must reboot before -> on reboot the device registers again on mDNS ad execute a reload
		} catch (IOException e) {
			LOG.error("restoreCheck", e);
		}
		SensorAddOn.restoreCheck(this, addOn, backupJsons, res);
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode configuration = backupJsons.get("Shelly.GetConfig.json");
		errors.add(Input.restore(this, configuration, 0));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 1));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 2));
		TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
		errors.add(Input.restore(this, configuration, 3));
		
		SensorAddOn.restore(this, addOn, backupJsons, errors);
	}
	
	@Override
	public int getModulesCount() {
		return 4;
	}

	@Override
	public DeviceModule[] getModules() {
		return inputs;
	}
}