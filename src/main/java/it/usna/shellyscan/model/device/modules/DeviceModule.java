package it.usna.shellyscan.model.device.modules;

import it.usna.shellyscan.model.device.LabelHolder;

public interface DeviceModule extends LabelHolder {
	default String getLastSource() {return null;}
}
