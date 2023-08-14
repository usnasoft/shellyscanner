package it.usna.shellyscan.model;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.ShellyUnmanagedDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

/**
 * Devices model intended for CLI non iteractive use
 */
public class NonInteractiveDevices implements Closeable {
	private final static Logger LOG = LoggerFactory.getLogger(NonInteractiveDevices.class);
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();


//	private final static int EXECUTOR_POOL_SIZE = 64;
	public final static long MULTI_QUERY_DELAY = 59;

	private JmmDNS jd;
	private Set<JmDNS> bjServices = new HashSet<>();

	private byte[] baseScanIP;
	private int lowerIP;
	private int higherIP;

	private final static String SERVICE_TYPE1 = "_http._tcp.local.";
//	private final static String SERVICE_TYPE2 = "_shelly._tcp.local.";
	private final List<ShellyAbstractDevice> devices = new ArrayList<>();

//	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
	private HttpClient httpClient = new HttpClient();
	private WebSocketClient wsClient = new WebSocketClient(httpClient);
	
	public NonInteractiveDevices() throws Exception {
		httpClient.start();
		wsClient.start();
	}

	public void scannerInit(boolean fullScan) throws IOException {
		if(fullScan) {
			jd = JmmDNS.Factory.getInstance();
			for(JmDNS dns: jd.getDNS()) {
				bjServices.add(dns);
				LOG.debug("{} {}", dns.getName(), dns.getInetAddress());
			}
		} else {
			final JmDNS dns = JmDNS.create(/*network == null ? InetAddress.getLocalHost() : network*//*InetAddress.getLocalHost()*/null, null);
			bjServices.add(dns);
			LOG.debug("Local scan: {} {}", dns.getName(), dns.getInetAddress());
		}
	}

	public void scannerInit(final byte[] ip, int first, final int last, int refreshInterval, int refreshTics) throws IOException {
		this.baseScanIP = ip;
		this.lowerIP = first;
		this.higherIP = last;
		LOG.debug("ip scan: {} {} {}", ip, first, last);
//		scanByIP();
	}

	private void scanByIP() throws IOException {
		for(/*int dalay = 0,*/ int ip4 = lowerIP; ip4 <= higherIP; /*dalay +=4,*/ ip4++) {
			baseScanIP[3] = (byte)ip4;
			final InetAddress addr = InetAddress.getByAddress(baseScanIP);
//			executor.schedule(() -> {
				try {
					if(addr.isReachable(4500)) {
//						Thread.sleep(MULTI_QUERY_DELAY);
						JsonNode info = isShelly(addr, 80);
						if(info != null) {
//							Thread.sleep(MULTI_QUERY_DELAY);
							create(addr, 80, info, addr.getHostAddress());
						}
					} else {
						LOG.trace("no ping {}", addr);
					}
				} catch (TimeoutException e) {
					LOG.trace("timeout {}", addr);
				} catch (IOException /*| InterruptedException*/ e) {
					LOG.error("ip scan error {} {}", addr, e.toString());
				}
//			}, dalay, TimeUnit.MILLISECONDS);
		}
	}

	private JsonNode isShelly(final InetAddress address, int port) throws TimeoutException {
		try {
			ContentResponse response = httpClient.newRequest("http://" + address.getHostAddress() + ":" + port + "/shelly").timeout(15, TimeUnit.SECONDS).send();
			JsonNode shellyNode = JSON_MAPPER.readTree(response.getContent());
			int resp = response.getStatus();
			if(resp == HttpStatus.OK_200 && shellyNode.has("mac")) { // "mac" is common to all shelly devices
				return shellyNode;
			} else {
				LOG.trace("Not Shelly {}, resp {}, node ()", address, resp, shellyNode);
				return null;
			}
		} catch (InterruptedException | ExecutionException | IOException e) {
			LOG.trace("Not Shelly {} - {}", address, e.toString());
			return null;
		}
	}

	public void execute(Consumer<ShellyAbstractDevice> c) throws IOException {
		LOG.trace("rescan");
		clear();
		if(this.baseScanIP == null) {
			for(JmDNS bonjourService: bjServices) {
				LOG.debug("scanning: {} {}", bonjourService.getName(), bonjourService.getInetAddress());
				final ServiceInfo[] serviceInfos = bonjourService.list(SERVICE_TYPE1);
				for (ServiceInfo info: serviceInfos) {
					final String name = info.getName();
					if(name.startsWith("shelly") || name.startsWith("Shelly")) { // ShellyBulbDuo-xxx
//						executor.execute(() -> create(info.getInetAddresses()[0], 80, null, name));
						create(info.getInetAddresses()[0], 80, null, name);
					}
				}
			}
		} else {
			scanByIP();
		}
		for(ShellyAbstractDevice d: devices ) {
			c.accept(d);
		}
		LOG.debug("end scan");
	}

	private void create(InetAddress address, int port, JsonNode info, String hostName) {
		LOG.trace("Creating {} - {}", address, hostName);
		try {
			ShellyAbstractDevice d = DevicesFactory.create(httpClient, wsClient, address, port, info, hostName);
			if(/*d != null &&*/ Thread.interrupted() == false) {
				synchronized(devices) {
					int ind = devices.indexOf(d);
					if(ind >= 0) {
						if(d instanceof ShellyUnmanagedDevice == false || devices.get(ind) instanceof ShellyUnmanagedDevice) { // Do not replace device if was recocnized and now is not
							devices.set(ind, d);
						}
					} else {
						devices.add(d);
					}
				}
				LOG.debug("Create {} - {}", address, d);

				if(d instanceof AbstractG2Device && (((AbstractG2Device)d).isExtender() || d.getStatus() == Status.NOT_LOOGGED)) {
					((AbstractG2Device)d).getRangeExtenderManager().getPorts().forEach(p -> {
						try {
//							executor.execute(() -> {
								try {
									JsonNode infoEx = isShelly(address, p);
									if(infoEx != null) {
										create(d.getAddress(), p, infoEx, d.getHostname() + "-EX" + ":" + p); // fillOnce will later correct hostname
									}
								} catch (TimeoutException | RuntimeException e) {
									LOG.debug("timeout {}:{}", d.getAddress(), p, e);
								}
//							});
						} catch(RuntimeException e) {
							LOG.error("Unexpected-add-ext: {}; host: {}:{}", address, hostName, p, e);
						}
					});
				}
			}
		} catch(Exception e) {
			LOG.error("Unexpected-add: {}; host: {}", address, hostName, e);
		}
	}

//	public ShellyAbstractDevice get(int ind) {
//		return devices.get(ind);
//	}

//	public int size() {
//		return devices.size();
//	}

	private void clear() {
//		executor.shutdownNow(); // full clean instead of refreshProcess.forEach(f -> f.cancel(true));
//		executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
		devices.clear();
	}

	@Override
	public void close() {
		LOG.trace("Model closing");
//		executor.shutdownNow();
		bjServices.parallelStream().forEach(dns -> {
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
		try {
			wsClient.stop();
		} catch (Exception e) {
			LOG.error("webSocketClient.stop", e);
		}
		try {
			httpClient.stop();
		} catch (Exception e) {
			LOG.error("httpClient.stop", e);
		}
		LOG.debug("Model closed");
	}
}