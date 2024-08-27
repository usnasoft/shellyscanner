package it.usna.shellyscan.model.device.modules;

public interface TimeAndLocationManager {
	String getSNTPServer();
	
	String setSNTPServer(String server);
}

//default: time.google.com