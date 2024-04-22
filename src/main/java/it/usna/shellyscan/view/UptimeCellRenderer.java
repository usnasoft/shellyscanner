package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.FontMetrics;
import java.time.LocalDateTime;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

public class UptimeCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private enum UptimeMode {SEC, DAY, FROM};
	
	private UptimeMode uptimeMode;
	
	public void setMode(String mode) {
		uptimeMode = UptimeMode.valueOf(mode);
	}
	
	@Override
	public void setValue(Object value) {
		if(value != null) {
			if(uptimeMode == UptimeMode.DAY) {
				int s = ((Number) value).intValue();
				final int gg = (int)(s / (3600 * 24));
				s = s % (3600 * 24);
				int hh = (int)(s / 3600);
				s = s % 3600;
				int mm = (int)(s / 60);
				s = s % 60;
				setText(String.format(LABELS.getString("col_uptime_as_day"), gg, hh, mm, s));
			} else if(uptimeMode == UptimeMode.FROM) {
				LocalDateTime since = LocalDateTime.now().minusSeconds(((Integer) value).intValue());
				setText(String.format(LABELS.getString("col_uptime_as_From"), since));
			} else { // UptimeMode.SEC
				setText(value.toString());
			}
		} else {
			setText("");
		}
	}
	
	public int getPreferredWidth(final FontMetrics fm) {
		if(uptimeMode == UptimeMode.DAY) { // %d days, %d hours, %d min, %d sec
			return SwingUtilities.computeStringWidth(fm, "100 days, 00 hours, 00 min, 00 sec");
		} else if(uptimeMode == UptimeMode.FROM) { // %1$td/%1$tm/%1$tY %1$tT
			return SwingUtilities.computeStringWidth(fm, "01/01/2024 00:00:00");
		} else {
			return SwingUtilities.computeStringWidth(fm, "17280000"); // 200 days
		}
	}
}