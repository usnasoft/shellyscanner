package it.usna.shellyscan.view.chart;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Toolkit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.util.UsnaEventListener;  

public class MeasuresChart extends JFrame implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private final Devices model;
//	private final List<Integer> indList;
	private final Map<Integer, TimeSeries> series = new HashMap<>();

	public MeasuresChart(JFrame owner, final Devices model, List<Integer> ind) {  
		super(/*owner,*/ LABELS.getString("dlgChartsTitle"));
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
		this.model = model;
//		this.indList = ind;

		// Create dataset  
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		// Create chart  
		JFreeChart chart = ChartFactory.createTimeSeriesChart(  
				null, // Chart  
				LABELS.getString("dlgChartsXLabel"), // X-Axis Label  
				LABELS.getString("dlgChartsYLabel"), // Y-Axis Label  
				dataset, true, true, false);  

		// Changes background color  
//		XYPlot plot = (XYPlot)chart.getPlot();  
//		plot.setBackgroundPaint(new Color(255,255,196));

		ChartPanel panel = new ChartPanel(chart);  
		setContentPane(panel);
		
		for(int i: ind) {
			String name = UtilCollecion.getDescName(model.get(i));
			TimeSeries s = new TimeSeries(name);
			dataset.addSeries(s);
			series.put(i, s);
		}


		model.addListener(this);

//		series1.add(new Millisecond(1, 0, 0, 0, 1, Millisecond.JANUARY, 2023), 50);  

		setSize(800, 400);  
		setLocationRelativeTo(owner);  
		setVisible(true);  
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
	
	@Override
	public void dispose() {
		model.removeListener(this);
		super.dispose();
	}

	@Override
	public void update(EventType mesgType, Integer ind) {
		TimeSeries ts;
		if(mesgType == Devices.EventType.UPDATE && (ts = series.get(ind)) != null) {
			SwingUtilities.invokeLater(() -> {
//				System.out.println(ind);
				ShellyAbstractDevice d = model.get(ind);
				if(d instanceof InternalTmpHolder) {
					ts.addOrUpdate(new Millisecond(new Date(d.getLastTime())), ((InternalTmpHolder)d).getInternalTmp());
				}
			});
		}
	}  
}
