package it.usna.shellyscan.model;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.blu.BluTRV;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.AbstractProDevice;
import it.usna.shellyscan.model.device.g3.AbstractG3Device;

/**
 * Devices model intended for CLI non iteractive use
 */
public class NonInteractiveDevices implements Closeable {
	private final static Logger LOG = LoggerFactory.getLogger(NonInteractiveDevices.class);
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

	private final static int EXECUTOR_POOL_SIZE = 64;

	private JmmDNS jd;
	private Set<JmDNS> bjServices = new HashSet<>();

	private IPCollection ipCollection = null;

	private final static String SERVICE_TYPE1 = "_http._tcp.local.";
//	private final static String SERVICE_TYPE2 = "_shelly._tcp.local.";
	private final List<ShellyAbstractDevice> devices = new ArrayList<>();

	private HttpClient httpClient = new HttpClient();
//	private WebSocketClient wsClient = new WebSocketClient(httpClient);
	
	private NonInteractiveDevices() throws Exception {
		httpClient.setDestinationIdleTimeout(300_000); // 5 min
		httpClient.setMaxConnectionsPerDestination(2);
		httpClient.start();
//		wsClient.start();
	}
	
	public NonInteractiveDevices(boolean fullScan) throws Exception {
		this();
		scannerInit(fullScan);
	}
	
	public NonInteractiveDevices(IPCollection ipCollection) throws Exception {
		this();
		scannerInit(ipCollection);
	}

	public void scannerInit(boolean fullScan) throws IOException {
		if(fullScan) {
			jd = JmmDNS.Factory.getInstance();
			try { Thread.sleep(2000); } catch (InterruptedException e) { } // hoping this time is enough to fill list 
			for(JmDNS dns: jd.getDNS()) {
				bjServices.add(dns);
				LOG.debug("Full scan {} {}", dns.getName(), dns.getInetAddress());
			}
		} else {
			final JmDNS dns = JmDNS.create(null, null);
			bjServices.add(dns);
			LOG.debug("Local scan: {} {}", dns.getName(), dns.getInetAddress());
		}
	}

	public void scannerInit(IPCollection ipCollection) throws IOException {
		this.ipCollection = ipCollection;
		LOG.debug("IP scan: {}", ipCollection);
	}
	
	private void scanByIP(Consumer<ShellyAbstractDevice> c) throws IOException {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
		try {
			int dalay = 0;
			for(InetAddress addr: ipCollection) {
				executor.schedule(() -> {
					try {
						if(addr.isReachable(5_000)) {
							Thread.sleep(Devices.MULTI_QUERY_DELAY);
							JsonNode info = isShelly(addr, 80);
							if(info != null) {
								Thread.sleep(Devices.MULTI_QUERY_DELAY);
								create(addr, 80, info, addr.getHostAddress(), c);
							}
						} else {
							LOG.trace("no ping {}", addr);
						}
					} catch (TimeoutException e) {
						LOG.trace("timeout {}", addr);
					} catch (IOException | InterruptedException e) {
						LOG.error("ip scan error {} {}", addr, e.toString());
					}
				}, dalay, TimeUnit.MILLISECONDS);
				dalay += 4;
			}
			executor.shutdown();
			executor.awaitTermination(60, TimeUnit.MINUTES);
		} catch (Exception e) {
			LOG.error("ip scan error {}", e.toString());
		}
	}

	private JsonNode isShelly(final InetAddress address, int port) throws TimeoutException {
		// if(name.startsWith("shelly") || name.startsWith("Shelly")) { // Shelly X devices can have different names
		try {
			ContentResponse response = httpClient.newRequest("http://" + address.getHostAddress() + ":" + port + "/shelly").timeout(80, TimeUnit.SECONDS).method(HttpMethod.GET).send();
			JsonNode shellyNode = null;// = JSON_MAPPER.readTree(response.getContent());
			int resp = response.getStatus();
			if(resp == HttpStatus.OK_200 && (shellyNode = JSON_MAPPER.readTree(response.getContent())).has("mac")) { // "mac" is common to all shelly devices
				return shellyNode;
			} else {
				LOG.trace("Not Shelly {}, status {}, node {}", address, resp, shellyNode);
				return null;
			}
		} catch (InterruptedException | ExecutionException | IOException e) { // SocketTimeoutException extends IOException
			LOG.trace("Not Shelly {} - {}", address, port, e);
			return null;
		}
	}

	public void execute(Consumer<ShellyAbstractDevice> c) throws IOException {
		LOG.trace("scan");
		if(this.ipCollection == null) {
			for(JmDNS bonjourService: bjServices) {
				LOG.debug("scanning: {} {}", bonjourService.getName(), bonjourService.getInetAddress());
				final ServiceInfo[] serviceInfos = bonjourService.list(SERVICE_TYPE1);
				for (ServiceInfo dnsInfo: serviceInfos) {
					final String name = dnsInfo.getName();
					try {
						JsonNode info = isShelly(dnsInfo.getInetAddresses()[0], 80);
						if(info != null) {
							create(dnsInfo.getInetAddresses()[0], 80, info, name, c);
						}
					} catch (TimeoutException e) {
						LOG.error("scan", e);
					}
				}
			}
		} else {
			scanByIP(c);
		}
		LOG.debug("end scan");
	}
	
	private void create(InetAddress address, int port, JsonNode info, String hostName, Consumer<ShellyAbstractDevice> consumer) {
		LOG.trace("Creating {}:{} - {}", address, port, hostName);
		try {
			ShellyAbstractDevice d = DevicesFactory.create(httpClient, /*wsClient*/null, address, port, info, hostName);
			if(devices.contains(d) == false) {
				devices.add(d);
				consumer.accept(d);
				LOG.debug("Create {}:{} - {}", address, port, d);

				// Rage extender
				if(/*port == 80 &&*/ d instanceof AbstractG2Device gen2 && (gen2.isExtender() || gen2.getStatus() == Status.NOT_LOOGGED)) {
					gen2.getRangeExtenderManager().getPorts().forEach(p -> {
						try {
							JsonNode infoEx = isShelly(address, p);
							if(infoEx != null) {
								create(d.getAddressAndPort().getAddress(), p, infoEx, d.getHostname() + "-EX" + ":" + p, consumer); // device will later correct hostname
							}
						} catch (TimeoutException | RuntimeException e) {
							LOG.debug("timeout {}:{}", d.getAddressAndPort().getAddress(), p, e);
						}
					});
				}
				// BTHome (BLU)
				if(d instanceof AbstractProDevice || d instanceof AbstractG3Device) {
					final JsonNode currenteComponents = d.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components"); // empty on 401
					for(JsonNode compInfo: currenteComponents) {
						String key = compInfo.path("key").asText();
						if(key.startsWith(AbstractBluDevice.DEVICE_KEY_PREFIX) || key.startsWith(BluTRV.DEVICE_KEY_PREFIX)) {
							AbstractBluDevice newBlu = DevicesFactory.createBlu((AbstractG2Device)d, httpClient, /*wsClient,*/ compInfo, key);
							consumer.accept(newBlu);
						}
					}
				}
			}
		} catch(Exception e) {
			LOG.error("Unexpected-add: {}:{}; host: {}", address, port, hostName, e);
		}
	}

	@Override
	public void close() {
		LOG.trace("Model closing");
		bjServices.stream().forEach(dns -> {
			try {
				dns.close();
			} catch (IOException e) {
				LOG.error("closing JmDNS", e);
			}
		});
		if(jd != null) {
			try {
				jd.close();
			} catch (IOException e) {
				LOG.error("closing JmmDNS", e);
			}
		}
//		try {
//			wsClient.stop();
//		} catch (Exception e) {
//			LOG.error("webSocketClient.stop", e);
//		}
		try {
			httpClient.stop();
		} catch (Exception e) {
			LOG.error("httpClient.stop", e);
		}
		LOG.debug("Model closed");
	}
}