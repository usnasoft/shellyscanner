package it.usna.shellyscan.view.chart;

import java.io.BufferedWriter;
import java.io.File;
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

	void exportAsCSV(File out, String separator, DateRange range) throws IOException {
		Path outPath = IOFile.addExtension(out.toPath(), "csv");
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
}