package it.usna.shellyscan.view.chart;

import static it.usna.shellyscan.Main.LABELS;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jfree.data.time.DateRange;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import it.usna.util.IOFile;

public class TimeChartsExporter {
	private final TimeSeriesCollection dataset;
	
	public TimeChartsExporter(TimeSeriesCollection dataset) {
		this.dataset = dataset;
	}

	void exportAsHorizontalCSV(Path out/*, String meas*/, String separator, DateRange range) throws IOException {
		Path outPath = IOFile.addExtension(out, "csv");
		try (BufferedWriter w = Files.newBufferedWriter(outPath)) {
			@SuppressWarnings("unchecked")
			List<TimeSeries> tsList = dataset.getSeries();
			for(TimeSeries ts: tsList) {
				w.write(ts.getKey().toString());
				w.newLine();
				@SuppressWarnings("unchecked")
				List<TimeSeriesDataItem> diList = ts.getItems();
				Stream.Builder<Long> millisCollector = Stream.builder();
				Stream.Builder<String> dateTimeCollector = Stream.builder();
				Stream.Builder<String> valueCollector = Stream.builder();
				for(TimeSeriesDataItem di: diList) {
					RegularTimePeriod period = di.getPeriod();
//					Date tic = di.getPeriod().getStart();
//					if(range == null || (tic.getTime() >= range.getLowerMillis() && tic.getTime() <= range.getUpperMillis())) {
					if(range == null || (period.getFirstMillisecond() >= range.getLowerMillis() && period.getLastMillisecond() <= range.getUpperMillis())) {
						millisCollector.accept(period.getFirstMillisecond());
						dateTimeCollector.accept(String.format(Locale.ENGLISH, "%1$tF %1$tT", period.getStart()));
						valueCollector.accept(String.format(Locale.ENGLISH, "%.2f", di.getValue()));
					}
				}
				w.write(millisCollector.build().map(m -> m.toString()).collect(Collectors.joining(separator)));
				w.newLine();
				w.write(dateTimeCollector.build().collect(Collectors.joining(separator)));
				w.newLine();
				w.write(valueCollector.build().collect(Collectors.joining(separator)));
				w.newLine();
			}
		}
	}
	
	void exportAsVerticalCSV(Path out, String meas, String separator, DateRange range) throws IOException {
		Path outPath = IOFile.addExtension(out, "csv");
		Stream.Builder<String> firstRowCollector = Stream.builder();
		Stream.Builder<String> labelRowCollector = Stream.builder();
		int seriesCount = dataset.getSeriesCount();
		@SuppressWarnings("unchecked")
		List<TimeSeries> tsList = dataset.getSeries();
		try (BufferedWriter w = Files.newBufferedWriter(outPath)) {
			
			//head
			int maxItem = 0;
			for(int ser = 0; ser < seriesCount; ser++) {
				TimeSeries ts = tsList.get(ser);
				if(ser > 0) {
					firstRowCollector.add("").add("").add("");
				}
				firstRowCollector.accept(ts.getKey().toString());
				if(ser > 0) {
					labelRowCollector.add("");
				}
				labelRowCollector.add(LABELS.getString("cvsLabelUnixtime")).add(LABELS.getString("cvsLabelTime")).add(meas);
				maxItem = Math.max(maxItem, ts.getItemCount());
			}
			w.write(firstRowCollector.build().collect(Collectors.joining(separator)));
			w.newLine();
			w.write(labelRowCollector.build().collect(Collectors.joining(separator)));
			w.newLine();
			
			//body
			for(int i = 0; i < maxItem; i++) {
				Stream.Builder<String> row = Stream.builder();
				boolean validRow = false;
				for(int ser = 0; ser < seriesCount; ser++) {
					if(ser > 0) {
						row.add("");
					}
					TimeSeries ts = tsList.get(ser);
					if(i < ts.getItemCount()) {
						TimeSeriesDataItem di = ts.getDataItem(i);
						RegularTimePeriod period = di.getPeriod();
						if(range == null || (period.getFirstMillisecond() >= range.getLowerMillis() && period.getLastMillisecond() <= range.getUpperMillis())) {
							row.add(period.getFirstMillisecond() + "").add(String.format(Locale.ENGLISH, "%1$tF %1$tT", period.getStart())).add(String.format(Locale.ENGLISH, "%.2f", di.getValue()));
							validRow = true;
						} else {
							row.add("").add("").add("");
						}
					} else {
						row.add("").add("").add("");
					}
				}
				if(validRow) {
					w.write(row.build().collect(Collectors.joining(separator)));
					w.newLine();
				}
			}
		}
	}
}