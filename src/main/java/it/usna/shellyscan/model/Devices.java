package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Path;
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

import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.GhostDevice;
import it.usna.shellyscan.model.device.InetAddressAndPort;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.model.device.ShellyUnmanagedDeviceInterface;
import it.usna.shellyscan.model.device.blu.AbstractBluDevice;
import it.usna.shellyscan.model.device.blu.BTHomeDevice;
import it.usna.shellyscan.model.device.blu.BluInetAddressAndPort;
import it.usna.shellyscan.model.device.blu.BluTRV;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.AbstractProDevice;
import it.usna.shellyscan.model.device.g3.AbstractG3Device;

public class Devices extends it.usna.util.UsnaObservable<Devices.EventType, Integer> {
	private final static Logger LOG = LoggerFactory.getLogger(Devices.class);
	private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
	public final static String SCANNER_AGENT = "Shelly Scanner";
	
	public enum EventType {
		ADD,
		UPDATE,
		SUBSTITUTE,
		DELETE, // Ghost
		READY, // Model is ready
		CLEAR // Clear model
	};

	private final static int EXECUTOR_POOL_SIZE = 128;
	public final static long MULTI_QUERY_DELAY = 59;

	private JmmDNS jd;
	private Set<JmDNS> bjServices = new HashSet<>();

	private IPCollection ipCollection = null;

	private final static String SERVICE_TYPE1 = "_http._tcp.local.";
//	private final static String SERVICE_TYPE2 = "_shelly._tcp.local.";
	private final List<ShellyAbstractDevice> devices = new ArrayList<>();
	private final List<ScheduledFuture<?>> refreshProcess = new ArrayList<>();
	private int refreshInterval = 2000;
	private int refreshTics = 3; // full refresh every STATUS_TICS refresh

	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
	private HttpClient httpClient = new HttpClient();
	private WebSocketClient wsClient = new WebSocketClient(httpClient);
	
	private DevicesStore ghostsStore = new DevicesStore();
	
	public Devices() throws Exception {
		httpClient.setDestinationIdleTimeout(300_000); // 5 min
		httpClient.setMaxConnectionsPerDestination(8);
		httpClient.start();
		
//		wsClient.setConnectTimeout(100_000);
//		wsClient.setIdleTimeout(Duration.ofMinutes(100));
		wsClient.setStopAtShutdown(true);
//		wsClient.setInputBufferSize(100_000);
		wsClient.start();
	}

	public void scannerInit(boolean fullScan, int refreshInterval, int refreshTics, boolean autorelod) throws IOException {
		this.refreshInterval = refreshInterval;
		this.refreshTics = refreshTics;
		final MDNSListener dnsListener = new MDNSListener();
		if(fullScan) {
			jd = JmmDNS.Factory.getInstance();
			for(JmDNS dns: jd.getDNS()) {
				bjServices.add(dns);
				LOG.debug("Full scan {} {}", dns.getName(), dns.getInetAddress());
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
			if(LOG.isTraceEnabled()) {
				LOG.trace("Creating JmDNS on: {}; interface: {}", InetAddress.getLocalHost(), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getInterfaceAddresses());
			}
			final JmDNS dns = JmDNS.create(/*network == null ? InetAddress.getLocalHost() : network*/InetAddress.getLocalHost(), null);
			bjServices.add(dns);
			LOG.debug("Local scan: {} {}", dns.getName(), dns.getInetAddress());
			dns.addServiceListener(SERVICE_TYPE1, dnsListener);
//			dns.addServiceListener(SERVICE_TYPE2, dnsListener);
		}
		fireEvent(EventType.READY);
		
		if(autorelod) {
			executor.schedule(() -> ghostsReconnect(), 45, TimeUnit.SECONDS);
		}
	}
	
	public void scannerInit(final IPCollection ipCollection, int refreshInterval, int refreshTics) throws IOException {
		this.ipCollection = ipCollection;
		this.refreshInterval = refreshInterval;
		this.refreshTics = refreshTics;
		LOG.debug("IP scan: {}", ipCollection);
		scanByIP();
		fireEvent(EventType.READY);
	}
	
	public void setRefreshTime(int refreshInterval, int refreshTics) {
		this.refreshInterval = refreshInterval;
		this.refreshTics = refreshTics;
	}
	
	public void setIPInterval(IPCollection coll) {
		this.ipCollection = coll;
	}
	
	private void scanByIP() throws IOException {
		int dalay = 0;
		for(InetAddress addr: ipCollection) {
			executor.schedule(() -> {
				try {
					if(addr.isReachable(10_000)) {
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
					LOG.error("IP scan error {} {}", addr, e.toString());
				}
			}, dalay, TimeUnit.MILLISECONDS);
			dalay += 4;
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
			LOG.trace("Not Shelly {} - {}", address, e);
			return null;
		}
	}


	public void rescan(boolean useStore) throws IOException {
		LOG.trace("rescan");
		List<GhostDevice> ghosts = null;
		if(useStore) {
			ghosts = ghostsStore.toGhosts(this);
		}
		clear();
		fireEvent(EventType.CLEAR);
		if(this.ipCollection == null) {
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
//						dns.addServiceListener(SERVICE_TYPE2, new MDNSListener());
					}

				} catch(Exception e) {
					LOG.debug("local rescan {}", e);
				}
			}
			for(JmDNS bonjourService: bjServices) {
				LOG.debug("scanning: {} {}", bonjourService.getName(), bonjourService.getInetAddress());
				final ServiceInfo[] serviceInfos = bonjourService.list(SERVICE_TYPE1);
//				final ServiceInfo[] serviceInfos = bonjourService.list(SERVICE_TYPE2);
				for (ServiceInfo dnsInfo: serviceInfos) {
					final String name = dnsInfo.getName();
					executor.execute(() -> create(dnsInfo.getInetAddresses()[0], 80, name, true));
				}
			}
			if(useStore) {
				loadGhosts(ghosts);
			}
		} else {
			scanByIP();
			if(useStore) {
				loadGhosts(ghosts);
			}
		}
		LOG.debug("end scan");
		fireEvent(EventType.READY);
	}

	public void refresh(final int ind, boolean force) {
		synchronized(devices) {
			final ShellyAbstractDevice d = devices.get(ind);
			if(d instanceof GhostDevice == false && (d.getStatus() != Status.READING || force)) {
				pauseRefresh(ind);
				d.setStatus(Status.READING);
				executor.schedule(() -> {
					if(d instanceof ShellyUnmanagedDeviceInterface unmanaged && unmanaged.getException() != null) { // try to create proper device
						InetAddressAndPort addr = d.getAddressAndPort();
						create(addr.getAddress(), addr.getPort(), d.getHostname(), true);
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
							activateRefresh(ind);
							updateViewRow(d, ind);
						}
					}
				}, MULTI_QUERY_DELAY, TimeUnit.MILLISECONDS);
			}
		}
	}

	public void reboot(int ind) {
		final ShellyAbstractDevice d = devices.get(ind);
		pauseRefresh(ind); // Before reboot disable refresh process
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
				activateRefresh(ind);
			}
			updateViewRow(d, ind);
		});
	}
	
	public void pauseRefresh(int ind) {
		synchronized(devices) {
			refreshProcess.get(ind).cancel(true);
		}
	}
	
	public void activateRefresh(int ind) {
		synchronized(devices) {
			if(refreshProcess.get(ind).isCancelled()) {
				refreshProcess.set(ind, scheduleRefresh(devices.get(ind), ind, refreshInterval, refreshTics));
			}
		}
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
	
	public void create(InetAddress address, int port, String hostName, boolean force) {
		LOG.trace("getting info (/shelly) {}:{} - {}", address, port, hostName);
		try {
			JsonNode info = isShelly(address, port);
			if(info != null) {
				create(address, port, info, hostName);
			} else if(force && (hostName.startsWith("shelly") || hostName.startsWith("Shelly"))) { // ShellyBulbDuo-xxx, ShellyWallDisplay-xxx, ...
				LOG.warn("create with error (info==null) {}:{}", address, port);
				newDevice(DevicesFactory.createWithError(httpClient, address, port, hostName, new NullPointerException()));
			}
//			Thread.sleep(Devices.MULTI_QUERY_DELAY);
		} catch(TimeoutException e) { // SocketTimeoutException extends IOException
			if(force && (hostName.startsWith("shelly") || hostName.startsWith("Shelly"))) {
				LOG.warn("create with error {}:{}", address, port, e);
				newDevice(DevicesFactory.createWithError(httpClient, address, port, hostName, e));
			}
		}
	}

	/**
	 * Create a device JsonNode info (/shelly) given
	 */
	private void create(InetAddress address, int port, JsonNode info, String hostName) {
		LOG.trace("Creating {}:{} - {}", address, port, hostName);
		try {
			ShellyAbstractDevice d = DevicesFactory.create(httpClient, wsClient, address, port, info, hostName);
			if(/*d != null &&*/ Thread.interrupted() == false) {
				newDevice(d);
				LOG.debug("Create {}:{} - {}", address, port, d);

				// Rage extender
				if(/*port == 80 &&*/ d instanceof AbstractG2Device gen2 && (gen2.isExtender() || gen2.getStatus() == Status.NOT_LOOGGED)) {
					gen2.getRangeExtenderManager().getPorts().forEach(p -> {
						try {
							executor.execute(() -> {
								try {
									JsonNode infoEx = isShelly(address, p);
									if(infoEx != null) {
										create(d.getAddressAndPort().getAddress(), p, infoEx, d.getHostname() + "-EX" + ":" + p); // device will later correct hostname
									}
								} catch (TimeoutException | RuntimeException e) {
									LOG.debug("timeout {}:{}", d.getAddressAndPort().getAddress(), p, e);
								}
							});
						} catch(RuntimeException e) {
							LOG.error("Unexpected-add-ext: {}; host: {}:{}", address, hostName, p, e);
						}
					});
				}
				// BTHome
//				if(d instanceof AbstractProDevice || d instanceof AbstractG3Device) {
//					final JsonNode currenteComponents = d.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components"); // empty on 401
//					HashSet<BluTRV> trvSet = new HashSet<>();
//					for(JsonNode compInfo: currenteComponents) {
//						String key = compInfo.path("key").asText();
////						String kPrefix = key.substring(0, key.indexOf(':'));
//						if(key.startsWith(AbstractBluDevice.DEVICE_KEY_PREFIX)) {
//							newBluDevice(d, compInfo, key);
//						} else if(key.startsWith(BluTRV.DEVICE_KEY_PREFIX)) { // temporary workaround
//							trvSet.add(new BluTRV((AbstractG2Device)d, compInfo, "-1"));
//						}
//					}
//					trvSet.forEach(trv -> { // temporary workaround
//						int ind = devices.indexOf(trv);
//						if(ind >= 0 && devices.get(ind) instanceof BTHomeDevice blu) {
//							blu.setTypeName("Blu TRV");
//						}
//					});
//				}
				if(d instanceof AbstractProDevice || d instanceof AbstractG3Device) {
					final JsonNode currenteComponents = d.getJSON("/rpc/Shelly.GetComponents?dynamic_only=true").path("components"); // empty on 401
					for(JsonNode compInfo: currenteComponents) {
						String key = compInfo.path("key").asText();
						//					String kPrefix = key.substring(0, key.indexOf(':'));
						if(key.startsWith(AbstractBluDevice.DEVICE_KEY_PREFIX) /*|| key.startsWith(BluTRV.DEVICE_KEY_PREFIX)*/) {
							newBluDevice(d, compInfo, key);
						}
						if( key.startsWith(BluTRV.DEVICE_KEY_PREFIX)) {
							newBluDevice(d, compInfo, key);
						}
					}
				}
			}
		} catch(Exception e) {
			LOG.error("Unexpected-add: {}:{}; host: {}", address, port, hostName, e);
		}
	}
	
	// Add or update (existence tested by mac address) a device
	private void newDevice(ShellyAbstractDevice d) {
		synchronized(devices) {
			int ind = devices.indexOf(d);
			if(ind >= 0) {
				ShellyAbstractDevice existingDevice = devices.get(ind);
				if(d instanceof ShellyUnmanagedDeviceInterface == false || existingDevice instanceof ShellyUnmanagedDeviceInterface || existingDevice instanceof GhostDevice) { // Do not replace device if was recognized and now is not
//					if(d.getTypeID() != existingDevice.getTypeID() || d instanceof BatteryDeviceInterface == false) { // who cares: full refresh
						if(refreshProcess.get(ind) != null) {
							refreshProcess.get(ind).cancel(true);
						}
						devices.set(ind, d);
						fireEvent(EventType.SUBSTITUTE, ind);
						refreshProcess.set(ind, scheduleRefresh(d, ind, refreshInterval, refreshTics));
//					} // else -> don't loose stored informations on BatteryDeviceInterface implementation
				}
			} else {
				final int idx = devices.size();
				devices.add(d);
				fireEvent(EventType.ADD, idx);
				refreshProcess.add(scheduleRefresh(d, idx, refreshInterval, refreshTics));
			}
		}
	}
	
	private void newBluDevice(ShellyAbstractDevice parent, JsonNode compInfo, String key) {
		try {
			AbstractBluDevice newBlu = DevicesFactory.createBlu((AbstractG2Device)parent, httpClient, /*wsClient,*/ compInfo, key);
			synchronized(devices) {
				int ind = devices.indexOf(newBlu);
				if(ind >= 0) { // already in list
					ShellyAbstractDevice oldBlu = devices.get(ind);
					if(oldBlu instanceof GhostDevice ||
							(oldBlu instanceof BTHomeDevice && newBlu instanceof BTHomeDevice == false) || // BTHome -> blu trv
							((newBlu.getLastTime() > oldBlu.getLastTime() || oldBlu.getAddressAndPort().equals(newBlu.getAddressAndPort())) && (oldBlu instanceof BTHomeDevice == false && newBlu instanceof BTHomeDevice) == false)) {
						if(refreshProcess.get(ind) != null) {
							refreshProcess.get(ind).cancel(true);
						}
						devices.set(ind, newBlu);
						if(oldBlu instanceof AbstractBluDevice old) { // could be a ghost
							((BluInetAddressAndPort)newBlu.getAddressAndPort()).addAlternativeParent(old);
						}
						fireEvent(EventType.SUBSTITUTE, ind);
						refreshProcess.set(ind, scheduleRefresh(newBlu, ind, refreshInterval, refreshTics));
					} else {
						((BluInetAddressAndPort)oldBlu.getAddressAndPort()).addAlternativeParent(parent.getAddressAndPort());
					}
				} else {
					final int idx = devices.size();
					devices.add(newBlu);
					fireEvent(EventType.ADD, idx);
					refreshProcess.add(scheduleRefresh(newBlu, idx, refreshInterval, refreshTics));
				}
			}
		} catch (RuntimeException e) {
			LOG.error("newBluDevice-parent: {} - key: {}", parent.getAddressAndPort(), compInfo.path("key").asText(), e);
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
					d.setStatus(Status.ERROR);
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

	private void ghostsReconnect() {
		synchronized(devices) {
			LOG.debug("Starting ghosts reconnect");
			int dalay = 0;
			for(int i = 0; i < devices.size(); i++) {
				if(devices.get(i) instanceof GhostDevice g && g.isBatteryOperated() == false && g.getGeneration().equals(AbstractBluDevice.GENERATION) == false /*&& g.getAddressAndPort().getPort() == 80*/) { // getPort() port is (currently) variable
					executor.schedule(() -> {
						try {
							create(g.getAddressAndPort().getAddress(), g.getAddressAndPort().getPort(), g.getAddressAndPort().getAddress().getHostAddress(), false);
						} catch (RuntimeException e) {/*LOG.trace("ghosts reload {}", d.getAddress());*/}
					}, dalay, TimeUnit.MILLISECONDS);
					dalay += 4;
				}
			}
		}
	}
	
	public int getIndex (ShellyAbstractDevice d) {
		return devices.indexOf(d);
	}

	public ShellyAbstractDevice get(int ind) {
		return devices.get(ind);
	}
	
	public void remove(int ind) {
		synchronized(devices) {
			final ScheduledFuture<?> f = refreshProcess.remove(ind);
			if(f != null) {
				f.cancel(true);
			}
			devices.remove(ind);
			fireEvent(EventType.DELETE, ind);
		}
	}

	public int size() {
		return devices.size();
	}
	
	public void loadFromStore(Path path) throws IOException {
		loadGhosts(ghostsStore.read(path));
	}
	
	private void loadGhosts(List<GhostDevice> ghosts) {
		synchronized(devices) {
			ghosts.forEach(d -> {
				if(devices.indexOf(d) < 0) {
					final int idx = devices.size();
					devices.add(d);
					fireEvent(EventType.ADD, idx);
					refreshProcess.add(null);
				}
			});
		}
	}
	
	public GhostDevice getGhost(ShellyAbstractDevice dev, int modelIdx) {
		return ghostsStore.getGhost(dev, modelIdx);
	}
	
	public GhostDevice getGhost(int modelIdx) {
		return ghostsStore.getGhost(devices.get(modelIdx), modelIdx);
	}
	
	public void saveToStore(Path path) throws IOException {
		ghostsStore.store(this, path);
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
		try {
			wsClient.stop();
			wsClient.destroy();
		} catch (Exception e) {
			LOG.error("webSocketClient.stop", e);
		}
		try {
			httpClient.stop();
			httpClient.destroy();
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
			if(LOG.isDebugEnabled()) {
				LOG.debug("Service removed: {} - {}", event.getInfo(), event.getInfo().getName());
			}
		}

		@Override
		public void serviceResolved(ServiceEvent event) {
			ServiceInfo info = event.getInfo();
			executor.execute(() -> create(info.getInetAddresses()[0], info.getPort(), info.getName(), true));
		}
	}
} // 197 - 307 - 326 - 418 - 510 - 544 - 574 - 629