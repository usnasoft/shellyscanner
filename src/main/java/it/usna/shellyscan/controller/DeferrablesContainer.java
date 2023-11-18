package it.usna.shellyscan.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.DeferrableAction.Status;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.UsnaEventListener;
import it.usna.util.UsnaObservable;

public class DeferrablesContainer extends UsnaObservable<DeferrableAction.Status, Integer> implements UsnaEventListener<Devices.EventType, Integer> {
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

	public void add(int modelIdx, DeferrableAction def) {
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
			DeferrableAction def = defer.get(index).def;
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
				DeferrableAction deferrable = defer.get(index).def;
				ShellyAbstractDevice device;
				if(deferrable.getStatus() == Status.WAITING && (device = model.get(modelIdx)).getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
					new Thread(() -> {
						Status s = deferrable.run(device);
						fireEvent(s, index);
					}).start();
					fireEvent(Status.RUNNING, index);
					String name = UtilMiscellaneous.getDescName(model.get(modelIdx)); // could have been changed since add(...)
					LOG.trace("Deferrable execution: {}", deferrable);
					defer.get(index).deviceName = name;
					devIdx.set(index, null);
				}
			}
			LOG.info(this.toString()); // todo remove
		} else if(mesgType == Devices.EventType.CLEAR) {
			synchronized (devIdx) {
				for(int i = 0; i < devIdx.size(); i++) {
					cancel(i);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "List of deferrables\n" + defer.stream().map(d -> d.toString()).collect(Collectors.joining("\n"));
	}

	private static class DeferrableRecord { // not a record, is mutable
		private final DeferrableAction def;
		private final LocalDateTime time;
		private String deviceName;

		private DeferrableRecord(DeferrableAction def, String deviceName, LocalDateTime time) {
			this.def = def;
			this.deviceName = deviceName;
			this.time = time;
		}

		@Override
		public String toString() {
			return time + " - " +  def + " - " + deviceName;
		}
	};
}