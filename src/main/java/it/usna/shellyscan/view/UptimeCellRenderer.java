package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.FontMetrics;
import java.time.LocalDateTime;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

public class UptimeCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	private enum UptimeMode {
		SEC(JLabel.RIGHT),
		DAY(JLabel.LEFT),
		FROM(JLabel.LEFT);

		private final int align;
		private UptimeMode(int align) {
			this.align = align;
		}
	};
	
	private UptimeMode uptimeMode;
	
	public void setMode(String mode) {
		uptimeMode = UptimeMode.valueOf(mode);
		setHorizontalAlignment(uptimeMode.align);
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
			} else if(uptimeMode == UptimeMode.FROM) { // here we need to avoid changes (no seconds + round minutes)
				LocalDateTime since = LocalDateTime.now().minusSeconds(((Number) value).intValue());
				if(since.getSecond() > 31) {
					since.plusMinutes(1);
				}
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
			return SwingUtilities.computeStringWidth(fm, "99 days, 00 hours, 00 min, 00 sec");
		} else if(uptimeMode == UptimeMode.FROM) { // %1$td/%1$tm/%1$tY %1$tT
			return SwingUtilities.computeStringWidth(fm, "01/01/2024 00:00");
		} else {
			return SwingUtilities.computeStringWidth(fm, "17280000"); // 200 days
		}
	}
}