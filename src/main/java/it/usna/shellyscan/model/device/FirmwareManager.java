package it.usna.shellyscan.model.device;

import java.io.IOException;

public interface FirmwareManager {
	public void chech() throws IOException;
	
	public String current();
	
	public String newBeta();
	
	public String newStable();
	
	public String update(boolean stable);
	
	public boolean upadating();
}