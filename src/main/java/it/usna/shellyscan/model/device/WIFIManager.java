package it.usna.shellyscan.model.device;

public interface WIFIManager {
	public enum Network {PRIMARY, SECONDARY, AP}; 

	public boolean isEnabled();
	
	public String getSSID();
	
	public boolean isStaticIP();
	
	public String getIP();
	
	public String getMask();
	
	public String getGateway();
	
	public String getDNS();
	
	public String disable();
	
	/** set dhcp values */
	public String set(String ssid, String pwd);

	/** set static ip values (if ip == null do not alter ip)*/
	public String set(String ssid, String pwd, String ip, String netmask, String gw, String dns);
}
