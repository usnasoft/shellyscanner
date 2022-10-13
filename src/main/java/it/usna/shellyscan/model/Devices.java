package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.util.UsnaObservable;

public class Devices extends UsnaObservable<Devices.EventType, Integer> {
	private final static Logger LOG = LoggerFactory.getLogger(Devices.class);
	public enum EventType {
		ADD,
		UPDATE,
		REMOVE,
		READY, // Model is ready
		CLEAR
	};

	private final static int EXECUTOR_POOL_SIZE = 128;
	public final static long MULTI_QUERY_DELAY = 50;

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
						Thread.sleep(MULTI_QUERY_DELAY);
						if(isShelly(addr)) {
							create(addr, addr.getHostAddress());
						}
					} else {
						LOG.trace("no ping {}", addr);
					}
				} catch (SocketTimeoutException e) {
					LOG.trace("timeout {}", addr);
				} catch (IOException | InterruptedException e) {
					LOG.error("ip scan error {} {}", addr, e.toString());
				}
			}, dalay, TimeUnit.MILLISECONDS);
		}
	}

	private static boolean isShelly(final InetAddress address) {
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection)new URL("http://" + address.getHostAddress() + "/shelly").openConnection();
			con.setConnectTimeout(15000);
			JsonNode shellyNode = new ObjectMapper().readTree(con.getInputStream());
			int resp = con.getResponseCode();
			if(resp == HttpURLConnection.HTTP_OK && shellyNode.has("mac")) { // "mac" is common to all shelly devices
				return true;
			} else {
				LOG.trace("Not Shelly {}, resp {}, node ()", address, resp, shellyNode);
				return false;
			}
//			return con.getResponseCode() == HttpURLConnection.HTTP_OK && shellyNode.has("mac");
		} catch (/*IO*/Exception e) {
			LOG.trace("Not Shelly {} - {}", address, e.toString());
			return false;
		} finally {
			con.disconnect();
		}
	}

	public void scan() throws IOException {
		//LOG.trace("Q {}", ((ScheduledThreadPoolExecutor)executor).getQueue().size());
		//		synchronized(devices) {
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
						//							create(info.getInetAddresses()[0], name);
						executor.execute(() -> create(info.getInetAddresses()[0], name));
					}
				}
			}
		} else {
			scanByIP();
		}
		LOG.debug("end scan");
		fireEvent(EventType.READY);
		//		}
	}

	public void refresh(int ind, boolean force) {
		synchronized(devices) {
			final ShellyAbstractDevice d = devices.get(ind);
			if(d.getStatus() != Status.READING || force) {
				refreshProcess.get(ind).cancel(true);
				d.setStatus(Status.READING);
				executor.schedule(() -> {
					try {
						d.refreshSettings();
						Thread.sleep(MULTI_QUERY_DELAY);
						d.refreshStatus();
					} catch (Exception e) {
						if(d.getStatus() == Status.ERROR) {
							LOG.error("Unexpected on refresh", e);
						} else {
							LOG.debug("refresh {} - {}", d/*.toString()*/, d.getStatus());
						}
					} finally {
						synchronized(devices) {
							if(refreshProcess.get(ind).isCancelled()) { // in case of many and fast "refresh"
								refreshProcess.set(ind, schedureRefresh(d, ind, refreshInterval, refreshTics));
							}
						}
						updateViewRow(d, ind);
					}
				}, MULTI_QUERY_DELAY, TimeUnit.MILLISECONDS);
			}
		}
	}

	public void pauseRefresh(int ind) {
		refreshProcess.get(ind).cancel(true);
	}

	public void reboot(int ind) {
		final ShellyAbstractDevice d = devices.get(ind);
		final ScheduledFuture<?> f = refreshProcess.get(ind);
		f.cancel(true);
		//if(d.getStatus() != Status.OFF_LINE) {
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
						refreshProcess.set(ind, schedureRefresh(d, ind, refreshInterval, refreshTics));
					}
				}
			}
			updateViewRow(d, ind);
		});
		//}
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

	public void create(InetAddress address, String hostName) {
		LOG.trace("Creating {} - {}", address, hostName);
		try {
			ShellyAbstractDevice d = DevicesFactory.create(address, hostName);
			if(Thread.interrupted() == false) {
				synchronized(devices) {
					int ind;
					if((ind = devices.indexOf(d)) >= 0) {
						refreshProcess.get(ind).cancel(true);
						devices.set(ind, d);
						fireEvent(EventType.UPDATE, ind);
						refreshProcess.set(ind, schedureRefresh(d, ind, refreshInterval, refreshTics));
					} else {
						final int idx = devices.size();
						devices.add(d);
						fireEvent(EventType.ADD, idx);
						refreshProcess.add(schedureRefresh(d, idx, refreshInterval, refreshTics));
					}
				}
				LOG.debug("Create {} - {}", address, d);
			}
		} catch(Exception e) {
			LOG.error("Unexpected-add: {}; host: {}", address, hostName, e);
		}
	}

	private ScheduledFuture<?> schedureRefresh(ShellyAbstractDevice d, int idx, final int interval, final int statusTics) {
		final Runnable refreshRunner = new Runnable() {
			private final Integer megIdx = Integer.valueOf(idx);
			private int ticCount = 0;

			@Override
			public void run() {
				try {
					if(++ticCount >= statusTics) {
						d.refreshSettings();
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
			final String name = info.getName();
			if(name.startsWith("shelly") || name.startsWith("Shelly")) {
				executor.execute(() -> create(info.getInetAddresses()[0], name));
			}
		}
	}
} // 197 - 307 - 326 - 418