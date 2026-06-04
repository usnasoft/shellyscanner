package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.RestoreUtil;
import it.usna.shellyscan.model.device.g2.meters.EM1Meters;
import it.usna.shellyscan.model.device.g2.meters.EMPhaseMeters;
import it.usna.shellyscan.model.device.g2.meters.EMTotalMeters;
import it.usna.shellyscan.model.device.g2.modules.EM1Manager;
import it.usna.shellyscan.model.device.g2.modules.EMManager;
import it.usna.shellyscan.model.device.meters.Meters;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

public class ShellyPro3EM extends AbstractProDevice implements InternalTmpHolder {
	public static final String ID = "Pro3EM";
	public static final String ID_ADDON = "Pro3EMProAddon";
	public static final String MODEL = "SPEM-003CEBEU";
	private float internalTmp;
	private EM1Meters em1meters0, em1meters1, em1meters2; // em1
	private EMPhaseMeters emMeters0, emMeters1, emMeters2; //em (triphase)
	private EMTotalMeters emTotal; // em
	private Meters meters[];
	private boolean triphase;
	
	private static final String MODE_TRIPHASE = "triphase";

	public ShellyPro3EM(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
	@Override
	protected void init(JsonNode devInfo) throws IOException {
		configurePhases(devInfo.get("profile").asString("").equals(MODE_TRIPHASE));
		super.init(devInfo);
	}
	
	private void configurePhases(boolean triphase) {
		this.triphase = triphase;
		if(triphase) {
			emMeters0 = new EMPhaseMeters("a");
			emMeters1 = new EMPhaseMeters("b");
			emMeters2 = new EMPhaseMeters("c");
			emTotal = new EMTotalMeters(new EMManager(this));
			meters = new Meters[] {emMeters0, emMeters1, emMeters2, emTotal};
			em1meters0 = em1meters1 = em1meters2 = null;
		} else {
			em1meters0 = new EM1Meters(new EM1Manager(this, 0));
			em1meters1 = new EM1Meters(new EM1Manager(this, 1));
			em1meters2 = new EM1Meters(new EM1Manager(this, 2));
			meters = new Meters[] {em1meters0, em1meters1, em1meters2};
			emMeters0 = emMeters1 = emMeters2 = null;
			emTotal = null;
		}
	}
	
//	@Override
//	protected void init(JsonNode devInfo) throws IOException {
//		configure(devInfo.get("profile").asString("").equals(MODE_TRIPHASE));
//		this.hostname = devInfo.get("id").asString("");
//		this.mac = devInfo.get("mac").asString("").toUpperCase();
//
//		final JsonNode config = configure(devInfo.get("profile").asString("").equals(MODE_TRIPHASE));
//		
//		fillSettings(config);
//		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
//	}
//	
//	private JsonNode configure(boolean triphase) throws IOException {
//		final JsonNode config = getJSON("/rpc/Shelly.GetConfig");
//		this.triphase = triphase;
//		if(triphase) {
//			emMeters0 = new EMPhaseMeters("a");
//			emMeters1 = new EMPhaseMeters("b");
//			emMeters2 = new EMPhaseMeters("c");
//			emTotal = new EMTotalMeters(new EMManager(this));
//			meters = new Meters[] {emMeters0, emMeters1, emMeters2, emTotal};
//			em1meters0 = em1meters1 = em1meters2 = null;
//		} else {
//			em1meters0 = new EM1Meters(new EM1Manager(this, 0));
//			em1meters1 = new EM1Meters(new EM1Manager(this, 1));
//			em1meters2 = new EM1Meters(new EM1Manager(this, 2));
//			meters = new Meters[] {em1meters0, em1meters1, em1meters2};
//			emMeters0 = emMeters1 = emMeters2 = null;
//			emTotal = null;
//		}
//		return config;
//	}

	@Override
	public String getTypeName() {
		return "Shelly Pro 3EM";
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
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		boolean config3phase = configuration.get("sys").get("device").get("profile").asString("").equals(MODE_TRIPHASE);
		if(config3phase != triphase) {
			configurePhases(config3phase);
		}
		if(config3phase) {
			emMeters0.fillSettings(configuration.get("em:0"));
		} else {
			em1meters0.fillSettings(configuration.get("em1:0"));
			em1meters1.fillSettings(configuration.get("em1:1"));
			em1meters2.fillSettings(configuration.get("em1:2"));
		}
	}

	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		if(triphase) {
			JsonNode em0 = status.get("em:0");
			emMeters0.fillStatus(em0);
			emMeters1.fillStatus(em0);
			emMeters2.fillStatus(em0);
			emTotal.fillStatus(em0);
		} else {
			em1meters0.fillStatus(status.get("em1:0"));
			em1meters1.fillStatus(status.get("em1:1"));
			em1meters2.fillStatus(status.get("em1:2"));
		}

		internalTmp = status.path("temperature:0").path("tC").floatValue();
	}
	
	@Override
	public String[] getInfoRequests() {
		if(triphase) {
			return EMManager.getInfoRequests(super.getInfoRequests());
		} else {
			return EM1Manager.getInfoRequests(super.getInfoRequests(), 0, 1, 2);
		}
	}
	
	@Override
	public void restoreCheck(Map<String, JsonNode> backupJsons, Map<RestoreMsg, Object> res) throws IOException {
		JsonNode devInfo = backupJsons.get("Shelly.GetDeviceInfo.json");
		boolean backModeTriphase = MODE_TRIPHASE.equals(devInfo.get("profile").asString(""));
		if(backModeTriphase != triphase) {
			res.put(RestoreMsg.ERR_RESTORE_MODE_TRIPHASE, null);
		}
	}

	@Override
	protected void restore(Map<String, JsonNode> backupJsons, List<String> errors) throws InterruptedException {
		JsonNode config = backupJsons.get("Shelly.GetConfig.json");
		if(triphase) {
			ObjectNode conf = RestoreUtil.createIndexedRestoreNode(config, "em", 0);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM.SetConfig", conf));
		} else {
			ObjectNode conf = RestoreUtil.createIndexedRestoreNode(config, "em1", 0);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM1.SetConfig", conf));
			
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			conf = RestoreUtil.createIndexedRestoreNode(config, "em1", 1);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM1.SetConfig", conf));
			
			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
			conf = RestoreUtil.createIndexedRestoreNode(config, "em1", 2);
			((ObjectNode)conf.get("config")).remove("ct_type");
			errors.add(postCommand("EM1.SetConfig", conf));
		}
	}
}