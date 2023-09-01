package it.usna.shellyscan.model.device;

/**
 * This interface identify Shelly devices not fully managed
 * @author usna
 */
public interface ShellyUnmanagedDevice {
	/**
	 * @return null if device type is unknown or exception if an error ha occurred on construction 
	 */
	public Throwable getException();
}
