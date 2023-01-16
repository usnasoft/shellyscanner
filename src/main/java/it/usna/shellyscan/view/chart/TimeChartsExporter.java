package it.usna.shellyscan.view.chart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jfree.data.time.DateRange;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

public class TimeChartsExporter {
	private final TimeSeriesCollection dataset;
	
	public TimeChartsExporter(TimeSeriesCollection dataset) {
		this.dataset = dataset;
	}

	void exportAsCSV(File out, String separator, DateRange range) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(out.toPath())) {
			@SuppressWarnings("unchecked")
			List<TimeSeries> tsList = dataset.getSeries();
			for(TimeSeries ts: tsList) {
				w.write(ts.getKey().toString() + "\n");
				@SuppressWarnings("unchecked")
				List<TimeSeriesDataItem> diList = ts.getItems();
				Stream.Builder<String> timeCollector = Stream.builder();
				Stream.Builder<String> valueCollector = Stream.builder();
				for(TimeSeriesDataItem di: diList) {
					Date tic = di.getPeriod().getStart();
					if(range == null || (tic.getTime() >= range.getLowerMillis() && tic.getTime() <= range.getUpperMillis())) {
						timeCollector.accept(String.format(Locale.ENGLISH, "%1$tF %1$tT", tic));
						valueCollector.accept(String.format(Locale.ENGLISH, "%.2f", di.getValue()));
					}
				}
				w.write(timeCollector.build().collect(Collectors.joining(separator)));
				w.newLine();
				w.write(valueCollector.build().collect(Collectors.joining(separator)));
				w.newLine();
			}
		}
	}
}
