package it.usna.shellyscan.model.device.g2.modules;

import it.usna.shellyscan.model.device.modules.InputInterface;

public interface InputActionInterface extends InputInterface {

	void associateWH(Webhooks webhooks);
}
