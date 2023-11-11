package it.usna.shellyscan.controller;

import java.util.ArrayList;

import it.usna.shellyscan.controller.DeferrableAction.Status;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.util.UsnaEventListener;

//TODO synchronize

public class DeferrablesContainer implements UsnaEventListener<Devices.EventType, Integer> {
	private static DeferrablesContainer instance;
	private final Devices model;
	private ArrayList<Integer> devIdx = new ArrayList<>();
	private ArrayList<DeferrableAction> deferrables = new ArrayList<>();

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

	public void addDeferrable(int idx, DeferrableAction def) {
		devIdx.add(idx);
		deferrables.add(def);
	}

	@Override
	public void update(EventType mesgType, Integer msgBody) {
		//		SwingUtilities.invokeLater(() -> {
		int index;
		ShellyAbstractDevice device;
		DeferrableAction deferrable;
		if((mesgType == EventType.SUBSTITUTE || mesgType == EventType.UPDATE) &&
				msgBody != null && (index = devIdx.indexOf(msgBody)) >= 0 && (deferrable = deferrables.get(index)).getStatus() == Status.WAITING &&
				(device = model.get(msgBody)).getStatus() == ShellyAbstractDevice.Status.ON_LINE) {
			new Thread(() -> deferrable.run(device));
		} else if(mesgType == Devices.EventType.CLEAR) {
			deferrables.forEach(def -> def.cancel());
		}
		//		});
	}
}
