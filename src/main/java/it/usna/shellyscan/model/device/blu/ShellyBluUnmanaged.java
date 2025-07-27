package it.usna.shellyscan.model.device.blu;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.RestoreMsg;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyUnmanagedDeviceInterface;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.FirmwareManager;

public class ShellyBluUnmanaged extends AbstractBluDevice implements ShellyUnmanagedDeviceInterface {
	private String type;
	private Throwable ex;

	private ShellyBluUnmanaged(ShellyAbstractDevice parent, JsonNode info, String localName, String componentIndex) {
		super((AbstractG2Device)parent, info, componentIndex);
		this.type = localName;
		this.hostname = localName + "-" + mac;
	}
	
	public ShellyBluUnmanaged(ShellyAbstractDevice parent, JsonNode info, String index, Throwable ex) {
		this(parent, info, info.path("config").path("meta").path("ui").path("local_name").asText(""), index);
		this.ex = ex;
		status = Status.ERROR;
	}

	@Override
	public String getTypeID() {
		return type;
	}
	
	public String getTypeName() {
		return "Unmanaged BTHome";
	}
	
	@Override
	public Status getStatus() {
		if(status == Status.ON_LINE && ex != null) {
			return Status.ERROR;
		} else {
			return super.getStatus();
		}
	}
	
	@Override
	public String[] getInfoRequests() {
		return new String[] {"/rpc/BTHomeDevice.GetConfig?id=" + componentIndex, "/rpc/BTHomeDevice.GetStatus?id=" + componentIndex, "/rpc/BTHomeDevice.GetKnownObjects?id=" + componentIndex};
	}
	
	@Override
	public Throwable getException() {
		return ex;
	}

	@Override
	public void refreshSettings() throws IOException {
		// no universal data
	}

	@Override
	public void refreshStatus() throws IOException {
		// no universal data
	}
	
	@Override
	public FirmwareManager getFWManager() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean backup(Path file) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<RestoreMsg, Object> restoreCheck(Map<String, JsonNode> backupJsons) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> restore(Map<String, JsonNode> backupJsons, Map<RestoreMsg, String> data) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		if(ex == null) {
			return "BTHome (unmanaged): " + type + ": " + super.toString();
		} else {
			return "BTHome (unmanaged): " + super.toString() + " Error: " + ex.getMessage();
		}
	}
}