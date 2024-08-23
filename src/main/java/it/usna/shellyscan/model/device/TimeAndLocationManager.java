package it.usna.shellyscan.model.device;

public interface TimeAndLocationManager {
	public String getSNTPServer();
	
	public String setSNTPServer(String server) /*throws IOException*/;
}

//default: time.google.com