package it.usna.shellyscan.model.device.g3;

import java.net.InetAddress;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;

/**
 * Base class for any gen3 Shelly device
 * @author usna
 */
public abstract class AbstractG3Device extends AbstractG2Device {
//	private final static Logger LOG = LoggerFactory.getLogger(AbstractG3Device.class);

	protected AbstractG3Device(InetAddress address, int port, String hostname) {
		super(address, port, hostname);
	}
	
//	@Override
//	public String[] getInfoRequests() {
//		return new String[] {
//				"/rpc/Shelly.GetDeviceInfo?ident=true", "/rpc/Shelly.GetConfig", "/rpc/Shelly.GetStatus", "/rpc/Shelly.CheckForUpdate", "/rpc/Schedule.List", "/rpc/Webhook.List",
//				"/rpc/Script.List", "/rpc/WiFi.ListAPClients" /*, "/rpc/Sys.GetStatus",*/, "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents",
//				/*"/rpc/BTHome.GetConfig", "/rpc/BTHome.GetStatus"*/};
//	}
	
//	@Override
//	public boolean backup(final File file) throws IOException {
//		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
//			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", out);
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", out);
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			try { //unmanaged battery device
//				sectionToStream("/rpc/Schedule.List", "Schedule.List.json", out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			} catch(Exception e) {}
//			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			try {
//				sectionToStream("/rpc/KVS.GetMany", "KVS.GetMany.json", out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			} catch(Exception e) {}
//			byte[] scripts = null;
//			try {
//				scripts = sectionToStream("/rpc/Script.List", "Script.List.json", out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			} catch(Exception e) {}
//			try { // Virtual components
//				sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			} catch(Exception e) {}
//			try { // On device with active sensor add-on
//				sectionToStream("/rpc/SensorAddon.GetPeripherals", SensorAddOn.BACKUP_SECTION, out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			} catch(Exception e) {}
//			try { // On X devices
//				sectionToStream("/rpc/XMOD.GetInfo", "XMOD.GetInfo.json", out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			} catch(Exception e) {}
//			// Scripts
//			if(scripts != null) {
//				JsonNode scrList = jsonMapper.readTree(scripts).get("scripts");
//				for(JsonNode scr: scrList) {
//					try {
//						Script script = new Script(this, scr);
//						byte[] code =  script.getCode().getBytes();
//						ZipEntry entry = new ZipEntry(scr.get("name").asText() + ".mjs");
//						out.putNextEntry(entry);
//						out.write(code, 0, code.length);
//					} catch(IOException e) {}
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//				}
//			}
//		} catch(InterruptedException e) {
//			LOG.error("backup", e);
//		}
//		return true;
//	}
}