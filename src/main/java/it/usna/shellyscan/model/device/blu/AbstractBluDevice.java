package it.usna.shellyscan.model.device.blu;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.DiviceInterface;
import it.usna.shellyscan.model.device.InetAddressAndPort;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public abstract class AbstractBluDevice implements DiviceInterface {
	protected final ShellyAbstractDevice parent;
	private final String index;
	
//	private final InetAddress parentAddress;
//	private final int parentPort;
	protected final InetAddressAndPort addressAndPort;
	private final String mac;
	private String name;
	private int rssi;
	private int lastConnection;
	private int battery;
	
	protected AbstractBluDevice(ShellyAbstractDevice parent, JsonNode info, String index) throws IOException {
		this.parent = parent;
		this.index = index;
		this.addressAndPort = parent.getAddressAndPort();
//		this.parentAddress = parent.getAddress();
//		this.parentPort = parent.getPort();
		
		final JsonNode config = info.path("config");
		this.mac = config.path("addr").asText();
		fillSettings(config);
		fillStatus(info.path("status"));
	}

	public abstract String getTypeID();
	
	public abstract String getTypeName();
	

	@Override
	public String getHostname() {
		return getTypeID() + "-" + mac;
	}
	
	@Override
	public InetAddressAndPort getAddressAndPort() {
		return addressAndPort;
	}
	
	public Status getStatus() {
		return Status.BLU;
	}

	public void fillSettings() throws IOException {
		fillSettings(parent.getJSON("/rpc/BTHomeDevice.GetConfig?id=" + index));
	}
	
	public void fillStatus() throws IOException {
		fillSettings(parent.getJSON("/rpc/BTHomeDevice.GetStatus?id=" + index));
	}
	
	protected void fillSettings(JsonNode config) {
		this.name = config.path("name").asText();
	}
	
	protected void fillStatus(JsonNode status) {
		this.rssi = status.path("rssi").intValue();
		this.battery = status.path("battery").intValue();
		this.lastConnection = status.path("last_updated_ts").intValue();
	}
	
	public int getRssi() {
		return rssi;
	}
	
	public long getLastTime() {
		return lastConnection;
	}
	
	public int getBattery() {
		return battery;
	}
	
	public Meters[] getMeters() {
		return null;
	}
	
	public String getName() {
		return name == null ? "" : name;
	}
	
	public String getMacAddress() {
		return mac;
	}
	
	@Override
	public String toString() {
		return getTypeName() + "-" + name + ":" + mac;
	}
}

/*
{
    "key" : "bthomedevice:200",
    "status" : {
      "id" : 200,
      "rssi" : -63,
      "battery" : 100,
      "packet_id" : 209,
      "last_updated_ts" : 1726738130
    },
    "config" : {
      "id" : 200,
      "addr" : "b0:c7:xx:xx:xx:xx",
      "name" : "h&t",
      "key" : null,
      "meta" : {
        "ui" : {
          "view" : "regular",
          "local_name" : "SBHT-003C",
          "icon" : null
        }
      }
    }
  }
  
  "local_name" : "SBHT-003C" -> h&t
  "local_name" : "SBBT-002C" -> button
*/