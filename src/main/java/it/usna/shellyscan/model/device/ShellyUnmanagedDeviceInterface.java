package it.usna.shellyscan.model.device;

/**
 * This interface identify Shelly devices not fully managed
 * @author usna
 */
public interface ShellyUnmanagedDeviceInterface {
	/**
	 * @return null if device type is unknown or an error has occurred on construction 
	 */
	Throwable getException();
}
