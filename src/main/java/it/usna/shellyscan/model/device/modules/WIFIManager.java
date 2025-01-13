package it.usna.shellyscan.model.device.modules;

public interface WIFIManager {
	enum Network {PRIMARY, SECONDARY, ETHERNET, UNKNOWN, AP}; 

	boolean isEnabled();
	
	String getSSID();
	
	boolean isStaticIP();
	
	String getIP();
	
	String getMask();
	
	String getGateway();
	
	String getDNS();
	
	String disable();

	/** set dhcp values */
	String set(String ssid, String pwd);

	/** set static ip values (if ip == null do not alter ip)*/
	String set(String ssid, String pwd, String ip, String netmask, String gw, String dns);

	String enableRoaming(boolean enable);

//	default String copyFrom(WIFIManager fromWF, String pwd) {
//		if(fromWF.isStaticIP()) {
//			return set(fromWF.getSSID(), pwd, fromWF.getIP(), fromWF.getMask(), fromWF.getGateway(), fromWF.getDNS());
//		} else {
//			return set(fromWF.getSSID(), pwd);
//		}
//	}
}