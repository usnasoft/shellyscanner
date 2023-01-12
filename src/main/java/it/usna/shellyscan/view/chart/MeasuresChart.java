package it.usna.shellyscan.view.chart;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
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
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.util.UsnaEventListener;  

// https://www.javatpoint.com/jfreechart-tutorial
public class MeasuresChart extends JFrame implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private final Devices model;
	private final Map<Integer, TimeSeries[]> seriesMap = new HashMap<>();

	private enum ChartType {
		INT_TEMP("dlgChartsIntTempLabel", "dlgChartsIntTempYLabel"),
		RSSI("dlgChartsRSSILabel", "dlgChartsRSSIYLabel"),
		P("dlgChartsAPowerLabel", "dlgChartsAPowerYLabel", Meters.Type.W),
		Q("dlgChartsQPowerLabel", "dlgChartsQPowerYLabel", Meters.Type.VAR),
		V("dlgChartsVoltageLabel", "dlgChartsVoltageYLabel", Meters.Type.V),
		I("dlgChartsCurrentLabel", "dlgChartsCurrentYLabel", Meters.Type.I),
		T("dlgChartsTempLabel", "dlgChartsTempYLabel", Meters.Type.T),
		H("dlgChartsHumidityLabel", "dlgChartsHumidityYLabel", Meters.Type.H),
		LUX("dlgChartsLuxLabel", "dlgChartsLuxYLabel", Meters.Type.L);

		private final String yLabel;
		private final String label;
		private Meters.Type mType;

		private ChartType(String labelID, String yLabelID) {
			this.yLabel = LABELS.getString(yLabelID);
			this.label = LABELS.getString(labelID);
		}

		private ChartType(String labelID, String yLabelID, Meters.Type mType) {
			this.yLabel = LABELS.getString(yLabelID);
			this.label = LABELS.getString(labelID);
			this.mType = mType;
		}

		@Override
		public String toString() { // combo box
			return label;
		}
	}

	private ChartType currentType = ChartType.INT_TEMP;

	public MeasuresChart(JFrame owner, final Devices model, int[] ind) {  
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
				"val", // Y-Axis Label  
				dataset, true, true, false);  

		XYPlot plot = chart.getXYPlot();
		//		plot.setBackgroundPaint(new Color(255,255,196));

		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setMouseZoomable(false);

		mainPanel.add(chartPanel, BorderLayout.CENTER);

		JPanel commandPanel = new JPanel(new BorderLayout());
		JPanel westCommandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		commandPanel.add(westCommandPanel, BorderLayout.WEST);
		JPanel eastCommandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		commandPanel.add(eastCommandPanel, BorderLayout.EAST);
		mainPanel.add(commandPanel, BorderLayout.SOUTH);
		
		JButton btnClear = new JButton(LABELS.getString("dlgChartsBtnClear"));
		btnClear.addActionListener(e -> initDataSet(plot.getRangeAxis(), dataset, model, ind));
		JButton btnClose = new JButton(LABELS.getString("dlgClose"));
		btnClose.addActionListener(e -> dispose());
		eastCommandPanel.add(btnClear);
		eastCommandPanel.add(btnClose);

		JComboBox<String> rangeCombo = new JComboBox<>();
		ValueAxis xAxis = plot.getDomainAxis();
		rangeCombo.addItem(LABELS.getString("dlgChartsRangeAuto"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange1min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange5min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange15min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange30min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange60min"));
		rangeCombo.addActionListener(e -> {
			int selected = rangeCombo.getSelectedIndex();
			if(selected == 1) xAxis.setFixedAutoRange(1000 * 1 * 60);
			else if(selected == 2) xAxis.setFixedAutoRange(1000 * 5 * 60);
			else if(selected == 3) xAxis.setFixedAutoRange(1000 * 15 * 60);
			else if(selected == 4) xAxis.setFixedAutoRange(1000 * 30 * 60);
			else if(selected == 5) xAxis.setFixedAutoRange(1000 * 60 * 60);
			else xAxis.setFixedAutoRange(0);
		});

		westCommandPanel.add(new JLabel(LABELS.getString("dlgChartsRangeComboLabel")));
		westCommandPanel.add(rangeCombo);
		westCommandPanel.add(new JLabel(LABELS.getString("dlgChartsTypeComboLabel")));

		JComboBox<ChartType> typeCombo = new JComboBox<>();
		for(ChartType t: ChartType.values()) {
			typeCombo.addItem(t);
		}
		typeCombo.addActionListener(e -> {
			currentType = (ChartType)typeCombo.getSelectedItem();
			initDataSet(plot.getRangeAxis(), dataset, model, ind);
		});

		westCommandPanel.add(typeCombo);

		initDataSet(plot.getRangeAxis(), dataset, model, ind);

		NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
		NumberFormat df = NumberFormat.getNumberInstance();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);
		yAxis.setNumberFormatOverride(df);

		getRootPane().registerKeyboardAction(e -> {
			int selected = rangeCombo.getSelectedIndex();
			rangeCombo.setSelectedIndex(++selected >= rangeCombo.getItemCount() ? 0 : selected);
		} , KeyStroke.getKeyStroke(KeyEvent.VK_R, MainView.SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);

		//		getRootPane().registerKeyboardAction(e -> {
		//			int selected = typeCombo.getSelectedIndex();
		//			typeCombo.setSelectedIndex(++selected >= typeCombo.getItemCount() ? 0 : selected);
		//		} , KeyStroke.getKeyStroke(KeyEvent.VK_G, MainView.SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);

		model.addListener(this);

		setSize(800, 460);
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	private void initDataSet(ValueAxis yAxis, TimeSeriesCollection dataset, final Devices model, int[] indexes) {
		dataset.removeAllSeries();
		yAxis.setLabel(currentType.yLabel);
		for(int ind: indexes) {
			final ShellyAbstractDevice d = model.get(ind);
			final String name = UtilCollecion.getDescName(d);
			
			if(currentType.mType == null) {
				TimeSeries s = new TimeSeries(name);
				dataset.addSeries(s);
				seriesMap.put(ind, new TimeSeries[] {s});
			} else {
				ArrayList<TimeSeries> temp = new ArrayList<>();
				Meters[] meters = d.getMeters();
				if(meters != null) {
					for(int i = 0; i < meters.length; i++) {
						if(meters[i].hasType(currentType.mType)) {
							final String sName = temp.size() == 0 ? name : name + "-" + (i + 1);
							TimeSeries s = new TimeSeries(sName);
							temp.add(s);
							dataset.addSeries(s);
						}
					}
				}
				if(temp.size() == 0) {
					dataset.addSeries(new TimeSeries(name)); // legend
				}
				seriesMap.put(ind, temp.toArray(new TimeSeries[temp.size()]));
			}
			update(Devices.EventType.UPDATE, ind);
		}
	}

	@Override
	public void dispose() {
		model.removeListener(this);
		super.dispose();
	}

	@Override
	public void update(EventType mesgType, Integer ind) {
		TimeSeries ts[];
		if(mesgType == Devices.EventType.UPDATE && (ts = seriesMap.get(ind)) != null) {
			SwingUtilities.invokeLater(() -> {
				// System.out.println(ind);
				final ShellyAbstractDevice d = model.get(ind);
				if(d.getStatus() == Status.ON_LINE) {
					final Millisecond timestamp = new Millisecond(new Date(d.getLastTime()));
					Meters[] m;
					if(currentType == ChartType.INT_TEMP && d instanceof InternalTmpHolder) {
						ts[0].addOrUpdate(timestamp, ((InternalTmpHolder)d).getInternalTmp());
					} else if(currentType == ChartType.RSSI) {
						ts[0].addOrUpdate(timestamp, d.getRssi());
					} else if(/*currentType.mType != null &&*/ (m = d.getMeters()) != null) {
						for(int i = 0; i < m.length; i++) {
							if(m[i].hasType(currentType.mType)) {
								ts[i].addOrUpdate(timestamp, m[i].getValue(currentType.mType));
							}
						}
					}
				}
			});
		}
	}  
}