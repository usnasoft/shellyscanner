package it.usna.shellyscan.controller;

import static it.usna.shellyscan.Main.LABELS;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.Msg;
import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.util.AppProperties;
import it.usna.util.IOFile;

public class ExportCSVAction extends UsnaAction {
	private static final long serialVersionUID = 1L;
//	private final static Logger LOG = LoggerFactory.getLogger(ExportCSVAction.class);

	public ExportCSVAction(MainView mainView, DevicesTable devicesTable) {
		super(mainView, "action_csv_name", "action_csv_tooltip", null, "/images/Table.png");
		
		setActionListener(e -> {
			AppProperties appProp = ScannerProperties.instance();
			final JFileChooser fc = new JFileChooser(appProp.getProperty("LAST_PATH"));
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_csv_desc"), "csv"));
			if(fc.showSaveDialog(mainView) == JFileChooser.APPROVE_OPTION) {
				Path outPath = IOFile.addExtension(fc.getSelectedFile().toPath(), "csv");
				try (BufferedWriter writer = Files.newBufferedWriter(outPath)) {
					export(devicesTable, writer, appProp.getProperty(ScannerProperties.PROP_CSV_SEPARATOR));
					JOptionPane.showMessageDialog(mainView, LABELS.getString("msgFileSaved"), Main.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
				} catch (/*IO*/Exception ex) {
					Msg.errorMsg(mainView, ex);
				}
				appProp.setProperty("LAST_PATH", fc.getCurrentDirectory().getPath());
			}
		});
	}
	
	private static void export(DevicesTable devicesTable, BufferedWriter w, String separator) throws IOException {
		Stream.Builder<String> h = Stream.builder();
		for(int col = 0; col < devicesTable.getColumnCount(); col++) {
			String name = devicesTable.getColumnName(col);
			h.accept(name.isEmpty() ? LABELS.getString("col_status_exp") : name); // dirty and fast
		}
		w.write(h.build().collect(Collectors.joining(separator)));
		w.newLine();

		for(int row = 0; row < devicesTable.getRowCount(); row++) {
//			try {
				Stream.Builder<String> r = Stream.builder();
				for(int col = 0; col < devicesTable.getColumnCount(); col++) {
					r.accept(devicesTable.cellValueAsString(devicesTable.getValueAt(row, col), row, col));
				}
				w.write(r.build().collect(Collectors.joining(separator)));
				w.newLine();
//			} catch(RuntimeException e) {
//				LOG.error("csvExport", e);
//			}
		}
	}
}