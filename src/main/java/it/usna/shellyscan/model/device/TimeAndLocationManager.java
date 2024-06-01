package it.usna.shellyscan.model.device;

import java.io.IOException;

public interface TimeAndLocationManager {
	public String getSNTPServer() throws IOException;
	
	public String setSNTPServer(String server) /*throws IOException*/;
}

//default: time.google.com