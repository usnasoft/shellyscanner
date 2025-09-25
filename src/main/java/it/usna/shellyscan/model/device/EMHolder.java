package it.usna.shellyscan.model.device;

import it.usna.shellyscan.model.device.g2.modules.EM1Manager;

// todo EM1Manager & EMManager will emplement a common interface
public interface EMHolder {
	EM1Manager getEM();
}
