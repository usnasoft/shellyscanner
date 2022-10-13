package it.usna.shellyscan.model.device.g2;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.client.CredentialsProvider;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.g2.modules.Relay;
import it.usna.shellyscan.model.device.modules.RelayCommander;
import it.usna.shellyscan.model.device.modules.RelayInterface;

public class ShellyPlus1 extends AbstractG2Device implements RelayCommander, InternalTmpHolder {
	public final static String ID = "Plus1";
	private Relay relay = new Relay(this, 0);
	private float internalTmp;

	public ShellyPlus1(InetAddress address, CredentialsProvider credentialsProv) throws IOException {
		super(address, credentialsProv);
		fillOnce();
		fillSettings(getJSON("/rpc/Shelly.GetConfig"));
		fillStatus(getJSON("/rpc/Shelly.GetStatus"));
	}
	
	@Override
	public String getTypeName() {
		return "Shelly 1 plus";
	}
	
	@Override
	public Relay getRelay(int index) {
		return relay;
	}
	
	@Override
	public RelayInterface[] getRelays() {
		return new Relay[] {relay};
	}
	
	@Override
	public float getInternalTmp() {
		return internalTmp;
	}
	
	@Override
	protected void fillSettings(JsonNode configuration) throws IOException {
		super.fillSettings(configuration);
		relay.fillSettings(configuration.get("switch:0"));
//		System.out.println("fill");
	}
	
	@Override
	protected void fillStatus(JsonNode status) throws IOException {
		super.fillStatus(status);
		JsonNode switchStatus = status.get("switch:0");
		relay.fillStatus(switchStatus);
		internalTmp = (float)switchStatus.get("temperature").get("tC").asDouble();
//		System.out.println("status");
	}

	@Override
	protected void restore(JsonNode configuration, ArrayList<String> errors) throws IOException {
		relay.restore(configuration, errors);
	}
	
	@Override
	public String toString() {
		return super.toString() + " Relay: " + relay;
	}
}