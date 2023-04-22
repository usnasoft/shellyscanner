package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.ShellyUnmanagedDevice;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;

public class Devices extends it.usna.util.UsnaObservable<Devices.EventType, Integer> {
	private final static Logger LOG = LoggerFactory.getLogger(Devices.class);
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	
	public enum EventType {
		ADD,
		UPDATE,
		REMOVE,
		READY, // Model is ready
		CLEAR // Clear model
	};

	private final static int EXECUTOR_POOL_SIZE = 128;
	public final static long MULTI_QUERY_DELAY = 59;

	private JmmDNS jd;
	private Set<JmDNS> bjServices = new HashSet<>();

	private byte[] baseScanIP;
	private int lowerIP;
	private int higherIP;

	private final static String SERVICE_TYPE1 = "_http._tcp.local.";
//	private final static String SERVICE_TYPE2 = "_shelly._tcp.local.";
	private final List<ShellyAbstractDevice> devices = new ArrayList<>();
	private final List<ScheduledFuture<?>> refreshProcess = new ArrayList<>();
	private int refreshInterval = 1000;
	private int refreshTics = 3; // full refresh every STATUS_TICS refresh

	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
	private HttpClient httpClient = new HttpClient();
	private WebSocketClient wsClient = new WebSocketClient(httpClient);
	
	public Devices() throws Exception {
		httpClient.start();
		wsClient.start();
	}

	public void scannerInit(boolean fullScan, int refreshInterval, int refreshTics) throws IOException {
		this.refreshInterval = refreshInterval;
		this.refreshTics = refreshTics;
		final MDNSListener dnsListener = new MDNSListener();
		if(fullScan) {
			jd = JmmDNS.Factory.getInstance();
			for(JmDNS dns: jd.getDNS()) {
				bjServices.add(dns);
				LOG.debug("{} {}", dns.getName(), dns.getInetAddress());
				dns.addServiceListener(SERVICE_TYPE1, dnsListener);
//				dns.addServiceListener(SERVICE_TYPE2, dnsListener);
			}
			jd.addNetworkTopologyListener(new NetworkTopologyListener() {
				@Override
				public void inetAddressRemoved(NetworkTopologyEvent event) {
					JmDNS dns = event.getDNS();
					LOG.debug("DNS remove {}", dns.getName());
					bjServices.remove(dns);
				}

				@Override
				public void inetAddressAdded(NetworkTopologyEvent event) {
					JmDNS dns = event.getDNS();
					try {
						LOG.debug("DNS add {} {}", dns.getName(), dns.getInetAddress());
						bjServices.add(dns);
						dns.addServiceListener(SERVICE_TYPE1, dnsListener);
//						dns.addServiceListener(SERVICE_TYPE2, dnsListener);
					} catch (IOException e) {
						LOG.error("DNS add {}", dns.getName(), e);
					}
				}
			});
		} else {
			final JmDNS dns = JmDNS.create(/*network == null ? InetAddress.getLocalHost() : network*//*InetAddress.getLocalHost()*/null, null);
			bjServices.add(dns);
			LOG.debug("Local scan: {} {}", dns.getName(), dns.getInetAddress());
			dns.addServiceListener(SERVICE_TYPE1, dnsListener);
//			dns.addServiceListener(SERVICE_TYPE2, dnsListener);
		}
		fireEvent(EventType.READY);
	}

	public void scannerInit(final byte[] ip, int first, final int last, int refreshInterval, int refreshTics) throws IOException {
		this.baseScanIP = ip;
		this.lowerIP = first;
		this.higherIP = last;
		this.refreshInterval = refreshInterval;
		this.refreshTics = refreshTics;
		LOG.debug("ip scan: {} {} {}", ip, first, last);
		scanByIP();
		fireEvent(EventType.READY);
	}
	
	public void setRefreshTime(int refreshInterval, int refreshTics) {
		this.refreshInterval = refreshInterval;
		this.refreshTics = refreshTics;
	}
	
	public void setIPInterval(int lower, int higher) {
		this.lowerIP = lower;
		this.higherIP = higher;
	}

	private void scanByIP() throws IOException {
		for(int dalay = 0, ip4 = lowerIP; ip4 <= higherIP; dalay +=4, ip4++) {
			baseScanIP[3] = (byte)ip4;
			final InetAddress addr = InetAddress.getByAddress(baseScanIP);
			executor.schedule(() -> {
				try {
					if(addr.isReachable(4500)) {
//						Thread.sleep(MULTI_QUERY_DELAY);
						JsonNode info = isShelly(addr, 80);
						if(info != null) {
							Thread.sleep(MULTI_QUERY_DELAY);
							create(addr, 80, info, addr.getHostAddress());
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
		}
	}

	private JsonNode isShelly(final InetAddress address, int port) throws TimeoutException {
		try {
//			ContentResponse response = httpClient.newRequest(/*"http://" +*/ address.getHostAddress() + "/shelly", port).timeout(15, TimeUnit.SECONDS).send();
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

	public void rescan() throws IOException {
		//LOG.trace("Q {}", ((ScheduledThreadPoolExecutor)executor).getQueue().size());
		//synchronized(devices) {
		LOG.trace("rescan");
		clear();
		fireEvent(EventType.CLEAR);
		if(this.baseScanIP == null) {
			if(jd == null && bjServices.size() == 1) { // local scan
				try {
					JmDNS dns = bjServices.iterator().next();
					if(dns.getInetAddress().equals(InetAddress.getLocalHost()) == false) {
						dns.close();
						bjServices.clear();
						dns = JmDNS.create(/*InetAddress.getLocalHost()*/null, null);
						LOG.debug("New local scan interface: {} {}", dns.getName(), dns.getInetAddress());
						bjServices.add(dns);
						dns.addServiceListener(SERVICE_TYPE1, new MDNSListener());
					}

				} catch(Exception e) {
					LOG.debug("local rescan {}", e);
				}
			}
			for(JmDNS bonjourService: bjServices) {
				LOG.debug("scanning: {} {}", bonjourService.getName(), bonjourService.getInetAddress());
				final ServiceInfo[] serviceInfos = bonjourService.list(SERVICE_TYPE1);
				for (ServiceInfo info: serviceInfos) {
					final String name = info.getName();
					if(name.startsWith("shelly") || name.startsWith("Shelly")) { // ShellyBulbDuo-xxx
						executor.execute(() -> create(info.getInetAddresses()[0], 80, null, name));
					}
				}
			}
		} else {
			scanByIP();
		}
		LOG.debug("end scan");
		fireEvent(EventType.READY);
		//}
	}

	public void refresh(int ind, boolean force) {
		synchronized(devices) {
			final ShellyAbstractDevice d = devices.get(ind);
			if(d.getStatus() != Status.READING || force) {
				refreshProcess.get(ind).cancel(true);
				d.setStatus(Status.READING);
				executor.schedule(() -> {
					if(d instanceof ShellyUnmanagedDevice && ((ShellyUnmanagedDevice)d).geException() != null) { // experimental; try to create proper device
						create(d.getAddress(), d.getPort(), null, d.getHostname());
					} else {
						try {
							d.refreshSettings();
							Thread.sleep(MULTI_QUERY_DELAY);
							d.refreshStatus();
						} catch (RuntimeException e) {
							LOG.error("Unexpected on refresh", e);
						} catch (IOException | InterruptedException e) {
							if(d.getStatus() == Status.ERROR) {
								LOG.error("Unexpected on refresh", e);
							} else {
								LOG.debug("refresh {} - {}", d, d.getStatus());
							}
						} finally {
							synchronized(devices) {
								if(refreshProcess.get(ind).isCancelled()) { // in case of many and fast "refresh"
									refreshProcess.set(ind, scheduleRefresh(d, ind, refreshInterval, refreshTics));
								}
							}
							updateViewRow(d, ind);
						}
					}
				}, MULTI_QUERY_DELAY, TimeUnit.MILLISECONDS);
			}
		}
	}

	public void pauseRefresh(int ind) {
		refreshProcess.get(ind).cancel(true);
	}

	// Before reboot disable refresh process
	public void reboot(int ind) {
		final ShellyAbstractDevice d = devices.get(ind);
		final ScheduledFuture<?> f = refreshProcess.get(ind);
		f.cancel(true);
		d.setStatus(Status.READING);
		updateViewRow(d, ind);
		executor.execute/*submit*/(() -> {
			try {
				d.reboot();
				d.setStatus(Status.READING);
				updateViewRow(d, ind);
				Thread.sleep(3000);
			} catch (Exception e) {
				if(d.getStatus() == Status.ERROR) {
					LOG.error("Unexpected on reboot", e);
				} else {
					LOG.debug("reboot {} - {}", d.toString(), d.getStatus());
				}
			} finally {
				synchronized(devices) {
					if(f.isCancelled()) { // in case of many and fast "refresh"
						refreshProcess.set(ind, scheduleRefresh(d, ind, refreshInterval, refreshTics));
					}
				}
			}
			updateViewRow(d, ind);
		});
	}

	private void updateViewRow(final ShellyAbstractDevice d, int ind) {
		if(Thread.interrupted() == false) {
			synchronized(devices) {
				if(devices.size() > ind && d == devices.get(ind)) { // underlying model unchanged (on rescan)
					fireEvent(EventType.UPDATE, ind);
				}
			}
		}
	}

	public void create(InetAddress address, int port, JsonNode info, String hostName) {
		LOG.trace("Creating {} - {}", address, hostName);
		try {
			ShellyAbstractDevice d = DevicesFactory.create(httpClient, wsClient, address, port, info, hostName);
			if(/*d != null &&*/ Thread.interrupted() == false) {
				synchronized(devices) {
					int ind;
					if((ind = devices.indexOf(d)) >= 0) {
						if(d instanceof ShellyUnmanagedDevice == false || devices.get(ind) instanceof ShellyUnmanagedDevice) { // Do not replace device if was recocnized and now is not
							refreshProcess.get(ind).cancel(true);
							devices.set(ind, d);
							fireEvent(EventType.UPDATE, ind);
							refreshProcess.set(ind, scheduleRefresh(d, ind, refreshInterval, refreshTics));
						}
					} else {
						final int idx = devices.size();
						devices.add(d);
						fireEvent(EventType.ADD, idx);
						refreshProcess.add(scheduleRefresh(d, idx, refreshInterval, refreshTics));
					}
				}
				LOG.debug("Create {} - {}", address, d);

				if(d instanceof AbstractG2Device && (((AbstractG2Device)d).isExtender() || d.getStatus() == Status.NOT_LOOGGED)) {
					((AbstractG2Device)d).getRangeExtenderManager().getPorts().forEach(p -> {
						try {
							executor.execute(() -> {
								try {
									JsonNode infoEx = isShelly(address, p);
									if(infoEx != null) {
										create(d.getAddress(), p, infoEx, d.getHostname() + "-EX" + ":" + p); // fillOnce will later correct hostname
									}
								} catch (TimeoutException | RuntimeException e) {
									LOG.debug("timeout {}:{}", d.getAddress(), p, e);
								}
							});
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

	private ScheduledFuture<?> scheduleRefresh(ShellyAbstractDevice d, int idx, final int interval, final int statusTics) {
		final Runnable refreshRunner = new Runnable() {
			private final Integer megIdx = Integer.valueOf(idx);
			private int ticCount = 0;

			@Override
			public void run() {
				try {
					if(++ticCount >= statusTics) {
						d.refreshSettings(); // if device is offline ticCount never goes to 0 -> full refresh if unsleep again
						ticCount = 0;
						Thread.sleep(MULTI_QUERY_DELAY);
					}
					d.refreshStatus();
				} catch (JsonProcessingException | RuntimeException e) {
					LOG.trace("Unexpected-refresh: {}", d, e);
				} catch (IOException | InterruptedException e) {}
				synchronized(devices) {
					if(devices.size() > idx && d == devices.get(idx) && Thread.interrupted() == false) { // underlying model unchanged (on rescan)
						fireEvent(EventType.UPDATE, megIdx);
					}
				}
			}
		};
		return executor.scheduleWithFixedDelay(refreshRunner, interval + idx, interval, TimeUnit.MILLISECONDS);
	}

	public ShellyAbstractDevice get(int ind) {
		return devices.get(ind);
	}

	public int size() {
		return devices.size();
	}

	private int indexOf(String hostname) {
		//synchronized(devices) {
		for(int i = 0; i < devices.size(); i++) {
			if(hostname.equals(devices.get(i).getHostname())) {
				return i;
			}
		}
		return -1;
	}

	private void clear() {
		executor.shutdownNow(); // full clean instead of refreshProcess.forEach(f -> f.cancel(true));
		executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
		refreshProcess.clear();
		devices.clear();
	}

	public void close() {
		LOG.trace("Model closing");
		removeListeners();
		executor.shutdownNow();
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

	private class MDNSListener implements ServiceListener {
		@Override
		public void serviceAdded(ServiceEvent event) {
			if(LOG.isTraceEnabled()) {
				LOG.trace("Service found: {}", event.getInfo().getName());
			}
		}

		@Override
		public void serviceRemoved(ServiceEvent event) {
			String hostname = event.getInfo().getName();
			synchronized(devices) {
				int ind;
				if((ind = indexOf(hostname)) >= 0) {
					devices.get(ind).setStatus(Status.OFF_LINE);
					fireEvent(EventType.UPDATE, ind);
				}
			}
			LOG.debug("Service removed: {} - {}", event.getInfo(), hostname);
		}

		@Override
		public void serviceResolved(ServiceEvent event) {
			ServiceInfo info = event.getInfo();
//			System.out.println(info.getPort());
			final String name = info.getName();
			if(name.startsWith("shelly") || name.startsWith("Shelly")) {
				executor.execute(() -> create(info.getInetAddresses()[0], 80, null, name));
			}
		}
	}
} // 197 - 307 - 326 - 418