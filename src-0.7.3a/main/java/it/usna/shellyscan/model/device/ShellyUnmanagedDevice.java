package it.usna.shellyscan.model.device;

public interface ShellyUnmanagedDevice {
	/**
	 * @return null if device type is unknown or exception if an error ha occurred on construction 
	 */
	public Exception geException();
}
