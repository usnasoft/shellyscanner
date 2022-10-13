package it.usna.shellyscan.model;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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

import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public class Devices extends Observable {
	private Set<JmDNS> bjServices = new HashSet<>();
	private final static String SERVICE_TYPE1 = "_http._tcp.local.";
//	private final static String SERVICE_TYPE2 = "_shelly._tcp.local.";
	private final List<ShellyAbstractDevice> devices = new ArrayList<>();
	private final List<ScheduledFuture<?>> refreshProcess = new ArrayList<>();
	private int refreshInterval = 1000;
	private int refreshTics = 3; // full refresh every STATUS_TICS refresh
	
	private ScheduledExecutorService executor;
	
	private JmmDNS jd;
	
	private final static int EXECUTOR_POOL_SIZE = 30;
	private final static long MULTI_QUERY_DELAY = 50;
	private final static Logger LOG = LoggerFactory.getLogger(Devices.class);

	public void scannerInit(boolean fullScan, int refreshInterval, int refreshTics) throws IOException {
		executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
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
			final JmDNS dns = JmDNS.create(InetAddress.getLocalHost()/*, "lo"*/);
			bjServices.add(dns);
			LOG.debug("Local scan: {} {}", dns.getName(), dns.getInetAddress());
			dns.addServiceListener(SERVICE_TYPE1, dnsListener);
		}

		setChanged();
		notifyObservers(new ModelMessage(ModelMessage.Type.READY, null));
	}
	
	public void setRefreshTime(int refreshInterval, int refreshTics) {
		this.refreshInterval = refreshInterval;
		this.refreshTics = refreshTics;
	}

	public void scan() throws IOException {
		LOG.trace("Q {}", ((ScheduledThreadPoolExecutor)executor).getQueue().size());
		synchronized(devices) {
			clear();
			setChanged();
			notifyObservers(new ModelMessage(ModelMessage.Type.CLEAR, null));
			for(JmDNS bonjourService: bjServices) {
				LOG.info("scanning: {} {}", bonjourService.getName(), bonjourService.getInetAddress());
				final ServiceInfo[] serviceInfos = bonjourService.list(SERVICE_TYPE1);
				for (ServiceInfo info: serviceInfos) {
					final String name = info.getName();
					if(name.startsWith("shelly") || name.startsWith("Shelly")) { // ShellyBulbDuo-xxx
//						executor.execute(() -> create(info.getInetAddresses()[0], name));
						create(info.getInetAddresses()[0], name);
					}
				}
			}
			LOG.info("end scan");
			setChanged();
			notifyObservers(new ModelMessage(ModelMessage.Type.READY, null));
		}
	}

	public void refresh(int ind, boolean force) {
		synchronized(devices) {
			final ShellyAbstractDevice d = devices.get(ind);
			if(d.getStatus() != Status.READING || force) {
				refreshProcess.get(ind).cancel(true);
				d.setStatus(Status.READING);
				executor.execute(() -> {
					try {
						d.refreshSettings();
						Thread.sleep(MULTI_QUERY_DELAY);
						d.refreshStatus();
					} catch (Exception e) {
						if(d.getStatus() == Status.ERROR) {
							LOG.error("Unexpected on refresh", e);
						} else {
							LOG.debug("refresh {} - {}", d.toString(), d.getStatus());
						}
					} finally {
						synchronized(devices) {
							if(refreshProcess.get(ind).isCancelled()) { // in case of many and fast "refresh"
								// System.out.println("c1");
								refreshProcess.set(ind, schedureRefresh(d, ind, refreshInterval, refreshTics));
							} // else System.out.println("c2");
						}
						updateViewRow(d, ind);
					}
				});
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
//		if(d.getStatus() != Status.OFF_LINE) {
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
//		}
	}

	private void updateViewRow(final ShellyAbstractDevice d, int ind) {
		if(Thread.interrupted() == false) {
			synchronized(devices) {
				if(devices.size() > ind && d == devices.get(ind)) { // underlying model unchanged (on rescan)
					setChanged();
					notifyObservers(new ModelMessage(ModelMessage.Type.UPDATE, ind));
				}
			}
		}
	}

	public void create(InetAddress address, String hostName) {
		try {
			executor.execute(() -> {
				LOG.trace("Creating {} - {}", address, hostName);
				ShellyAbstractDevice d = DevicesFactory.create(address, hostName/*, Devices.this*/);
				if(Thread.interrupted() == false) {
					synchronized(devices) {
						setChanged();
						int ind;
						if((ind = devices.indexOf(d)) >= 0) {
							refreshProcess.get(ind).cancel(true);
							devices.set(ind, d);
							notifyObservers(new ModelMessage(ModelMessage.Type.UPDATE, ind));
							refreshProcess.set(ind, schedureRefresh(d, ind, refreshInterval, refreshTics));
						} else {
							final int idx = devices.size();
							devices.add(d);
							notifyObservers(new ModelMessage(ModelMessage.Type.ADD, idx));
							refreshProcess.add(schedureRefresh(d, idx, refreshInterval, refreshTics));
						}
					}
					LOG.debug("Create {} - {}", address, d);
				}
			});
		} catch(Exception e) {
			LOG.error("Unexpected-add: {}; host: {}", address, hostName, e);
		}
	}
	
	private ScheduledFuture<?> schedureRefresh(ShellyAbstractDevice d, int idx, final int interval, final int statisTics) {
		final Runnable refreshRunner = new Runnable() {
			private final ModelMessage msg = new ModelMessage(ModelMessage.Type.UPDATE, idx);
			private int ticCount = 0;
			
			@Override
			public void run() {
				try {
					if(++ticCount >= statisTics) {
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
						setChanged();
						notifyObservers(msg);
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
//		synchronized(devices) {
			for(int i = 0; i < devices.size(); i++) {
				if(hostname.equals(devices.get(i).getHostname())) {
					return i;
				}
			}
			return -1;
//		}
	}
	
	private void clear() {
		executor.shutdownNow(); // full clean instead of refreshProcess.forEach(f -> f.cancel(true));
		executor = Executors.newScheduledThreadPool(EXECUTOR_POOL_SIZE);
		refreshProcess.clear();
		devices.clear();
	}

	public void close() {
		LOG.trace("Model closing");
		deleteObservers();
		executor.shutdownNow();
		for(JmDNS dns: bjServices) {
			try {
				dns.close();
			} catch (IOException e) {
				LOG.error("closing JmDNS", e);
			}
		};
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
			LOG.trace("Service found: {}", event.getInfo().getName());
		}

		@Override
		public void serviceRemoved(ServiceEvent event) {
			String hostname = event.getInfo().getName();
			synchronized(devices) {
				int ind;
				if((ind = indexOf(hostname)) >= 0) {
					devices.get(ind).setStatus(Status.OFF_LINE);
					setChanged();
					notifyObservers(new ModelMessage(ModelMessage.Type.UPDATE, ind));
				}
			}
			LOG.debug("Service removed: {} - {}", event.getInfo(), event.getInfo().getName());
		}

		@Override
		public void serviceResolved(ServiceEvent event) {
			ServiceInfo info = event.getInfo();
			final String name = info.getName();
			if(name.startsWith("shelly") || name.startsWith("Shelly")) {
				create(info.getInetAddresses()[0], name);
			}
		}
	}
} // 197 - 307 - 326