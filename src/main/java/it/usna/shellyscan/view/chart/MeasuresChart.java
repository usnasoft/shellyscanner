package it.usna.shellyscan.view.chart;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.DateRange;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.Devices.EventType;
import it.usna.shellyscan.model.device.InternalTmpHolder;
import it.usna.shellyscan.model.device.Meters;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.util.AppProperties;
import it.usna.util.UsnaEventListener;  

// https://www.javatpoint.com/jfreechart-tutorial
public class MeasuresChart extends JFrame implements UsnaEventListener<Devices.EventType, Integer> {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(MeasuresChart.class);
	private final static NumberFormat NF = NumberFormat.getNumberInstance(Locale.ENGLISH);
	static {
		NF.setMaximumFractionDigits(2);
		NF.setMinimumFractionDigits(2);
	}
	private final Devices model;
	private final Map<Integer, TimeSeries[]> seriesMap = new HashMap<>();

	private static boolean outStream = false;

	private JComboBox<String> seriesCombo = new JComboBox<>();

	public enum ChartType {
		INT_TEMP("dlgChartsIntTempLabel", "dlgChartsIntTempYLabel"),
		RSSI("dlgChartsRSSILabel", "dlgChartsRSSIYLabel"),
		P("dlgChartsAPowerLabel", "dlgChartsAPowerYLabel", Meters.Type.W),
		P_SUM("dlgChartsAPowerSumLabel", "dlgChartsAPowerYLabel"),
		Q("dlgChartsQPowerLabel", "dlgChartsQPowerYLabel", Meters.Type.VAR),
		V("dlgChartsVoltageLabel", "dlgChartsVoltageYLabel", Meters.Type.V),
		I("dlgChartsCurrentLabel", "dlgChartsCurrentYLabel", Meters.Type.I),
		//		T("dlgChartsTempLabel", "dlgChartsTempYLabel", Meters.Type.T),
		//		T1("dlgChartsTemp1Label", "dlgChartsTempYLabel", Meters.Type.T1),
		//		T2("dlgChartsTemp2Label", "dlgChartsTempYLabel", Meters.Type.T2),
		//		T3("dlgChartsTemp3Label", "dlgChartsTempYLabel", Meters.Type.T3),
		//		T4("dlgChartsTemp4Label", "dlgChartsTempYLabel", Meters.Type.T4),
		T_ALL("dlgChartsTempAllLabel", "dlgChartsTempYLabel"),
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

	private ChartType currentType;

	public MeasuresChart(JFrame owner, final Devices model, int[] ind, AppProperties appProp) {
		setIconImages(owner.getIconImages());
		setTitle(LABELS.getString("dlgChartsTitle") + " - " + (ind.length == 1 ? UtilMiscellaneous.getDescName(model.get(ind[0])) : ind.length));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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

		NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
		yAxis.setNumberFormatOverride(NF);

		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
		renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator("{0}: {1} - {2}", new SimpleDateFormat("HH:mm:ss.SSS"), NF));

		ChartPanel chartPanel = new ChartPanel(chart, false, true, true, false, true);
		chartPanel.setInitialDelay(0); // tootip
		chartPanel.setDismissDelay(20_000); // tootip
		//chartPanel.setMouseZoomable(true);
		//chartPanel.setMouseWheelEnabled(true);

		mainPanel.add(chartPanel, BorderLayout.CENTER);

		JPanel commandPanel = new JPanel(new BorderLayout());
		JPanel westCommandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		commandPanel.add(westCommandPanel, BorderLayout.WEST);
		JPanel eastCommandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		commandPanel.add(eastCommandPanel, BorderLayout.EAST);
		mainPanel.add(commandPanel, BorderLayout.SOUTH);

		JButton btnClear = new JButton(new UsnaAction("dlgChartsBtnClear", e -> initDataSet(plot.getRangeAxis(), dataset, model, ind)));
		JButton btnClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		eastCommandPanel.add(btnClear);
		eastCommandPanel.add(btnClose);

		ValueAxis xAxis = plot.getDomainAxis();

		JComboBox<String> rangeCombo = new JComboBox<>();
		rangeCombo.addItem(LABELS.getString("dlgChartsRangeAuto"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange1min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange5min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange15min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange30min"));
		rangeCombo.addItem(LABELS.getString("dlgChartsRange60min"));

		westCommandPanel.add(new JLabel(LABELS.getString("dlgChartsRangeComboLabel")));
		westCommandPanel.add(rangeCombo);
		westCommandPanel.add(new JLabel(LABELS.getString("dlgChartsTypeComboLabel")));

		JComboBox<ChartType> typeCombo = new JComboBox<>();
		for(ChartType t: ChartType.values()) {
			typeCombo.addItem(t);
		}
		westCommandPanel.add(typeCombo);

		JToggleButton btnPause = new JToggleButton(new ImageIcon(MeasuresChart.class.getResource("/images/Pause16.png")));
		btnPause.setSelectedIcon(new ImageIcon(MeasuresChart.class.getResource("/images/Play16.png")));
		btnPause.setRolloverEnabled(false);
		btnPause.setPreferredSize(new Dimension(33, 28));
		btnPause.setToolTipText(LABELS.getString("dlgChartsPauseTooltip"));
		btnPause.addActionListener(e ->  {
			if(btnPause.isSelected()) {
				xAxis.setRange(xAxis.getRange());
			} else {
				setRange(xAxis, rangeCombo.getSelectedIndex());
			}
		});

		ImageIcon markerIcon = new ImageIcon(MeasuresChart.class.getResource("/images/Tag16.png"));
		JToggleButton btnMarks = new JToggleButton(new ImageIcon(GrayFilter.createDisabledImage(markerIcon.getImage())));
		btnMarks.setSelectedIcon(markerIcon);
		btnMarks.setRolloverEnabled(false);
		btnMarks.setPreferredSize(new Dimension(33, 28));
		btnMarks.setToolTipText(LABELS.getString("dlgChartsMarkersTooltip"));
		btnMarks.addActionListener(e -> {
			for(int i = 0; i < dataset.getSeriesCount(); i++) {
				renderer.setSeriesShapesVisible(i, btnMarks.isSelected() && renderer.getSeriesLinesVisible(i)); //renderer.setDefaultShapesVisible(btnMarks.isSelected());
			}
		});

		JButton btnDownload = new JButton(new UsnaAction(MeasuresChart.this, "dlgChartsCSVTooltip", "/images/DownloadEmpty16.png", e -> {
			try {
				final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
				fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_csv_desc"), "csv"));
				if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					TimeChartsExporter exp = new TimeChartsExporter(dataset);
					File out = fc.getSelectedFile();
					DateRange range = btnPause.isSelected() ? (DateRange)xAxis.getRange() : null;
					if("V".equals(appProp.getProperty(DialogAppSettings.PROP_CHARTS_EXPORT))) {
						exp.exportAsVerticalCSV(out, currentType.yLabel, appProp.getProperty(DialogAppSettings.PROP_CSV_SEPARATOR, DialogAppSettings.PROP_CSV_SEPARATOR_DEFAULT), range);
					} else {
						exp.exportAsHorizontalCSV(out, appProp.getProperty(DialogAppSettings.PROP_CSV_SEPARATOR, DialogAppSettings.PROP_CSV_SEPARATOR_DEFAULT), range);
					}
					appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
					JOptionPane.showMessageDialog(this, LABELS.getString("msgFileSaved"), Main.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (IOException ex) {
				Msg.errorMsg(ex);
			}
		}));
		//btnDownload.setPreferredSize(new Dimension(33, 28));

		JButton btnCopy = new JButton(new UsnaAction(null, "btnCopy", "/images/Toolbar-Copy16.png", e -> chartPanel.doCopy()));
		//btnCopy.setPreferredSize(new Dimension(33, 28));

		westCommandPanel.add(new JLabel(LABELS.getString("dlgChartsSeriesLabel")));
		westCommandPanel.add(seriesCombo);

		//westCommandPanel.add(Box.createHorizontalStrut(10));
		westCommandPanel.add(btnMarks);
		westCommandPanel.add(btnPause);
		westCommandPanel.add(btnDownload);
		westCommandPanel.add(btnCopy);

		rangeCombo.addActionListener(e -> {
			btnPause.setSelected(false);
			setRange(xAxis, rangeCombo.getSelectedIndex());
		});

		typeCombo.addActionListener(e -> {
			currentType = (ChartType)typeCombo.getSelectedItem();
			initDataSet(plot.getRangeAxis(), dataset, model, ind);
			btnPause.setSelected(false);
			xAxis.setAutoRange(true);
			//setRange(xAxis, rangeCombo.getSelectedIndex());
		});

		seriesCombo.addActionListener(e -> {
			if(seriesCombo.getItemCount() > 0) {
				if(seriesCombo.getSelectedIndex() == 0) {
					for(int i = 0; i < dataset.getSeriesCount(); i++ ) {
						renderer.setSeriesLinesVisible(i, true);
						renderer.setSeriesShapesVisible(i, btnMarks.isSelected());
					}
				} else {
					for(int i = 0; i < dataset.getSeriesCount(); i++ ) {
						renderer.setSeriesLinesVisible(i, seriesCombo.getSelectedIndex() - 1 == i);
						renderer.setSeriesShapesVisible(i, seriesCombo.getSelectedIndex() - 1 == i && btnMarks.isSelected());
					}
				}
			}
		});

		this.currentType = ChartType.valueOf(appProp.getProperty(DialogAppSettings.PROP_CHARTS_START, ChartType.INT_TEMP.name()));
		typeCombo.setSelectedItem(currentType);

		initDataSet(plot.getRangeAxis(), dataset, model, ind);

		getRootPane().registerKeyboardAction(e -> {
			int selected = rangeCombo.getSelectedIndex();
			rangeCombo.setSelectedIndex(++selected >= rangeCombo.getItemCount() ? 0 : selected);
		} , KeyStroke.getKeyStroke(KeyEvent.VK_R, MainView.SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(e -> btnPause.doClick(), KeyStroke.getKeyStroke(KeyEvent.VK_P, MainView.SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(e -> chartPanel.doCopy(), KeyStroke.getKeyStroke(KeyEvent.VK_C, MainView.SHORTCUT_KEY), JComponent.WHEN_IN_FOCUSED_WINDOW);

		model.addListener(this);

		setSize(900, 460);
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	private static void setRange(ValueAxis xAxis, int selected) {
		if(selected == 1) xAxis.setFixedAutoRange(1000 * 1 * 60);
		else if(selected == 2) xAxis.setFixedAutoRange(1000 * 5 * 60);
		else if(selected == 3) xAxis.setFixedAutoRange(1000 * 15 * 60);
		else if(selected == 4) xAxis.setFixedAutoRange(1000 * 30 * 60);
		else if(selected == 5) xAxis.setFixedAutoRange(1000 * 60 * 60);
		else xAxis.setFixedAutoRange(0); // selected == 0
		xAxis.setAutoRange(true);
	}

	private void initDataSet(ValueAxis yAxis, TimeSeriesCollection dataset, final Devices model, int[] modelIndexes) {
		dataset.removeAllSeries();
		yAxis.setLabel(currentType.yLabel);
		for(int ind: modelIndexes) {
			final ShellyAbstractDevice d = model.get(ind);
			if(currentType == ChartType.T_ALL) {
				final ArrayList<TimeSeries> temp = new ArrayList<>(5);
				final Meters[] meters = d.getMeters();
				if(meters != null) {
					for(int i = 0; i < meters.length; i++) {
						if(meters[i].hasType(Meters.Type.T)) {
							TimeSeries ts = new TimeSeries(UtilMiscellaneous.getDescName(d));
							temp.add(ts);
							dataset.addSeries(ts);
						}
						if(meters[i].hasType(Meters.Type.T1)) {
							TimeSeries ts = new TimeSeries(UtilMiscellaneous.getDescName(d) + "-1");
							temp.add(ts);
							dataset.addSeries(ts);
						}
						if(meters[i].hasType(Meters.Type.T2)) {
							TimeSeries ts = new TimeSeries(UtilMiscellaneous.getDescName(d) + "-2");
							temp.add(ts);
							dataset.addSeries(ts);
						}
						if(meters[i].hasType(Meters.Type.T3)) {
							TimeSeries ts = new TimeSeries(UtilMiscellaneous.getDescName(d) + "-3");
							temp.add(ts);
							dataset.addSeries(ts);
						}
						if(meters[i].hasType(Meters.Type.T4)) {
							TimeSeries ts = new TimeSeries(UtilMiscellaneous.getDescName(d) + "-4");
							temp.add(ts);
							dataset.addSeries(ts);
						}
					}
				}
				if(temp.size() == 0) {
					dataset.addSeries(new TimeSeries(UtilMiscellaneous.getDescName(d))); // legend
				}
				seriesMap.put(ind, temp.toArray(TimeSeries[]::new));
			} else if(currentType.mType == null) { // device property (INT_TEMP & RSSI), not from "Meters" or P_SUM
				TimeSeries ts = new TimeSeries(UtilMiscellaneous.getDescName(d));
				dataset.addSeries(ts);
				seriesMap.put(ind, new TimeSeries[] {ts});
			} else {
				ArrayList<TimeSeries> temp = new ArrayList<>(5);
				Meters[] meters = d.getMeters();
				if(meters != null) {
					for(int i = 0; i < meters.length; i++) {
						if(meters[i].hasType(currentType.mType)) {
							TimeSeries ts = new TimeSeries(UtilMiscellaneous.getDescName(d, i));
							temp.add(ts);
							dataset.addSeries(ts);
						}
					}
				}
				if(temp.size() == 0) {
					dataset.addSeries(new TimeSeries(UtilMiscellaneous.getDescName(d))); // legend
				}
				seriesMap.put(ind, temp.toArray(TimeSeries[]::new));
			}
			update(Devices.EventType.UPDATE, ind);
		}

		// seriesCombo
		seriesCombo.removeAllItems();
		seriesCombo.addItem(LABELS.getString("dlgChartsShowAllSeriesLabel"));
		for(int i = 0; i < dataset.getSeriesCount(); i++ ) {
			seriesCombo.addItem(dataset.getSeries(i).getKey().toString());
		}
	}

	@Override
	public void dispose() {
		LOG.trace("closing charts");
		model.removeListener(this);
		super.dispose();
	}

	@Override
	public void update(EventType mesgType, Integer ind) {
		if(mesgType == Devices.EventType.UPDATE) {
			final TimeSeries ts[];
			final ShellyAbstractDevice d;
			if((ts = seriesMap.get(ind)) != null && ((d = model.get(ind)).getStatus() == Status.ON_LINE || ts[0].getItemCount() == 0)) { // ts[0].getItemCount() == 0 for battery devices
				SwingUtilities.invokeLater(() -> {
					try {
						final Millisecond timestamp = new Millisecond(new Date(d.getLastTime()));
						Meters[] m;
						if(currentType == ChartType.INT_TEMP && d instanceof InternalTmpHolder tempH) {
							ts[0].addOrUpdate(timestamp, tempH.getInternalTmp());
							outStream(d, 0, currentType.name(), timestamp, tempH.getInternalTmp());
						} else if(currentType == ChartType.RSSI) {
							ts[0].addOrUpdate(timestamp, d.getRssi());
							outStream(d, 0, currentType.name(), timestamp, d.getRssi());
						} else if(currentType == ChartType.T_ALL && (m = d.getMeters()) != null) {
							int j = 0;
							for(int i = 0; i < m.length; i++) {
								if(m[i].hasType(Meters.Type.T)) {
									float val = m[i].getValue(Meters.Type.T);
									ts[j++].addOrUpdate(timestamp, val);
									outStream(d, 0, /*ChartType.T*/"T", timestamp, val);
								}
								if(m[i].hasType(Meters.Type.T1)) {
									float val = m[i].getValue(Meters.Type.T1);
									ts[j++].addOrUpdate(timestamp, val);
									outStream(d, 0, /*ChartType.T1*/"T1", timestamp, val);
								}
								if(m[i].hasType(Meters.Type.T2)) {
									float val = m[i].getValue(Meters.Type.T2);
									ts[j++].addOrUpdate(timestamp, val);
									outStream(d, 0, /*ChartType.T2*/"T2", timestamp, val);
								}
								if(m[i].hasType(Meters.Type.T3)) {
									float val = m[i].getValue(Meters.Type.T3);
									ts[j++].addOrUpdate(timestamp, val);
									outStream(d, 0, /*ChartType.T3*/"T3", timestamp, val);
								}
								if(m[i].hasType(Meters.Type.T4)) {
									float val = m[i].getValue(Meters.Type.T4);
									ts[j/*++*/].addOrUpdate(timestamp, val);
									outStream(d, 0, /*ChartType.T4*/"T4", timestamp, val);
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
								ts[0].addOrUpdate(timestamp, sumW);
								outStream(d, 0, currentType.name(), timestamp, sumW);
							}
						} else if((m = d.getMeters()) != null) {
							int j = 0;
							for(int i = 0; i < m.length; i++) {
								if(m[i].hasType(currentType.mType)) {
									float val = m[i].getValue(currentType.mType);
									ts[j].addOrUpdate(timestamp, val);
									outStream(d, j, currentType.name(), timestamp, val);
									j++;
								}
							}
						}
					} catch (Throwable ex) {
						LOG.error("Unexpected", ex);
					}
				});
			}
		} else if(mesgType == Devices.EventType.CLEAR) {
			SwingUtilities.invokeLater(() -> dispose());
		}
	}

	private static void outStream(ShellyAbstractDevice d, int channel, String type, Millisecond timestamp, float val) {
		if(outStream) {
			System.out.println("graph_data->" + d.getHostname() + ":" + channel + ":" + type + ":" + timestamp.getFirstMillisecond() + ":" + val);
		}
	}

	public static void setDoOutStream(boolean stream) {
		outStream = stream;
	}
}