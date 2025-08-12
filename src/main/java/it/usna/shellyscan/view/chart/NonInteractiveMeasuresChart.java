package it.usna.shellyscan.view.chart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;
import it.usna.util.UsnaEventListener;  

public class NonInteractiveMeasuresChart implements UsnaEventListener<Devices.EventType, Integer> {
	private static final Logger LOG = LoggerFactory.getLogger(NonInteractiveMeasuresChart.class);
	private ChartType currentType;
	private final boolean fahrenheit;
	private final Devices model;

	public NonInteractiveMeasuresChart(final Devices model, ChartType currentType, AppProperties appProp) {
		this.model = model;
		this.currentType = currentType;
		this.fahrenheit = appProp.getProperty(ScannerProperties.PROP_TEMP_UNIT).equals("F");
		model.addListener(this);
	}

	@Override
	public void update(EventType mesgType, Integer ind) {
		if(mesgType == Devices.EventType.UPDATE) {
			final ShellyAbstractDevice d;
			if(((d = model.get(ind)).getStatus() == Status.ON_LINE)) {
				try {
					Meters[] m;
					if(currentType == ChartType.INT_TEMP && d instanceof InternalTmpHolder tempH) {
						float val = tempH.getInternalTmp();
						if(fahrenheit) val = val*1.8f + 32f;
						outStream(d, 0, currentType.name(), d.getLastTime(), val);
					} else if(currentType == ChartType.RSSI) {
						outStream(d, 0, currentType.name(), d.getLastTime(), d.getRssi());
					} else if(currentType == ChartType.T_ALL && (m = d.getMeters()) != null) {
						for(int i = 0; i < m.length; i++) {
							if(m[i].hasType(Meters.Type.T)) {
								float val = m[i].getValue(Meters.Type.T);
								if(fahrenheit) val = val*1.8f + 32f;
								outStream(d, 0, /*ChartType.T*/"T", d.getLastTime(), val);
							}
							if(m[i].hasType(Meters.Type.T1)) {
								float val = m[i].getValue(Meters.Type.T1);
								if(fahrenheit) val = val*1.8f + 32f;
								outStream(d, 0, /*ChartType.T1*/"T1", d.getLastTime(), val);
							}
							if(m[i].hasType(Meters.Type.T2)) {
								float val = m[i].getValue(Meters.Type.T2);
								if(fahrenheit) val = val*1.8f + 32f;
								outStream(d, 0, /*ChartType.T2*/"T2", d.getLastTime(), val);
							}
							if(m[i].hasType(Meters.Type.T3)) {
								float val = m[i].getValue(Meters.Type.T3);
								if(fahrenheit) val = val*1.8f + 32f;
								outStream(d, 0, /*ChartType.T3*/"T3", d.getLastTime(), val);
							}
							if(m[i].hasType(Meters.Type.T4)) {
								float val = m[i].getValue(Meters.Type.T4);
								if(fahrenheit) val = val*1.8f + 32f;
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
			}
		}
	}

	private static void outStream(ShellyAbstractDevice d, int channel, String type, long time, float val) {
		System.out.println("graph_data->" + d.getHostname() + ":" + channel + ":" + type + ":" + time + ":" + val);
	}
}