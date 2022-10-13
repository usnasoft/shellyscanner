package it.usna.shellyscan.model.device.g2;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.usna.shellyscan.model.device.WIFIManager;

public class WIFIManagerG2 implements WIFIManager {
	private final String net;
	private final AbstractG2Device d;
	private boolean enabled;
	private String dSSID;
	private boolean staticIP;
	private String ip;
	private String netmask;
	private String gw;
	private String dns;
	
	public WIFIManagerG2(AbstractG2Device d, Network network) throws IOException {
		net = (network == Network.PRIMARY) ? "sta" : "sta1";
		this.d = d;
		init();
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
		JsonNodeFactory factory = new JsonNodeFactory(false);
		ObjectNode pars = factory.objectNode();
		pars.put("ssid", ssid);
		pars.put("pass", pwd);
		pars.put("enable", true);
		pars.put("ipv4mode", "dhcp");
		ObjectNode network = factory.objectNode();
		network.set(net, pars);
		ObjectNode config = factory.objectNode();
		config.set("config", network);
		return d.postCommand("Wifi.SetConfig", config);
	}
	
	// todo netmask
	@Override
	public String set(String ssid, String pwd, String ip, String netmask, String gw, String dns) {
		JsonNodeFactory factory = new JsonNodeFactory(false);
		ObjectNode pars = factory.objectNode();
		pars.put("ssid", ssid);
		pars.put("pass", pwd);
		pars.put("enable", true);
		pars.put("ipv4mode", "static");
		if(ip != null && ip.isEmpty() == false) {
			pars.put("ip", ip);
		}
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
		ObjectNode network = factory.objectNode();
		network.set(net, pars);
		ObjectNode config = factory.objectNode();
		config.set("config", network);
		return d.postCommand("Wifi.SetConfig", config);
	}
	
	
	// restore /wifi/ap & /wifi/roam
	public static String restoreAP_roam(AbstractG2Device d, JsonNode wifi, String pwd) {
		JsonNodeFactory factory = new JsonNodeFactory(false);
		ObjectNode outWifi = factory.objectNode();
		ObjectNode outAP = factory.objectNode();
		
		JsonNode enable = wifi.at("/ap/enable");
		outAP.put("enable", enable.asBoolean());
		if(enable.asBoolean()) {
			JsonNode open = wifi.at("/ap/is_open");
			outAP.put("is_open", open.asBoolean());
			if(open.asBoolean() == false && pwd != null) {
				outAP.put("pass", pwd);
			}
		}
		outWifi.set("ap", outAP);

		outWifi.set("roam", wifi.get("roam").deepCopy());
		
		ObjectNode outConfig = factory.objectNode();
		outConfig.set("config", outWifi);
		return d.postCommand("WiFi.SetConfig", outConfig);
	}
	
	public static Network currentConnection(AbstractG2Device d) throws IOException {
		JsonNode settings = d.getJSON("/rpc/Shelly.GetConfig").get("wifi");
		JsonNode sta;
		if((sta = settings.get("sta")).get("enable").asBoolean() && sta.get("ssid").asText("").equals(d.getSSID())) {
			return Network.PRIMARY;
		} else if((sta = settings.get("sta1")).get("enable").asBoolean() && sta.get("ssid").asText("").equals(d.getSSID())) {
			return Network.SECONDARY;
		}
		return Network.AP;
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
}