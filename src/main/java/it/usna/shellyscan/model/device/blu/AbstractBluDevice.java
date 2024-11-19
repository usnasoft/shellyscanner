package it.usna.shellyscan.model.device.blu;

import java.io.IOException;

import org.eclipse.jetty.client.HttpClient;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.DynamicComponents;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.model.device.modules.InputResetManager;
import it.usna.shellyscan.model.device.modules.LoginManager;
import it.usna.shellyscan.model.device.modules.MQTTManager;
import it.usna.shellyscan.model.device.modules.TimeAndLocationManager;
import it.usna.shellyscan.model.device.modules.WIFIManager;
import it.usna.shellyscan.model.device.modules.WIFIManager.Network;

public abstract class AbstractBluDevice extends ShellyAbstractDevice {
	public final static String GENERATION = "blu";
//	private final static Logger LOG = LoggerFactory.getLogger(AbstractBluDevice.class);
	protected final AbstractG2Device parent;
//	protected WebSocketClient wsClient;
	protected final String componentIndex;
//	protected String localName;
//	protected SensorsCollection sensors;
//	private Meters[] meters;
	
	public final static String DEVICE_KEY_PREFIX = DynamicComponents.BTHOME_DEVICE + ":"; // "bthomedevice:";
	public final static String SENSOR_KEY_PREFIX = DynamicComponents.BTHOME_SENSOR + ":"; // "bthomesensor:";
	public final static String GROUP_KEY_PREFIX = DynamicComponents.GROUP_TYPE + ":"; // "group:";
	
	/**
	 * AbstractBluDevice constructor
	 * @param parent
	 * @param info
	 * @param index
	 */
	protected AbstractBluDevice(AbstractG2Device parent, JsonNode compInfo, String index) {
		super(new BluInetAddressAndPort(parent.getAddressAndPort(), Integer.parseInt(index)));
		this.parent = parent;
		this.componentIndex = index;
		this.mac = compInfo.path("config").path("addr").asText();
	}
	
	public void init(HttpClient httpClient/*, WebSocketClient wsClient*/) throws IOException {
		this.httpClient = httpClient;
//		this.wsClient = wsClient;
		refreshStatus();
		refreshSettings();
	}

//	@Override
//	public BluInetAddressAndPort getAddressAndPort() {
//		return (BluInetAddressAndPort)addressAndPort;
//	}
	
	public ShellyAbstractDevice getParent() {
		return parent;
	}
	
	public String getIndex() {
		return componentIndex;
	}
	
	@Override
	public Status getStatus() {
		if(rssi < 0) {
			return parent.getStatus();
		} else if(parent.getStatus() == Status.NOT_LOOGGED) {
			return Status.NOT_LOOGGED;
		} else {
			return Status.OFF_LINE;
		}
	}
	
	public String postCommand(final String method, String payload) {
		return parent.postCommand(method, payload);
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex};
	}

	@Override
	public void reboot() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String setCloudEnabled(boolean enable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setEcoMode(boolean eco) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FirmwareManager getFWManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WIFIManager getWIFIManager(Network net) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MQTTManager getMQTTManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public LoginManager getLoginManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public TimeAndLocationManager getTimeAndLocationManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public InputResetManager getInputResetManager() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return getTypeName() + "-" + name + ":" + mac;
	}
}