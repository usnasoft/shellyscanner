package it.usna.shellyscan.model.device.g2.modules;

import java.util.Map;

import it.usna.shellyscan.model.device.g2.modules.Webhooks.Webhook;
import it.usna.shellyscan.model.device.modules.InputInterface;

public interface InputActionInterface extends InputInterface{

	void associateWH(Webhooks webhooks);
	
	void associateWH(Map<String, Webhook> wh);
}
