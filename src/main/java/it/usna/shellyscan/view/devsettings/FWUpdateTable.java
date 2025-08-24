package it.usna.shellyscan.view.devsettings;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.modules.FirmwareManager;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.swing.table.ExTooltipTable;

public class FWUpdateTable extends ExTooltipTable {
	private static final long serialVersionUID = 1L;
	static final int COL_STATUS = 0;
	static final int COL_ID = 1;
	static final int COL_CURRENT = 2;
	static final int COL_STABLE = 3;
	static final int COL_BETA = 4;
	
	private final PanelFWUpdate fwPanel;

	public FWUpdateTable(TableModel tm, PanelFWUpdate fwPanel) {
		super(tm);
		this.fwPanel = fwPanel;

		TableRowSorter<TableModel> sorter = new TableRowSorter<>(tm);
		sorter.setStringConverter(new TableStringConverter() {
			@Override
			public String toString(TableModel model, int row, int column) {
				if(model.getValueAt(row, column) == null) return "";
				FirmwareManager fw = fwPanel.getFirmwareManager(row);
				if(column == COL_CURRENT && fw != null) {
					return FirmwareManager.getShortVersion(fw.current());
				} else if(column == COL_STABLE && fw != null) {
					return FirmwareManager.getShortVersion(fw.newStable());
				} else if(column == COL_BETA && fw != null) {
					return FirmwareManager.getShortVersion(fw.newBeta());
				}
				return model.getValueAt(row, column).toString();
			}
		});
		this.setRowSorter(sorter);

		getTableHeader().setReorderingAllowed(false);
		TableCellRenderer fwRendered = new FWCellRendered();
		columnModel.getColumn(COL_STABLE).setCellRenderer(fwRendered);
		columnModel.getColumn(COL_BETA).setCellRenderer(fwRendered);
		columnModel.getColumn(COL_STATUS).setMaxWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 2);
		columnModel.getColumn(COL_STATUS).setMinWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 2);
		
		// On update COL_STABLE value class is String for the updating row, if this is the first row ... see UsnaTableModel.getColumnClass(...))
		TableCellEditor booleanEditor = getDefaultEditor(Boolean.class);
		columnModel.getColumn(COL_STABLE).setCellEditor(booleanEditor);
		columnModel.getColumn(COL_BETA).setCellEditor(booleanEditor);
		
		activateSingleCellStringCopy();
//		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

	@Override
	public boolean isCellEditable(final int row, final int column) {
		return getValueAt(row, column) instanceof Boolean;
	}
	
	@Override
	public Component prepareEditor(TableCellEditor editor, int row, int column) {
		JCheckBox editorComponent = (JCheckBox)super.prepareEditor(editor, row, column);
		FirmwareManager fw = fwPanel.getFirmwareManager(convertRowIndexToModel(row));
		if(fw != null) {
			editorComponent.setText(FirmwareManager.getShortVersion(column == COL_STABLE ? fw.newStable() : fw.newBeta()));
		} else if(column == COL_STABLE) { // no info -> try update
			editorComponent.setText(Main.LABELS.getString("labelUpdateToAny"));
		}
		editorComponent.setBackground(getSelectionBackground());
		editorComponent.setForeground(getSelectionForeground());
		editorComponent.setHorizontalAlignment(JLabel.LEFT);
		return editorComponent;
	}

	@Override
	public void editingStopped(ChangeEvent e) {
		final int r = getEditingRow();
		final int c = getEditingColumn();
		super.editingStopped(e);
		if(r >= 0 && c >= 0 && getValueAt(r, c) == Boolean.TRUE) {
			final int toOff = (c == COL_STABLE) ? COL_BETA : COL_STABLE;
			if(getValueAt(r, toOff) instanceof Boolean) {
				setValueAt(Boolean.FALSE, r, toOff);
			}
		}
		fwPanel.countSelection();
	}
	
	@Override
	protected String getToolTipText(Object value, boolean cellTooSmall, int row, int col) {
		if (value != null && (col == COL_CURRENT || col == COL_STABLE || col == COL_BETA)) {
			return cellValueAsString(value, row, col);
		} else {
			return super.getToolTipText(value, cellTooSmall, row, col);
		}
	}

	@Override
	protected String cellValueAsString(Object value, int row, int column) {
		FirmwareManager fw = fwPanel.getFirmwareManager(convertRowIndexToModel(row));
		if(column == COL_CURRENT && fw != null) {
			return fw.current();
		} else if(column == COL_STABLE && fw != null) {
			return fw.newStable();
		} else if(column == COL_BETA && fw != null) {
			return fw.newBeta();
		}
		return super.cellValueAsString(value, row, column);
	}

	@Override
	public Point getToolTipLocation(final MouseEvent evt) {
		final int column = columnAtPoint(evt.getPoint());
		if(column == COL_STABLE || column == COL_BETA) {
			final int row = rowAtPoint(evt.getPoint());
			final Rectangle cellRec = getCellRect(row, column, true);
			return new Point(cellRec.x + 16, cellRec.y); // +16 -> do not overlap to checkbox
		} else {
			return super.getToolTipLocation(evt);
		}
	}
	
	public void setRowFilter(String filter) {
		TableRowSorter<?> sorter = (TableRowSorter<?>)getRowSorter();
		if(filter.isEmpty()) {
			sorter.setRowFilter(null);
		} else {
			RowFilter<TableModel, Integer> regexFilter = RowFilter.regexFilter("(?i).*\\Q" + filter.replace("\\E", "\\e") + "\\E.*", COL_ID, COL_CURRENT, COL_STABLE, COL_BETA);
			sorter.setRowFilter(regexFilter);
		}
	}

	private class FWCellRendered implements TableCellRenderer {
		private FWCellRendered() {
			JCheckBox booleanRenderer = (JCheckBox)getDefaultRenderer(Boolean.class);
			booleanRenderer.setOpaque(true);
			booleanRenderer.setHorizontalAlignment(JCheckBox.LEFT);	
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if(value == null || value instanceof Boolean) {
				JCheckBox comp = (JCheckBox)table.getDefaultRenderer(Boolean.class).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if(value == null) {
					comp.setEnabled(false);
					comp.setText("");
				} else {
					comp.setEnabled(true);
					FirmwareManager fw = fwPanel.getFirmwareManager(convertRowIndexToModel(row));
					if(fw != null) {
						comp.setText(FirmwareManager.getShortVersion(column == COL_STABLE ? fw.newStable() : fw.newBeta()));
					} else if(column == COL_STABLE) { // no info -> try update
						comp.setText(Main.LABELS.getString("labelUpdateToAny"));
					}
				}
				return comp;
			} else {
				return table.getDefaultRenderer(String.class).getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		}
	}
}