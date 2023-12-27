package it.usna.shellyscan.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.controller.DeferrableTask.Status;
import it.usna.shellyscan.controller.DeferrableTask.Task;
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

//	public static DeferrablesContainer getInstance(Devices model) {
//		if(instance == null) {
//			instance = new DeferrablesContainer(model);
//			model.addListener(instance);
//		}
//		return instance; 
//	}

	public static DeferrablesContainer getInstance() {
		return instance; 
	}
	
	public static void init(Devices model) {
		instance = new DeferrablesContainer(model);
		model.addListener(instance);
	}

	private DeferrablesContainer(Devices model) {
		this.model = model;
	};

	public void add(int modelIdx, String decription,  Task task) {
		synchronized (devIdx) {
			DeferrableRecord newDef = new DeferrableRecord(decription, task, UtilMiscellaneous.getDescName(model.get(modelIdx)), LocalDateTime.now());
			devIdx.add(modelIdx);
			defer.add(newDef);
			fireEvent(Status.WAITING, devIdx.size() - 1);
			LOG.trace("Deferrable added: {}", newDef);
		}
	}
	
//	public List<DeferrableTask> getWaitingDefByModelIndex(Integer modelIdx) {
//		ArrayList<DeferrableTask> res = new ArrayList<>();
//		synchronized (devIdx) {
//			for(int i = 0; i < devIdx.size(); i++) {
//				if(devIdx.get(i).equals(modelIdx)) {
//					res.add(defer.get(i).def)	;
//				}
//			}
//		}
//		return res;
//	}
	
	public int indexOf(Integer modelIdx, String description) {
		synchronized (devIdx) {
			for(int i = 0; i < devIdx.size(); i++) {
				if(modelIdx.equals(devIdx.get(i)) && defer.get(i).getDescription().equals(description)) {
					return i;
				}
			}
		}
		return -1;
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
		ShellyAbstractDevice device;
		if((mesgType == EventType.SUBSTITUTE || mesgType == EventType.UPDATE) &&
				(index = devIdx.indexOf(modelIdx)) >= 0 &&
				(device = model.get(modelIdx)).getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
			synchronized (devIdx) {
				DeferrableTask deferrable = defer.get(index).def;
				if(deferrable.getStatus() == Status.WAITING) {
					deferrable.setStatus(Status.RUNNING); // deferrable.run(device) change the status but we need it is changed before fireEvent
					new Thread(() -> {
						final Status s = deferrable.run(device);
						fireEvent(s, index);
					}).start();
					fireEvent(Status.RUNNING, index);
					LOG.trace("Deferrable execution: {}", deferrable);
					defer.get(index).deviceName = UtilMiscellaneous.getDescName(device); // could have been changed since add(...)
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
	
	public int count() {
		return defer.size();
	}
	
	public int countWaiting() {
		synchronized (devIdx) {
			return (int)defer.stream().filter(def -> def.getStatus() == Status.WAITING).count();
		}
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

		private DeferrableRecord(String decription, Task task, String deviceName, LocalDateTime time) {
			this.def = new DeferrableTask(decription, task);
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
			return def.getReturn();
		}

		public String getDeviceName() {
			return deviceName;
		}

		@Override
		public String toString() {
			return time + " - " +  def + " - " + deviceName;
		}
	}
}