package it.usna.shellyscan.view.chart;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.util.UsnaEventListener;  

public class MeasuresChart extends JFrame implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private final Devices model;
	private final Map<Integer, TimeSeries> series = new HashMap<>();

	public MeasuresChart(JFrame owner, final Devices model, List<Integer> ind) {  
		super(LABELS.getString("dlgChartsTitle"));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		this.model = model;

		JPanel mainPanel = new JPanel(new BorderLayout());
		this.setContentPane(mainPanel);

		// Create dataset  
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		// Create chart  
		JFreeChart chart = ChartFactory.createTimeSeriesChart(  
				null, // Chart  
				LABELS.getString("dlgChartsXLabel"), // X-Axis Label  
				LABELS.getString("dlgChartsYLabel"), // Y-Axis Label  
				dataset, true, true, false);  

		// Changes background color  
		XYPlot plot = chart.getXYPlot();
		//		plot.setBackgroundPaint(new Color(255,255,196));

		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setMouseZoomable(false);

		mainPanel.add(chartPanel, BorderLayout.CENTER);

		JPanel commandPanel = new JPanel(new BorderLayout());
		mainPanel.add(commandPanel, BorderLayout.SOUTH);

		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.addActionListener(e -> dispose());

		commandPanel.add(btnClose, BorderLayout.EAST);

		JComboBox<String> rangeCombo = new JComboBox<>();
		ValueAxis xAxis = plot.getDomainAxis();
		rangeCombo.addItem(LABELS.getString("dlgChartsRangeAuto"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange5min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange15min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange30min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange60min"));
		rangeCombo.addActionListener(e -> {
			int selected = rangeCombo.getSelectedIndex();
			if(selected == 1) xAxis.setFixedAutoRange(1000 * 5 * 60);
			else if(selected == 2) xAxis.setFixedAutoRange(1000 * 15 * 60);
			else if(selected == 3) xAxis.setFixedAutoRange(1000 * 30 * 60);
			else if(selected == 4) xAxis.setFixedAutoRange(1000 * 60 * 60);
			else xAxis.setFixedAutoRange(0);
		});

		JPanel westCommandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		westCommandPanel.add(new JLabel(LABELS.getString("dlgChartsRangeLabel")));
		westCommandPanel.add(rangeCombo);
		commandPanel.add(westCommandPanel, BorderLayout.WEST);

		for(int i: ind) {
			String name = UtilCollecion.getDescName(model.get(i));
			TimeSeries s = new TimeSeries(name);
			dataset.addSeries(s);
			series.put(i, s);
			update(Devices.EventType.UPDATE, i);
		}

		NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
		NumberFormat df = NumberFormat.getNumberInstance();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
		yAxis.setNumberFormatOverride(df);

		model.addListener(this);

		setSize(800, 450);
		setLocationRelativeTo(owner);
		setVisible(true);
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
				// System.out.println(ind);
				final ShellyAbstractDevice d = model.get(ind);
				if(d.getStatus() == Status.ON_LINE) {
					if(d instanceof InternalTmpHolder) {
						ts.addOrUpdate(new Millisecond(new Date(d.getLastTime())), ((InternalTmpHolder)d).getInternalTmp());
					}
				}
			});
		}
	}  
}