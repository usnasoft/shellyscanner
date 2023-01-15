package it.usna.shellyscan.view.chart;

import static it.usna.shellyscan.Main.LABELS;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.view.appsettings.DialogAppSettings;
import it.usna.util.AppProperties;

public class ChartsUtil {

	static void exportCSV(JFrame parent, AppProperties appProp, TimeSeriesCollection dataset) {
		final JFileChooser fc = new JFileChooser();
		final String path = appProp.getProperty("LAST_PATH");
		if(path != null) {
			fc.setCurrentDirectory(new File(path));
		}
		fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_csv_desc"), "csv"));
		if(fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			File out = fc.getSelectedFile();
			if(out.getName().contains(".") == false) {
				out = new File(out.getParentFile(), out.getName() + ".csv");
			}
			String separator = appProp.getProperty(DialogAppSettings.PROP_CSV_SEPARATOR);
			try (FileWriter w = new FileWriter(out)) {
				@SuppressWarnings("unchecked")
				List<TimeSeries> tsList = dataset.getSeries();
				for(TimeSeries ts: tsList) {
					w.write(ts.getKey().toString() + "\n");
					@SuppressWarnings("unchecked")
					List<TimeSeriesDataItem> diList = ts.getItems();
					Stream.Builder<String> time = Stream.builder();
					Stream.Builder<String> value = Stream.builder();
					for(TimeSeriesDataItem di: diList) {
						time.accept(String.format(Locale.ENGLISH, "%1$tF %1$tT", di.getPeriod().getStart()));
						value.accept(String.format(Locale.ENGLISH, "%.2f", di.getValue()));
					}
					w.write(time.build().collect(Collectors.joining(separator)) + "\n");
					w.write(value.build().collect(Collectors.joining(separator)) + "\n");
				}
				JOptionPane.showMessageDialog(parent, LABELS.getString("msgFileSaved"), Main.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException ex) {
				Main.errorMsg(ex);
			}
			appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
		}
	}
}
