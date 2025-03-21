package it.usna.shellyscan.view.checklist;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.InetAddressAndPort;
import it.usna.shellyscan.model.device.blu.BluInetAddressAndPort;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

class CheckListTable extends ExTooltipTable {
	private static final long serialVersionUID = 1L;
	private final static Color GREEN_OK = new Color(0, 192, 0);
	public final static int COL_STATUS = 0;
	public final static int COL_NAME = 1;
	public final static int COL_IP = 2;
	public final static int COL_ECO = 3;
	public final static int COL_LED = 4;
	public final static int COL_LOGS = 5;
	public final static int COL_BLE = 6;
	public final static int COL_AP = 7;
	public final static int COL_ROAMING = 8;
	public final static int COL_WIFI1 = 9;
	public final static int COL_WIFI2 = 10;
	public final static int COL_EXTENDER = 11;
	public final static int COL_SCRIPTS = 12;
	public final static int COL_LAST = COL_SCRIPTS;
	
	public CheckListTable(UsnaTableModel tModel, final SortOrder ipSort) {
		super(tModel, true);
		
		columnModel.getColumn(COL_STATUS).setMaxWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 2);
		columnModel.getColumn(COL_STATUS).setMinWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 2);
		setHeadersTooltip(
				LABELS.getString("col_status_exp"), null, null, LABELS.getString("col_eco_tooltip"), LABELS.getString("col_ledoff_tooltip"), LABELS.getString("col_logs_tooltip"),
				LABELS.getString("col_blt_tooltip"), LABELS.getString("col_AP_tooltip"), LABELS.getString("col_roaming_tooltip"), LABELS.getString("col_wifi1_tooltip"),
				LABELS.getString("col_wifi2_tooltip"), LABELS.getString("col_extender_tooltip"), LABELS.getString("col_scripts_tooltip"));

		TableCellRenderer rendTrueOk = new CheckRenderer(true);
		TableCellRenderer rendFalseOk = new CheckRenderer(false);
		columnModel.getColumn(COL_IP).setCellRenderer(new InetAddressAndPortRenderer());
		columnModel.getColumn(COL_ECO).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_LED).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_LOGS).setCellRenderer(rendFalseOk);
		columnModel.getColumn(COL_BLE).setCellRenderer(new StringJudgedRenderer("0", CheckListView.FALSE_STR));
		columnModel.getColumn(COL_AP).setCellRenderer(rendFalseOk);
		columnModel.getColumn(COL_ROAMING).setCellRenderer(rendFalseOk);
		columnModel.getColumn(COL_WIFI1).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_WIFI2).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_EXTENDER).setCellRenderer(new StringJudgedRenderer("0", CheckListView.FALSE_STR));
		columnModel.getColumn(COL_SCRIPTS).setCellRenderer(new StringJudgedRenderer(null, null));

		TableRowSorter<?> rowSorter = ((TableRowSorter<?>) getRowSorter());
		rowSorter.setSortsOnUpdates(true);
		final Comparator<?> sorter = (o1, o2) -> { // null, Boolean, String
			String s1 = o1 == null ? "" : o1.toString();
			String s2 = o2 == null ? "" : o2.toString();
			return s1.compareTo(s2);
		};
		rowSorter.setComparator(COL_ECO, sorter);
		rowSorter.setComparator(COL_LED, sorter);
		rowSorter.setComparator(COL_LOGS, sorter);
		rowSorter.setComparator(COL_BLE, sorter);
		rowSorter.setComparator(COL_AP, sorter);
		rowSorter.setComparator(COL_ROAMING, sorter);
		rowSorter.setComparator(COL_WIFI1, sorter);
		rowSorter.setComparator(COL_WIFI2, sorter);
		rowSorter.setComparator(COL_EXTENDER, sorter);
		rowSorter.setComparator(COL_SCRIPTS, sorter);

		if (ipSort != SortOrder.UNSORTED) {
			sortByColumn(COL_IP, ipSort);
		}
	}
	
	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component comp = super.prepareRenderer(renderer, row, column);
		if(isRowSelected(row) == false) {
			comp.setBackground((row % 2 == 0) ? Main.TAB_LINE1_COLOR : Main.TAB_LINE2_COLOR);
		}
		return comp;
	}
	
	private static class StringJudgedRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		private String redValue;
		private String greenValue;
		
		public StringJudgedRenderer(final String redValue, final String greenValue) {
			this.redValue = redValue;
			this.greenValue = greenValue;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel ret = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(value == null) {
				ret.setText(CheckListView.NOT_APPLICABLE_STR);
				if (isSelected == false) {
					ret.setForeground(table.getForeground());
				}
			} else if(value.toString().equals(redValue)) {
				ret.setForeground(Color.red);
				if (isSelected) {
					ret.setFont(ret.getFont().deriveFont(Font.BOLD));
				}
			} else if(value.toString().equals(greenValue)) {
				ret.setForeground(GREEN_OK);
				if (isSelected) {
					ret.setFont(ret.getFont().deriveFont(Font.BOLD));
				}
			} else if (isSelected == false) {
				ret.setForeground(table.getForeground());
			}
			return ret;
		}
	}

	private static class CheckRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		private final boolean goodVal;

		private CheckRenderer(boolean goodVal) {
			this.goodVal = goodVal;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component ret;
			if (value instanceof Boolean val) {
				if (val) {
					ret = super.getTableCellRendererComponent(table, CheckListView.TRUE_STR, isSelected, hasFocus, row, column);
					ret.setForeground(goodVal ? GREEN_OK : Color.red);
				} else {
					ret = super.getTableCellRendererComponent(table, CheckListView.FALSE_STR, isSelected, hasFocus, row, column);
					ret.setForeground(goodVal ? Color.red : GREEN_OK);
				}
				if (isSelected) {
					ret.setFont(ret.getFont().deriveFont(Font.BOLD));
				}
			} else {
				ret = super.getTableCellRendererComponent(table, value == null ? CheckListView.NOT_APPLICABLE_STR : value, isSelected, hasFocus, row, column);
				if (isSelected == false) {
					ret.setForeground(table.getForeground());
				}
			}
			return ret;
		}
	}
	
	private static class InetAddressAndPortRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JLabel ret = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			List<InetAddressAndPort> parents;
			if(value instanceof BluInetAddressAndPort bluAddr && (parents = bluAddr.getAlternativeParents()).size() > 0) {
				ret.setText(bluAddr.getRepresentation() + parents.stream().map(InetAddressAndPort::getRepresentation).collect(Collectors.joining(" / ", " / ", "")));
				ret.setForeground(Color.red);
				if (isSelected) {
					ret.setFont(ret.getFont().deriveFont(Font.BOLD));
				}
			} else if (isSelected == false) {
				ret.setForeground(table.getForeground());
			}
			return ret;
		}
	}
}