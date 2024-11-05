package it.usna.shellyscan.view.chart;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.util.UsnaEventListener;  

public class NonInteractiveMeasuresChart implements UsnaEventListener<Devices.EventType, Integer> {
	private final static Logger LOG = LoggerFactory.getLogger(NonInteractiveMeasuresChart.class);
	private ChartType currentType;
	private final Devices model;
	
	public NonInteractiveMeasuresChart(final Devices model, ChartType currentType/*, int[] ind*/) {
		this.model = model;
		this.currentType = currentType;
	}

	@Override
	public void update(EventType mesgType, Integer ind) {
		if(mesgType == Devices.EventType.UPDATE) {
			final ShellyAbstractDevice d;
			if(((d = model.get(ind)).getStatus() == Status.ON_LINE)) {
				SwingUtilities.invokeLater(() -> {
					try {
						Meters[] m;
						if(currentType == ChartType.INT_TEMP && d instanceof InternalTmpHolder tempH) {
							outStream(d, 0, currentType.name(), d.getLastTime(), tempH.getInternalTmp());
						} else if(currentType == ChartType.RSSI) {
							outStream(d, 0, currentType.name(), d.getLastTime(), d.getRssi());
						} else if(currentType == ChartType.T_ALL && (m = d.getMeters()) != null) {
							for(int i = 0; i < m.length; i++) {
								if(m[i].hasType(Meters.Type.T)) {
									float val = m[i].getValue(Meters.Type.T);
									outStream(d, 0, /*ChartType.T*/"T", d.getLastTime(), val);
								}
								if(m[i].hasType(Meters.Type.T1)) {
									float val = m[i].getValue(Meters.Type.T1);
									outStream(d, 0, /*ChartType.T1*/"T1", d.getLastTime(), val);
								}
								if(m[i].hasType(Meters.Type.T2)) {
									float val = m[i].getValue(Meters.Type.T2);
									outStream(d, 0, /*ChartType.T2*/"T2", d.getLastTime(), val);
								}
								if(m[i].hasType(Meters.Type.T3)) {
									float val = m[i].getValue(Meters.Type.T3);
									outStream(d, 0, /*ChartType.T3*/"T3", d.getLastTime(), val);
								}
								if(m[i].hasType(Meters.Type.T4)) {
									float val = m[i].getValue(Meters.Type.T4);
									outStream(d, 0, /*ChartType.T4*/"T4", d.getLastTime(), val);
								}
							}
						} else if(currentType == ChartType.P_SUM && (m = d.getMeters()) != null) {
							boolean exists = false;
							float sumW = 0;
							for(int i = 0; i < m.length; i++) {
								if(m[i].hasType(Meters.Type.W)) {
									sumW += m[i].getValue(Meters.Type.W);
									exists = true;
								}
							}
							if(exists) {
								outStream(d, 0, currentType.name(), d.getLastTime(), sumW);
							}
						} else if((m = d.getMeters()) != null) {
							int j = 0;
							for(int i = 0; i < m.length; i++) {
								if(m[i].hasType(currentType.mType) /*&& j < ts.length*/) {
									float val = m[i].getValue(currentType.mType);
									outStream(d, j, currentType.name(), d.getLastTime(), val);
									j++;
								}
							}
						}
					} catch (Throwable ex) {
						LOG.warn("Unexpected {}-{}", d, currentType.name(), ex); // possible error on graph type change
					}
				});
			}
		}
	}

	private static void outStream(ShellyAbstractDevice d, int channel, String type, long time, float val) {
		System.out.println("graph_data->" + d.getHostname() + ":" + channel + ":" + type + ":" + time + ":" + val);
	}
}