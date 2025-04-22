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
//				"/rpc/Script.List", "/rpc/WiFi.ListAPClients", "/rpc/KVS.GetMany", "/rpc/Shelly.GetComponents", "/rpc/BLE.CloudRelay.ListInfos"/*, "/rpc/KNX.GetConfig"*/};
//	}
	
//	@Override
//	// add LoRa to AbstractG2Device.backup
//	public boolean backup(final File file) throws IOException {
//		try(ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file), StandardCharsets.UTF_8)) {
//			sectionToStream("/rpc/Shelly.GetDeviceInfo", "Shelly.GetDeviceInfo.json", out);
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			JsonNode config = jsonMapper.readTree(sectionToStream("/rpc/Shelly.GetConfig", "Shelly.GetConfig.json", out));
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			try { // unmanaged battery device
//				sectionToStream("/rpc/Schedule.List", "Schedule.List.json", out);
//			} catch(Exception e) {}
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			sectionToStream("/rpc/Webhook.List", "Webhook.List.json", out);
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			try {
//				sectionToStream("/rpc/KVS.GetMany", "KVS.GetMany.json", out);
//			} catch(Exception e) {}
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			byte[] scripts = null;
//			try {
//				scripts = sectionToStream("/rpc/Script.List", "Script.List.json", out);
//			} catch(Exception e) {}
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			try { // Virtual components (PRO & gen3)
//				sectionToStream("/rpc/Shelly.GetComponents?dynamic_only=true", "Shelly.GetComponents.json", out);
//			} catch(Exception e) {}
//			TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			String addon = config.get("sys").get("device").path("addon_type").asText();
//			if(SensorAddOn.ADDON_TYPE.equals(addon)) {
//				sectionToStream("/rpc/SensorAddon.GetPeripherals", SensorAddOn.BACKUP_SECTION, out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			}
//			if(LoRaAddOn.ADDON_TYPE.equals(addon)) {
//				sectionToStream("/rpc/LoRa.GetConfig?id=" + LoRaAddOn.ID, LoRaAddOn.BACKUP_SECTION, out);
//				TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//			}
//			// Scripts
//			if(scripts != null) {
//				for(Script script: Script.list(this, jsonMapper.readTree(scripts))) {
//					try {
//						byte[] code =  script.getCode().getBytes();
//						ZipEntry entry = new ZipEntry(script.getName() + ".mjs");
//						out.putNextEntry(entry);
//						out.write(code, 0, code.length);
//					} catch(IOException e) {}
//					TimeUnit.MILLISECONDS.sleep(Devices.MULTI_QUERY_DELAY);
//				}
//			}
//			try { // Device specific
//				backup(out);
//			} catch(Exception e) {
//				LOG.error("backup specific", e);
//			}
//		} catch(InterruptedException e) {
//			LOG.error("backup", e);
//		}
//		return true;
//	}
}