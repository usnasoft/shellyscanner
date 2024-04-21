package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Component;
import java.time.LocalDateTime;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class UptimeCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private enum UptimeMode {SEC, DAY, FROM};
	
	private UptimeMode uptimeMode;
	
	public void setMode(String mode) {
		uptimeMode = UptimeMode.valueOf(mode);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if(value != null) {
			if(uptimeMode == UptimeMode.DAY) {
				int s = ((Integer) value).intValue();
				final int gg = (int)(s / (3600 * 24));
				s = s % (3600 * 24);
				int hh = (int)(s / 3600);
				s = s % 3600;
				int mm = (int)(s / 60);
				s = s % 60;
				((JLabel)l).setText(String.format(LABELS.getString("col_uptime_as_day"), gg, hh, mm, s));
			} else if(uptimeMode == UptimeMode.FROM) {
				LocalDateTime since = LocalDateTime.now().minusSeconds(((Integer) value).intValue());
				((JLabel)l).setText(String.format(LABELS.getString("col_uptime_as_From"), since));
			} //else int value
		}
		return l;
	}
}
