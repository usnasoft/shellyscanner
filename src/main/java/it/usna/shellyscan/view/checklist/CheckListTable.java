package it.usna.shellyscan.view.checklist;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Collection;
import java.util.Comparator;

import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.blu.BLEGateway;
import it.usna.shellyscan.model.device.blu.BluInetAddressAndPort;
import it.usna.shellyscan.view.DevicesTable;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

class CheckListTable extends ExTooltipTable {
	private static final long serialVersionUID = 1L;
	private static final Color GREEN_OK = new Color(0, 192, 0);
	public static final int COL_STATUS = 0;
	public static final int COL_NAME = 1;
	public static final int COL_IP = 2;
	public static final int COL_ECO = 3;
	public static final int COL_LED = 4;
	public static final int COL_LOGS = 5;
	public static final int COL_BLE = 6;
	public static final int COL_AP = 7;
	public static final int COL_ROAMING = 8;
	public static final int COL_WIFI1 = 9;
	public static final int COL_WIFI2 = 10;
	public static final int COL_EXTENDER = 11;
	public static final int COL_SCRIPTS = 12;
	public static final int COL_AUTO_FW_UPDATE = 13;
	public static final int COL_LAST = COL_AUTO_FW_UPDATE;
	
	public CheckListTable(UsnaTableModel tModel, final SortOrder ipSort) {
		super(tModel, true);
		
		columnModel.getColumn(COL_STATUS).setMaxWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 2);
		columnModel.getColumn(COL_STATUS).setMinWidth(DevicesTable.ONLINE_BULLET.getIconWidth() + 2);
		setHeadersTooltip(
				LABELS.getString("col_status_exp"), null, null, LABELS.getString("col_eco_tooltip"), LABELS.getString("col_ledoff_tooltip"), LABELS.getString("col_logs_tooltip"),
				LABELS.getString("col_blt_tooltip"), LABELS.getString("col_AP_tooltip"), LABELS.getString("col_roaming_tooltip"), LABELS.getString("col_wifi1_tooltip"),
				LABELS.getString("col_wifi2_tooltip"), LABELS.getString("col_extender_tooltip"), LABELS.getString("col_scripts_tooltip"), LABELS.getString("col_auto_fw_update_tooltip"));

		TableCellRenderer rendTrueOk = new CheckRenderer(true);
		TableCellRenderer rendFalseOk = new CheckRenderer(false);
		columnModel.getColumn(COL_IP).setCellRenderer(new InetAddressAndPortRenderer());
		columnModel.getColumn(COL_ECO).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_LED).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_LOGS).setCellRenderer(rendFalseOk);
		columnModel.getColumn(COL_BLE).setCellRenderer(new BLERenderer());
		columnModel.getColumn(COL_AP).setCellRenderer(new BooleanRenderer());
		columnModel.getColumn(COL_ROAMING).setCellRenderer(rendFalseOk);
		columnModel.getColumn(COL_WIFI1).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_WIFI2).setCellRenderer(rendTrueOk);
		columnModel.getColumn(COL_EXTENDER).setCellRenderer(new StringJudgedRenderer("0", CheckListView.FALSE_STR));
		columnModel.getColumn(COL_SCRIPTS).setCellRenderer(new StringJudgedRenderer(null, null));
		columnModel.getColumn(COL_AUTO_FW_UPDATE).setCellRenderer(new StringJudgedRenderer(null, null));

		TableRowSorter<?> rowSorter = ((TableRowSorter<?>) getRowSorter());
		rowSorter.setSortsOnUpdates(true);
		final Comparator<?> sorter = (o1, o2) -> { // use when there is a mix: null, Boolean, String
			String s1 = o1 == null ? "" : o1.toString();
			String s2 = o2 == null ? "" : o2.toString();
			return s1.compareTo(s2);
		};
		final Comparator<?> collectionSorter = (o1, o2) -> { // use when there is a mix: null, Boolean, String
			int x1 = 0;
			int x2 = 0;
			if(o1 instanceof Collection c) x1 = c.size();
			else if(o1.equals(CheckListView.FALSE_STR)) x1 = Integer.MIN_VALUE;
			else if(o1.equals(CheckListView.TRUE_STR)) x1 = Integer.MIN_VALUE + 1;
			if(o2 instanceof Collection c) x2 = c.size();
			else if(o2.equals(CheckListView.FALSE_STR)) x2 = Integer.MIN_VALUE;
			else if(o2.equals(CheckListView.TRUE_STR)) x2 = Integer.MIN_VALUE + 1;
			return x1 - x2;
		};
		rowSorter.setComparator(COL_ECO, sorter);
		rowSorter.setComparator(COL_LED, sorter);
		rowSorter.setComparator(COL_LOGS, sorter);
		rowSorter.setComparator(COL_BLE, collectionSorter);
		rowSorter.setComparator(COL_AP, sorter);
		rowSorter.setComparator(COL_ROAMING, sorter);
		rowSorter.setComparator(COL_WIFI1, sorter);
		rowSorter.setComparator(COL_WIFI2, sorter);
		rowSorter.setComparator(COL_EXTENDER, sorter);
//		rowSorter.setComparator(COL_SCRIPTS, sorter);

		if (ipSort != SortOrder.UNSORTED) {
			sortByColumn(COL_IP, ipSort);
		}
	}
	
	@Override
	protected String getToolTipText(Object value, boolean cellTooSmall, int r, int c) {
		if(value instanceof Collection<?> gwCollection && convertColumnIndexToModel(c) == COL_BLE) {
			StringBuilder res = new StringBuilder("<html><table>");
			// BLU devices (list of gateways)
			gwCollection.stream().filter(w -> w instanceof BLEGateway).map(w -> (BLEGateway) w).sorted(Comparator.reverseOrder()).forEach(gw -> {
				res
				.append("<tr><td>").append(UtilMiscellaneous.getDescName(gw.gw()))
				.append("</td><td>").append(gw.gw().getAddressAndPort())
				.append("</td><td>").append(System.currentTimeMillis()/1000 - gw.lastSeen())
				.append("</td></tr>");
			});
			// Gateways (list of BLU devices)
			gwCollection.stream().filter(w -> w instanceof BLEGateway == false).forEach(blu -> {
				if(blu instanceof ShellyAbstractDevice bth) {
					res
					.append("<tr><td>").append(UtilMiscellaneous.getDescName(bth))
					.append("</td><td>").append(bth.getMacAddress())
					.append("</td></tr>");
				} else {
					res.append("<tr><td>").append(blu).append("</td></tr>");
				}
			});
			return res.toString();
		} else {
			return super.getToolTipText(value, cellTooSmall, r, c);
		}
	}
	
	@Override
	public void columnsWidthAdapt() {
		super.columnsWidthAdapt();
		final FontMetrics fm = getFontMetrics(getFont());
		TableColumn tc = columnModel.getColumn(COL_BLE);
		tc.setPreferredWidth(Math.max(SwingUtilities.computeStringWidth(fm, "99"), SwingUtilities.computeStringWidth(fm, tc.getHeaderValue().toString())));
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
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(value == null) {
				setText(CheckListView.NOT_APPLICABLE_STR);
				if (isSelected == false) {
					setForeground(table.getForeground());
				}
			} else if(value.toString().equals(redValue)) {
				setForeground(Color.red);
				if (isSelected) {
					setFont(getFont().deriveFont(Font.BOLD));
				}
			} else if(value.toString().equals(greenValue)) {
				setForeground(GREEN_OK);
				if (isSelected) {
					setFont(getFont().deriveFont(Font.BOLD));
				}
			} else if (isSelected == false) {
				setForeground(table.getForeground());
			}
			return this;
		}
	}
	
	private static class BooleanRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (value instanceof Boolean val) {
				if (val) {
					super.getTableCellRendererComponent(table, CheckListView.TRUE_STR, isSelected, hasFocus, row, column);
				} else {
					super.getTableCellRendererComponent(table, CheckListView.FALSE_STR, isSelected, hasFocus, row, column);
				}
			} else {
				super.getTableCellRendererComponent(table, value == null ? CheckListView.NOT_APPLICABLE_STR : value, isSelected, hasFocus, row, column);
			}
			return this;
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
			if (value instanceof Boolean val) {
				if (val) {
					super.getTableCellRendererComponent(table, CheckListView.TRUE_STR, isSelected, hasFocus, row, column);
					setForeground(goodVal ? GREEN_OK : Color.red);
				} else {
					super.getTableCellRendererComponent(table, CheckListView.FALSE_STR, isSelected, hasFocus, row, column);
					setForeground(goodVal ? Color.red : GREEN_OK);
				}
				if (isSelected) {
					setFont(getFont().deriveFont(Font.BOLD));
				}
			} else {
				super.getTableCellRendererComponent(table, value == null ? CheckListView.NOT_APPLICABLE_STR : value, isSelected, hasFocus, row, column);
				if (isSelected == false) {
					setForeground(table.getForeground());
				}
			}
			return this;
		}
	}
	
	private static class BLERenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if(value == null) {
				value = CheckListView.NOT_APPLICABLE_STR;
				setEnabled(true);
			} else if(value instanceof Collection c) {
				value = c.size() + "";
				setEnabled(c.size() > 0);
			} else if(value instanceof Number n && n.intValue() == 0) {
				setEnabled(false);
			} else {
				setEnabled(true);
			}
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			return this;
		}
	}
	
	private static class InetAddressAndPortRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(value instanceof BluInetAddressAndPort bluAddr && bluAddr.getAlternativeParents().size() > 0) {
				setText(bluAddr.getParentsAsString());
				setForeground(Color.red);
				if (isSelected) {
					setFont(getFont().deriveFont(Font.BOLD));
				} else {
					setFont(getFont().deriveFont(Font.PLAIN));
				}
			} else if (isSelected == false) {
				setForeground(table.getForeground());
				setFont(getFont().deriveFont(Font.PLAIN));
			}
			return this;
		}
	}
}