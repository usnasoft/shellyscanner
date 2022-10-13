package it.usna.shellyscan.view;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.modules.DeviceModule;
import it.usna.shellyscan.model.device.modules.RelayInterface;
import it.usna.swing.table.ExTooltipTable;

public class DevicesTable extends ExTooltipTable {
	private static final long serialVersionUID = 1L;
	final static int COL_STATUS_IDX = 0;
	final static int COL_TYPE = 1;
	final static int COL_NAME = 2;
	final static int COL_IP_IDX = 3;
	final static int COL_RSSI_IDX = 4;
	final static int COL_CLOUD = 5;
	final static int COL_UPTIME_IDX = 6;
	final static int COL_INT_TEMP = 7;
	final static int COL_DEBUG = 8;
	final static int COL_COMMAND_IDX = 9;

	private boolean adaptTooltipLocation = false;
	private final DevicesTableRenderer renderer = new DevicesTableRenderer();

	public DevicesTable(TableModel tm) {
		super(tm, true);
		columnModel.getColumn(COL_STATUS_IDX).setMaxWidth(MainView.ONLINE_BULLET.getIconWidth() + 4);
		final TableColumn colCommand = columnModel.getColumn(COL_COMMAND_IDX);
		colCommand.setCellRenderer(renderer);
		colCommand.setCellEditor(new DeviceTableCellEditor(this));

		TableRowSorter<?> sorter = (TableRowSorter<?>)getRowSorter();
		sorter.setComparator(COL_COMMAND_IDX, (o1, o2) -> {
			final String s1, s2;
			if(o1 == null) {
				s1 = "";
			} else if (o1 instanceof DeviceModule[]) {
				s1 = ((DeviceModule[])o1)[0].getLabel();
			} else if (o1 instanceof DeviceModule) {
				s1 = ((DeviceModule)o1).getLabel();
			} else {
				s1 = o1.toString();
			}
			if(o2 == null) {
				s2 = "";
			} else if (o2 instanceof DeviceModule[]) {
				s2 = ((DeviceModule[])o2)[0].getLabel();
			} else if (o1 instanceof DeviceModule) {
				s2 = ((DeviceModule)o2).getLabel();
			} else {
				s2 = o2.toString();
			}
			return s1.compareTo(s2);
		});

//		sorter.setSortsOnUpdates(true); // messes rows heights and selection
		setUpdateSelectionOnSort(true);
	}

	@Override
	public boolean isCellEditable(final int row, final int column) {
		Object val = getValueAt(row, column);
		return val instanceof DeviceModule || val instanceof DeviceModule[];
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component comp = super.prepareRenderer(renderer, row, column);
		if(isRowSelected(row) == false) {
			if(row % 2 == 0 ) {
				comp.setBackground(Main.TAB_LINE1);
			} else {
				comp.setBackground(Main.TAB_LINE2);
			}
		}
		return comp;
	}
	
	public void stopCellEditing() {
		TableCellEditor editor = getCellEditor();
		if(editor != null) {
			editor.stopCellEditing();
		}
	}

	@Override
	public String getToolTipText(final MouseEvent evt) {
		final int r, c;
		final Object value;
		if(((Component) evt.getSource()).isVisible() && (r = rowAtPoint(evt.getPoint())) >= 0 && (c = columnAtPoint(evt.getPoint())) >= 0 && (value = getValueAt(r, c)) != null) {
			if(c == convertColumnIndexToView(COL_UPTIME_IDX)) {
				adaptTooltipLocation = false;
				long s = ((Number)value).longValue();
				final int gg = (int)(s / (3600 * 24));
				s = s % (3600 * 24);
				int hh = (int)(s / 3600);
				s = s % 3600;
				int mm = (int)(s / 60);
				s = s % 60;
				return String.format(Main.LABELS.getString("col_uptime_tooltip"), gg, hh, mm, s);
			} else if (value instanceof ImageIcon) {
				adaptTooltipLocation = false;
				return ((ImageIcon)value).getDescription();
			} else if(value instanceof RelayInterface[]) {
				adaptTooltipLocation = false;
				String tt = Arrays.stream((RelayInterface[])value).
						map(rel -> String.format(Main.LABELS.getString("col_last_source_tooltip"), rel, rel.getLastSource())).collect(Collectors.joining("<br>"));
				return "<html>" + tt + "</html>";
//			} else if(value instanceof Roller) {
//				adaptTooltipLocation = false;
//				return String.format(Main.LABELS.getString("col_last_source_tooltip"), value, ((Roller)value).getLastSource());
//			} else if(value instanceof LightWhite) {
//				adaptTooltipLocation = false;
//				return String.format(Main.LABELS.getString("col_last_source_tooltip"), value, ((LightWhite)value).getLastSource());
//			} else if(value instanceof LightBulbRGB) {
//				adaptTooltipLocation = false;
//				return String.format(Main.LABELS.getString("col_last_source_tooltip"), value, ((LightBulbRGB)value).getLastSource());
//			} else if(value instanceof LightRGBW) {
//				adaptTooltipLocation = false;
//				return String.format(Main.LABELS.getString("col_last_source_tooltip"), value, ((LightRGBW)value).getLastSource());
			} else if(value instanceof DeviceModule) {
				final String ret;
				if((ret = ((DeviceModule)value).getLastSource()) != null) {
					adaptTooltipLocation = false;
					return String.format(Main.LABELS.getString("col_last_source_tooltip"), value, ret);
				}
			} else {
				adaptTooltipLocation = true;
				return super.getToolTipText(evt);
			}
		}
		return null;
	}

	@Override
	public Point getToolTipLocation(final MouseEvent evt) {
		if(adaptTooltipLocation) {
			return super.getToolTipLocation(evt);
		} else {
			return null;
		}
	}
	
	public void resetRowsComputedHeight() {
		renderer.resetRowsHeight();
	}

	@Override
	public void columnsWidthAdapt() {
		Graphics g = getGraphics();
		if(g != null) {
			final FontMetrics fm = g.getFontMetrics();
			for(int c = 1; c < getColumnCount(); c++) {
				Object val = columnModel.getColumn(c).getHeaderValue();
				int width = val != null ? SwingUtilities.computeStringWidth(fm, val.toString()) : 1;
				for(int r = 0; r < getRowCount(); r++) {
					val = getValueAt(r, c);
					if(val != null) {
						if(val instanceof Object[]) {
							for(Object v: (Object[])val) {
								int w = SwingUtilities.computeStringWidth(fm, v.toString());
								width = Math.max(width, w);
							}
						} else {
							width = Math.max(width, SwingUtilities.computeStringWidth(fm, val.toString()));
						}
					}
				}
				columnModel.getColumn(c).setPreferredWidth(width);
			}
		}
	}
	
	public void csvExport(Writer w, String separator) throws IOException {
		Stream.Builder<String> h = Stream.builder();
		for(int col = 0; col < getColumnCount(); col++) {
			String name = getColumnName(col);
			h.accept(name.length() == 0 ? Main.LABELS.getString("col_status_exp") : name); // dirty and fast
		}
		w.write(h.build().collect(Collectors.joining(separator)) + "\n");

		for(int row = 0; row < getRowCount(); row++) {
			Stream.Builder<Object> r = Stream.builder();
			for(int col = 0; col < getColumnCount(); col++) {
				r.accept(getValueAt(row, col));
			}
			w.write(r.build().map(cell -> {
				if(cell == null) return "";
				else if(cell instanceof Object[]) return Arrays.stream((Object[])cell).map(el -> el.toString()).collect(Collectors.joining(" + "));
				else return cell.toString();
			}).collect(Collectors.joining(separator)) + "\n");
		}
	}

	//https://stackoverflow.com/questions/1783607/auto-adjust-the-height-of-rows-in-a-jtable
	//	private void updateRowHeights() {
	//	    for (int row = 0; row < devicesTable.getRowCount(); row++) {
	//	        int rowHeight = devicesTable.getRowHeight();
	//	        for (int column = 0; column < devicesTable.getColumnCount(); column++) {
	//	            Component comp = devicesTable.prepareRenderer(devicesTable.getCellRenderer(row, column), row, column);
	//	            rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
	//	        }
	//	        devicesTable.setRowHeight(row, rowHeight);
	//	    }
	//	}
	public void setRowFilter(String filter) {
		TableRowSorter<?> sorter = (TableRowSorter<?>)getRowSorter();
		filter = filter.replace("\\E", "\\e");
		sorter.setRowFilter(RowFilter.regexFilter("(?i).*\\Q" + filter + "\\E.*", COL_TYPE, COL_NAME));
	}
}