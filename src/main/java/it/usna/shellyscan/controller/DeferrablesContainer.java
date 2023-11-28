package it.usna.shellyscan.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.DeferrableTask.Status;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.UsnaEventListener;
import it.usna.util.UsnaObservable;

public class DeferrablesContainer extends UsnaObservable<DeferrableTask.Status, Integer> implements UsnaEventListener<Devices.EventType, Integer> {
	private final static Logger LOG = LoggerFactory.getLogger(DeferrablesContainer.class);
	private static DeferrablesContainer instance;
	private final Devices model;
	private ArrayList<Integer> devIdx = new ArrayList<>();
	private ArrayList<DeferrableRecord> defer = new ArrayList<>();

	public static DeferrablesContainer getInstance(Devices model) {
		if(instance == null) {
			instance = new DeferrablesContainer(model);
			model.addListener(instance);
		}
		return instance; 
	}

	private DeferrablesContainer(Devices model) {
		this.model = model;
	};

	public void add(int modelIdx, DeferrableTask def) {
		synchronized (devIdx) {
			DeferrableRecord newDef = new DeferrableRecord(def, UtilMiscellaneous.getDescName(model.get(modelIdx)), LocalDateTime.now());
			devIdx.add(modelIdx);
			defer.add(newDef);
			fireEvent(Status.WAITING, devIdx.size() - 1);
			LOG.trace("Deferrable added: {}", newDef);
		}
	}

	public void cancel(int index) {
		synchronized (devIdx) {
			DeferrableTask def = defer.get(index).def;
			if(def.getStatus() == Status.WAITING) {
				devIdx.set(index, null);
				def.cancel();
				fireEvent(Status.CANCELLED, index);
			}
		}
	}

	@Override
	public void update(EventType mesgType, Integer modelIdx) {
		int index;
		if((mesgType == EventType.SUBSTITUTE || mesgType == EventType.UPDATE) && (index = devIdx.indexOf(modelIdx)) >= 0) {
			synchronized (devIdx) {
				DeferrableTask deferrable = defer.get(index).def;
				ShellyAbstractDevice device;
				if(deferrable.getStatus() == Status.WAITING && (device = model.get(modelIdx)).getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
					deferrable.setStatus(Status.RUNNING); // deferrable.run(device) change the status but we need it is changed before fireEvent
					new Thread(() -> {
						final Status s = deferrable.run(device);
						fireEvent(s, index);
					}).start();
					String name = UtilMiscellaneous.getDescName(model.get(modelIdx)); // could have been changed since add(...)
					fireEvent(Status.RUNNING, index);
					LOG.trace("Deferrable execution: {}", deferrable);
					defer.get(index).deviceName = name;
					devIdx.set(index, null);
				}
			}
		} else if(mesgType == Devices.EventType.CLEAR) {
			synchronized (devIdx) {
				for(int i = 0; i < devIdx.size(); i++) {
					cancel(i);
				}
			}
		}
	}
	
	public int size() {
		return defer.size();
	}
	
	public DeferrableRecord get(int index) {
		return defer.get(index);
	}

	@Override
	public String toString() {
		return "List of deferrables\n" + defer.stream().map(DeferrableRecord::toString).collect(Collectors.joining("\n"));
	}

	public static class DeferrableRecord { // not a record, deviceName is mutable
		private final DeferrableTask def;
		private final LocalDateTime time;
		private String deviceName;

		private DeferrableRecord(DeferrableTask def, String deviceName, LocalDateTime time) {
			this.def = def;
			this.deviceName = deviceName;
			this.time = time;
		}
		
		public LocalDateTime getTime() {
			return time;
		}

		public String getDescription() {
			return def.getDescription();
		}

		public Status getStatus() {
			return def.getStatus();
		}
		
		public String getRetMsg() {
			return def.getreturn();
		}

		public String getDeviceName() {
			return deviceName;
		}

		@Override
		public String toString() {
			return time + " - " +  def + " - " + deviceName;
		}
	};
}