package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.KVS;
import it.usna.shellyscan.model.device.g2.modules.KVS.KVItem;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class KVSPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private ExTooltipTable table;
	private JScrollPane scrollPane = null;

	private static int COL_KEY = 0;
	private static int COL_VALUE = 2;

	public KVSPanel(AbstractG2Device device) throws IOException {
		setLayout(new BorderLayout(0, 0));

		final KVS kvs = new KVS(device);

		final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("lblKeyColName"), LABELS.getString("lblEtagColName"), LABELS.getString("lblValColName"));

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
				setAutoCreateRowSorter(true);
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				activateSingleCellStringCopy();
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				final int mCol = convertColumnIndexToModel(column);
				final int mRow = convertRowIndexToModel(row);
				return mCol == COL_VALUE || (mCol == COL_KEY && mRow >= kvs.size());
			}

//			@Override
//			public Component prepareEditor(TableCellEditor editor, int row, int column) {
////				JTextComponent comp = (JTextComponent)((DefaultCellEditor)editor).getComponent();
//				JTextComponent comp = (JTextComponent)super.prepareEditor(editor, row, column);
//				comp.setForeground(table.getSelectionForeground());
//				comp.setBackground(table.getSelectionBackground());
//				comp.setSelectedTextColor(table.getForeground());
//				return comp;
//			}

			@Override
			public void editingStopped(ChangeEvent e) {
				KVSPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					final int mRow = convertRowIndexToModel(getEditingRow());
					if (mRow < kvs.size()) { // existing element -> value
						String value = (String) getCellEditor().getCellEditorValue();
						if(value.equals(kvs.get(mRow).value()) == false) {
							KVItem item = kvs.edit(mRow, value);
							tModel.setRow(mRow, item.key(), item.etag(), item.value());
						}
					} else { // new element -> key edited
						String key = (String) getCellEditor().getCellEditorValue();
						if (kvs.getIndex(key) >= 0) {
							Msg.errorMsg(KVSPanel.this, "msgKVSExisting");
						} else if (key.length() > 0) {
							KVItem item = kvs.add(key, "");
							tModel.setRow(mRow, key, item.etag(), item.value());
						}
					}
					super.editingStopped(e);
				} catch (IOException e1) {
					Msg.errorMsg(KVSPanel.this, e1);
				} finally {
					KVSPanel.this.setCursor(Cursor.getDefaultCursor());
				}
			}

			//				NOT called on "esc"
			//				public void editingCanceled(ChangeEvent e) {
			//				}

			@Override
			public void removeEditor() {
				int edRow = getEditingRow();
				if(edRow >= 0) {
					final int mRow = convertRowIndexToModel(edRow);
					super.removeEditor();
					if (mRow >= kvs.size()) { // new aborted
						tModel.removeRow(mRow);
					}
				}
			}
		};

		scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		JPanel operationsPanel = new JPanel();
		operationsPanel.setLayout(new GridLayout(1, 0, 2, 0));
		operationsPanel.setBackground(Color.WHITE);
		add(operationsPanel, BorderLayout.SOUTH);

		final JButton btnDelete = new JButton(new UsnaAction(KVSPanel.this, "btnDelete", e -> {
			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
			final TableCellEditor editor = table.getCellEditor();
			if(editor != null)  {
				int edCol = table.convertColumnIndexToModel(table.getEditingColumn());
				editor.cancelCellEditing();
				if(edCol == COL_KEY) { // editing key -> new, not yet inserted, item
					return; // removeEditor() will do tModel.removeRow(...)
				}
			}
			final String cancel = UIManager.getString("OptionPane.cancelButtonText");
			if (JOptionPane.showOptionDialog(KVSPanel.this, LABELS.getString("msgDeleteConfirm"), LABELS.getString("btnDelete"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					new Object[] { UIManager.getString("OptionPane.yesButtonText"), cancel }, cancel) == 0) {
				try {
					kvs.delete(mRow);
					tModel.removeRow(mRow);
				} catch (IOException e1) {
					Msg.errorMsg(e1);
				}
			}
		}));
		operationsPanel.add(btnDelete);

		final JButton btnNew = new JButton(new UsnaAction("btnNew", e -> {
			TableCellEditor editor = table.getCellEditor();
			if(editor != null) {
				editor.stopCellEditing();
			}
			int row = tModel.addRow(LABELS.getString("lblKeyColName").toLowerCase(), "", "");
			int tabRow = table.convertRowIndexToView(row);
			//				table.setRowSelectionInterval(tabRow, tabRow);
			//				table./*getEditorComponent().*/requestFocus();
			int colKey = table.convertColumnIndexToView(COL_KEY);
			table.changeSelection(tabRow, colKey, false, false);
			table.editCellAt(tabRow, colKey);
			table.getEditorComponent().requestFocus();

		}));
		operationsPanel.add(btnNew);
		
//		final JButton btnRefresh = new JButton(new UsnaAction(KVSPanel.this, "labelRefresh", e -> {
//			kvs = new KVS(device);
//			TableCellEditor editor = table.getCellEditor();
//			if(editor != null) {
//				editor.cancelCellEditing();
//			}
//			scrollPane.getHorizontalScrollBar().setValue(0);
//			tModel.clear();
//			kvs.getItems().stream().forEach(item -> tModel.addRow(item.key(), item.etag(), item.value()));
//		}));
//		operationsPanel.add(btnRefresh);

		// fill table
		kvs.getItems().stream().forEach(item -> tModel.addRow(item.key(), item.etag(), item.value()));

		ListSelectionListener l = e -> {
			final boolean selection = table.getSelectedRowCount() > 0;
			btnDelete.setEnabled(selection);
		};
		table.getSelectionModel().addListSelectionListener(l);
		l.valueChanged(null);
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		SwingUtilities.invokeLater(() -> {
			if (v) {
				table.columnsWidthAdapt();
				TableColumn col0 = table.getColumnModel().getColumn(0);
				col0.setPreferredWidth(col0.getPreferredWidth() * 120 / 100);
				TableColumn col1 = table.getColumnModel().getColumn(1);
				col1.setPreferredWidth(col1.getPreferredWidth() * 120 / 100);
				TableColumn col2 = table.getColumnModel().getColumn(2);
				col2.setPreferredWidth(col2.getPreferredWidth() * 110 / 100);
				if (scrollPane.getViewport().getWidth() > table.getPreferredSize().width) {
					table.setAutoResizeMode(ExTooltipTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
				} else {
					table.setAutoResizeMode(ExTooltipTable.AUTO_RESIZE_OFF);
				}
			} else {
				if(table.isEditing()) {
					table.getCellEditor().stopCellEditing();
				}
			}
		});
	}
}