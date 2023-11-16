package it.usna.shellyscan.controller;

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

public class DeferrablesContainer implements UsnaEventListener<Devices.EventType, Integer> {
	private final static Logger LOG = LoggerFactory.getLogger(DeferrablesContainer.class);
	private static DeferrablesContainer instance;
	private final Devices model;
	private ArrayList<Integer> devIdx = new ArrayList<>();
	private ArrayList<DeferrableAction> deferrables = new ArrayList<>();
	private ArrayList<String> deviceDesc = new ArrayList<>();

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
			devIdx.add(modelIdx);
			deferrables.add(def);
			String name = UtilMiscellaneous.getDescName(model.get(modelIdx));
			deviceDesc.add(name);
			LOG.trace("Deferrable added: {} - {}", def, name);
		}
	}
	
	public void cancel(int index) {
		synchronized (devIdx) {
			DeferrableAction def = deferrables.get(index);
			if(def.getStatus() == Status.WAITING) {
//				deviceDesc.set(index, UtilMiscellaneous.getDescName(model.get(devIdx.get(index))));
				devIdx.set(index, null);
				def.cancel();
			}
		}
	}

	@Override
	public void update(EventType mesgType, Integer modelIdx) {
		int index;
		ShellyAbstractDevice device;
		DeferrableAction deferrable;
		if((mesgType == EventType.SUBSTITUTE || mesgType == EventType.UPDATE) &&
				(index = devIdx.indexOf(modelIdx)) >= 0 && (deferrable = deferrables.get(index)).getStatus() == Status.WAITING &&
				(device = model.get(modelIdx)).getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
			
			LOG.info(this.toString()); // todo remove
			synchronized (devIdx) {
				new Thread(() -> deferrable.run(device)).run();
				String name = UtilMiscellaneous.getDescName(model.get(modelIdx)); // could have been changed since add(...)
				LOG.trace("Deferrable execution: {} - {}", name, device);
				deviceDesc.set(index, name);
				devIdx.set(index, null);
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
		return "List of deferrables\n" + deferrables.stream().map(d -> d.toString()).collect(Collectors.joining("\n"));
	}
}