package it.usna.shellyscan.model.device.g2.modules;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.modules.WIFIManager;

public class WIFIManagerG2 implements WIFIManager {
	private String net;
	private final AbstractG2Device d;
	private boolean enabled;
	private String dSSID;
	private boolean staticIP;
	private String ip;
	private String netmask;
	private String gw;
	private String dns;
	
	public WIFIManagerG2(AbstractG2Device d, Network network) throws IOException {
		this.d = d;
		if(network != null) {
			if(network == Network.PRIMARY) {
				net = "sta";
			} else if(network == Network.SECONDARY) {
				net = "sta1";
			} else {
				net = "err";
			}
			init();
		}
	}
	
	public WIFIManagerG2(AbstractG2Device d, Network network, boolean noInit) throws IOException {
		net = (network == Network.PRIMARY) ? "sta" : "sta1";
		this.d = d;
		if(noInit == false) {
			init();
		}
	}
	
	private void init() throws IOException {
		// https://shelly-api-docs.shelly.cloud/gen2/Components/SystemComponents/WiFi
		//'{"id": 1, "method": "Wifi.SetConfig", "params": {"config": {"sta": {"ssid": "Shelly", "pass": "Shelly", "enable": true}}}}'
		JsonNode wifi = d.getJSON("/rpc/Wifi.GetConfig").get(net);
		enabled = wifi.get("enable").asBoolean();
		dSSID = wifi.get("ssid").asText("");
		staticIP = wifi.get("ipv4mode").asText().equals("static");
		ip = wifi.get("ip").asText("");
		netmask = wifi.get("netmask").asText("");
		gw = wifi.get("gw").asText("");
		dns = wifi.get("nameserver").asText("");
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String getSSID() {
		return dSSID;
	}

	@Override
	public boolean isStaticIP() {
		return staticIP;
	}

	@Override
	public String getIP() {
		return ip;
	}

	@Override
	public String getMask() {
		return netmask;
	}

	@Override
	public String getGateway() {
		return gw;
	}

	@Override
	public String getDNS() {
		return dns;
	}
	
	@Override
	public String disable() {
		return d.postCommand("Wifi.SetConfig", "{\"config\": {" + net + ":{\"enable\": false}}}");
	}

	@Override
	public String set(String ssid, String pwd) {
		ObjectNode pars = JsonNodeFactory.instance.objectNode();
		pars.put("ssid", ssid);
		pars.put("pass", pwd);
		pars.put("enable", true);
		pars.put("ipv4mode", "dhcp");
		ObjectNode network = JsonNodeFactory.instance.objectNode();
		network.set(net, pars);
		ObjectNode config = JsonNodeFactory.instance.objectNode();
		config.set("config", network);
		return d.postCommand("Wifi.SetConfig", config);
	}

	@Override
	public String set(String ssid, String pwd, String ip, String netmask, String gw, String dns) {
		ObjectNode pars = JsonNodeFactory.instance.objectNode();
		pars.put("ssid", ssid);
		pars.put("pass", pwd);
		pars.put("enable", true);
		pars.put("ipv4mode", "static");
		pars.put("ip", ip);
		if(netmask != null && netmask.isEmpty() == false) {
			pars.put("netmask", netmask);
		}
		if(gw != null && gw.isEmpty() == false) {
			pars.put("gw", gw);
		}
		if(dns != null && dns.isEmpty() == false) {
			pars.put("nameserver", dns);
		} else {
			pars.putNull("nameserver");
		}
		ObjectNode network = JsonNodeFactory.instance.objectNode();
		network.set(net, pars);
		ObjectNode config = JsonNodeFactory.instance.objectNode();
		config.set("config", network);
		return d.postCommand("Wifi.SetConfig", config);
	}
	
	@Override
	public String enableRoaming(boolean enable) {
		int interval = enable ? 60 : 0;
		return d.postCommand("WiFi.SetConfig", "{\"config\":{\"roam\":{\"interval\":" + interval + "}}}");
	}
	
	public static Network currentConnection(AbstractG2Device d) {
		try {
			JsonNode settings = d.getJSON("/rpc/Shelly.GetConfig").get("wifi");
			JsonNode sta;
			if((sta = settings.get("sta")).get("enable").asBoolean() && sta.get("ssid").asText("").equals(d.getSSID())) {
				return Network.PRIMARY;
			} else if((sta = settings.get("sta1")).get("enable").asBoolean() && sta.get("ssid").asText("").equals(d.getSSID())) {
				return Network.SECONDARY;
			} else if(settings.get("ap").get("enable").asBoolean()) {
				return Network.AP;
			} else {
				return Network.ETHERNET;
			}
		} catch(Exception e) {
			return Network.UNKNOWN;
		}
	}

	public String restore(JsonNode wifi, String pwd) {
		if(wifi.get("enable").asBoolean()) {
			if(wifi.get("ipv4mode").asText().equals("static")) {
				return set(wifi.get("ssid").asText(), pwd, wifi.get("ip").asText(), wifi.get("netmask").asText(""), wifi.get("gw").asText(""), wifi.get("nameserver").asText(""));
			} else {
				return set(wifi.get("ssid").asText(), pwd);
			}
		} else {
			return disable();
		}
	}
	
	// restore /wifi/ap & /wifi/roam
	public static String restoreAP_roam(AbstractG2Device d, JsonNode wifi, String pwd) {
		ObjectNode outWifi = JsonNodeFactory.instance.objectNode();

		JsonNode ap = (ObjectNode)wifi.path("ap");
		if(ap.isMissingNode() == false) {
			ObjectNode outAP = (ObjectNode)ap.deepCopy();
			outAP.remove("ssid");
			if(ap.path("is_open").booleanValue() == false) {
				outAP.put("pass", pwd);
			}
			outWifi.set("ap", outAP);
		}

		JsonNode roam = wifi.path("roam");
		if(roam.isMissingNode() == false) {
			outWifi.set("roam", roam.deepCopy());
		}
		
		if(outWifi.size() > 0) {
			ObjectNode outConfig = JsonNodeFactory.instance.objectNode();
			outConfig.set("config", outWifi);
			return d.postCommand("WiFi.SetConfig", outConfig);
		} else {
			return null;
		}
	}
	
	public static String enableAP(AbstractG2Device d, boolean enable) {
		return d.postCommand("WiFi.SetConfig", "{\"config\":{\"ap\":{\"enable\":" + enable + "}}}");
	}
}