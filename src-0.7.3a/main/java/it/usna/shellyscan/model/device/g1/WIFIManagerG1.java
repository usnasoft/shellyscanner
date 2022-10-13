package it.usna.shellyscan.model.device.g1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.WIFIManager;

public class WIFIManagerG1 implements WIFIManager {
	private final String net;
	private final AbstractG1Device d;
	private boolean enabled;
	private String dSSID;
	private boolean staticIP;
	private String ip;
	private String netmask;
	private String gw;
	private String dns;
	
	public WIFIManagerG1(AbstractG1Device d, Network network) throws IOException {
		net = (network == Network.PRIMARY) ? "sta" : "sta1";
		this.d = d;
		init();
	}
	
	private void init() throws IOException {
		JsonNode sta = d.getJSON("/settings/" + net);
		enabled = sta.get("enabled").asBoolean();
		dSSID = sta.path("ssid").asText();
		staticIP = sta.path("ipv4_method").asText().equals("static");
		ip = sta.path("ip").asText("");
		netmask = sta.path("mask").asText("");
		gw = sta.path("gw").asText("");
		dns = sta.path("dns").asText("");
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
		return d.sendCommand("/settings/sta1?enabled=false");
	}

	@Override
	public String set(String ssid, String pwd) {
		try {
			String cmd = "/settings/" + net + "?enabled=true" +
					"&ssid=" + URLEncoder.encode(ssid, StandardCharsets.UTF_8.name()) +
					"&key=" + URLEncoder.encode(pwd, StandardCharsets.UTF_8.name()) +
					"&ipv4_method=dhcp";
			return d.sendCommand(cmd);
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}
	
	@Override
	public String set(String ssid, String pwd, String ip, String netmask, String gw, String dns) {
		try {
			String cmd = "/settings/" + net + "?enabled=true" +
					"&ssid=" + URLEncoder.encode(ssid, StandardCharsets.UTF_8.name()) +
					"&key=" + URLEncoder.encode(pwd, StandardCharsets.UTF_8.name()) +
					"&ipv4_method=static" +
					"&netmask=" + netmask +
					"&gateway=" + gw +
					"&dns=" + dns;
			if(ip != null && ip.isEmpty() == false) {
				cmd += "&ip=" + ip;
			}
			return d.sendCommand(cmd);
		} catch (UnsupportedEncodingException e) {
			return e.getMessage();
		}
	}
	
//	restore
//	final JsonNode sta1 = settings.get("wifi_sta1"); // password not restored
//	if(sta1.get("enabled").asBoolean()) {
//		errors.add(sendCommand("/settings/sta1?enabled=true&" + jsonNodeToURLPar(sta1, "ssid", "ipv4_method", "ip", "gw", "mask", "dns")));
//	} else {
//		errors.add(sendCommand("/settings/sta1?enabled=false"));
//	}
}
