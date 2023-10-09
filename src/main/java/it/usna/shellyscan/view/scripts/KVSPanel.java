package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class KVSPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final ExTooltipTable table;
	private final UsnaTableModel tModel =
			new UsnaTableModel(LABELS.getString("lblKeyColName"), LABELS.getString("lblEtagColName"), LABELS.getString("lblValColName"));
	JScrollPane scrollPane = null;
	
	public KVSPanel(AbstractG2Device device) {
		setLayout(new BorderLayout(0, 0));

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
//				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
				setAutoCreateRowSorter(true);
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

//				columnModel.getColumn(2).setCellRenderer(new ButtonCellRenderer());
//				columnModel.getColumn(2).setCellEditor(new ButtonCellEditor());
				
//				setAutoResizeMode(ExTooltipTable.AUTO_RESIZE_OFF);
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				return false;
			}

//			@Override
//			public Component prepareEditor(TableCellEditor editor, int row, int column) {
//				JComponent comp = (JComponent)super.prepareEditor(editor, row, column);
//				comp.setBackground(table.getSelectionBackground());
//				comp.setForeground(table.getSelectionForeground());
//				return comp;
//			}
//
//			public void editingStopped(ChangeEvent e) {
//				KVSPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				try {
//					final int mCol = convertColumnIndexToModel(getEditingColumn());
//					final int mRow = convertRowIndexToModel(getEditingRow());
//					final Script sc = scripts.get(mRow);
//					if(mCol == 0) { // name
//						sc.setName((String)getCellEditor().getCellEditorValue());
//					} else if(mCol == 1) { // enabled
//						sc.setEnabled((Boolean)getCellEditor().getCellEditorValue());
//					}
//					super.editingStopped(e);
//				} finally {
//					KVSPanel.this.setCursor(Cursor.getDefaultCursor());
//				}
//			}
		};
		
		/*JScrollPane*/ scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		JPanel operationsPanel = new JPanel();
		operationsPanel.setLayout(new GridLayout(1, 0, 2, 0));
		operationsPanel.setBackground(Color.WHITE);
		add(operationsPanel, BorderLayout.SOUTH);

//		JButton btnDelete = new JButton(LABELS.getString("btnDelete"));
//		btnDelete.addActionListener(e -> {
//			final String cancel = UIManager.getString("OptionPane.cancelButtonText");
//			if(JOptionPane.showOptionDialog(
//					KVSPanel.this, LABELS.getString("msdDeleteConfitm"), LABELS.getString("btnDelete"),
//					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
//					new Object[] {UIManager.getString("OptionPane.yesButtonText"), cancel}, cancel) == 0) {
//				try {
//					final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//					final Script sc = scripts.get(mRow);
//					sc.delete();
//					scripts.remove(mRow);
//					tModel.removeRow(mRow);
//				} catch (IOException e1) {
//					Msg.errorMsg(e1);
//				}
//			}
//		});
//		operationsPanel.add(btnDelete);
//
//		JButton btnNew = new JButton(LABELS.getString("btnNew"));
//		btnNew.addActionListener(e -> {
//			try {
//				Script sc = Script.create(device, null);
//				scripts.add(sc);
//				tModel.addRow(new Object [] {sc.getName(), sc.isEnabled(), sc.isRunning()});
//			} catch (IOException e1) {
//				Msg.errorMsg(e1);
//			}
//		});
//		operationsPanel.add(btnNew);
//
//		JButton btnDownload = new JButton(LABELS.getString("btnDownload"));
//		operationsPanel.add(btnDownload);
//		btnDownload.addActionListener(e -> {
//			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//			final Script sc = scripts.get(mRow);
//			final JFileChooser fc = new JFileChooser();
//			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
//			fc.setSelectedFile(new File(sc.getName()));
//			if(fc.showSaveDialog(KVSPanel.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
//					w.write(sc.getCode());
//				} catch (IOException e1) {
//					Msg.errorMsg(KVSPanel.this, LABELS.getString("msgScrNoCode"));
//				} finally {
//					setCursor(Cursor.getDefaultCursor());
//				}
//			}
//		});
//
//		JButton btnUpload = new JButton(LABELS.getString("btnUpload"));
//		operationsPanel.add(btnUpload);
//		btnUpload.addActionListener(e -> {
//			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//			final Script sc = scripts.get(mRow);
//			final JFileChooser fc = new JFileChooser();
//			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
//			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
//			fc.setSelectedFile(new File(sc.getName()));
//			if(fc.showOpenDialog(KVSPanel.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				loadCodeFromFile(fc.getSelectedFile(), sc);
//				setCursor(Cursor.getDefaultCursor());
//			}
//		});

		try {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			KVS kvs = new KVS(device);
			for(KVS.KVItem item: kvs.getItems()) {
				tModel.addRow(new Object [] {item.key, item.etag, item.value});
			}
		} catch (IOException e) {
			Msg.errorMsg(e);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
		
//		ListSelectionListener l = e -> {
//			final boolean selection = table.getSelectedRowCount() > 0;
//			btnDelete.setEnabled(selection);
//			btnDownload.setEnabled(selection);
//			btnUpload.setEnabled(selection);
////			editBtn.setEnabled(selection);
//		};
//		table.getSelectionModel().addListSelectionListener(l);
//		l.valueChanged(null);
	}
	
	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		if(v) {
			table.columnsWidthAdapt();
			TableColumn col0 = table.getColumnModel().getColumn(0);
			col0.setPreferredWidth(col0.getPreferredWidth() * 120 / 100);
			TableColumn col1 = table.getColumnModel().getColumn(1);
			col1.setPreferredWidth(col1.getPreferredWidth() * 120 / 100);
			TableColumn col2 = table.getColumnModel().getColumn(2);
			col2.setPreferredWidth(col2.getPreferredWidth() * 120 / 100);
			if(scrollPane.getViewport().getWidth() > table.getPreferredSize().width) {
				table.setAutoResizeMode(ExTooltipTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			} else {
				table.setAutoResizeMode(ExTooltipTable.AUTO_RESIZE_OFF);
			}
		}
	}
	
	private class KVS {
		private final AbstractG2Device device;
		private ArrayList<KVItem> kvItems = new ArrayList<>();
		
		public KVS(AbstractG2Device device) throws IOException {
			this.device = device;
			refresh();
		}
		
		public void refresh() throws IOException {
			kvItems.clear();
			JsonNode kvsItems = device.getJSON("/rpc/KVS.GetMany");
			Iterator<Entry<String, JsonNode>> fields = kvsItems.path("items").fields();
			while(fields.hasNext()) {
				Entry<String, JsonNode> item = fields.next();
				kvItems.add(new KVItem(item.getKey(), item.getValue().get("etag").asText(), item.getValue().get("value").asText()));
			}
		}
		
		public ArrayList<KVItem> getItems() {
			return kvItems;
		}
		
		public void delete(int index) throws IOException {
			device.getJSON("/rpc/KVS.Delete?key=" + URLEncoder.encode(kvItems.get(index).key, StandardCharsets.UTF_8.name()));
		}
		
		public void modify(int index, String value) throws IOException {
			String key = kvItems.get(index).key;
			JsonNode node = device.getJSON("/rpc/KVS.Set?key=" + URLEncoder.encode(kvItems.get(index).key, StandardCharsets.UTF_8.name()) + "&value=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			kvItems.set(index, new KVItem(key, node.get("etag").asText(), value));
		}
		
		public void add(String key, String value) throws IOException {
			JsonNode node = device.getJSON("/rpc/KVS.Set?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8.name()) + "&value=" + URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
			kvItems.add(new KVItem(key, node.get("etag").asText(), value));
		}
		
		public record KVItem(String key, String etag, String value) {}
	}
}